package com.dgq.seckill.user.service.impl;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.dgq.seckill.common.domain.dataobject.UserDO;
import com.dgq.seckill.common.domain.mapper.UserDOMapper;
import com.dgq.seckill.common.enums.ResponseCodeEnum;
import com.dgq.seckill.common.exception.BizException;
import com.dgq.seckill.common.utils.Response;
import com.dgq.seckill.user.enums.LoginTypeEnum;
import com.dgq.seckill.user.enums.UserStatusEnum;
import com.dgq.seckill.user.enums.VerifyCodeTypeEnum;
import com.dgq.seckill.user.model.vo.LoginUserReqVO;
import com.dgq.seckill.user.model.vo.LoginUserRspVO;
import com.dgq.seckill.user.model.vo.RegisterUserReqVO;
import com.dgq.seckill.user.model.vo.SendVerifyCodeReqVO;
import com.dgq.seckill.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserDOMapper userDOMapper;

//    @Resource
//    private RedisTemplate<String, Object> redisTemplate;

    @Resource(name = "bizExecutor")
    private Executor bizExecutor;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // redis 中验证码的 key 前缀
    public static final String VERIFY_CODE_KEY_PREFIX = "verify_code:";
    // redis 中发送频率限制的 key 前缀
    public static final String VERIFY_CODE_LIMIT_KEY_PREFIX = "verify_code_limit:";
    // 验证码过期时间（分钟）
    private static final Long VERIFY_CODE_EXPIRE_MINUTES = 5L;
    // 发送频率限制时间（秒）
    private static final Long VERIFY_CODE_LIMIT_SECONDS = 60L;
    // Redis 中每日发送次数限制的 key 前缀
    public static final String VERIFY_CODE_DAILY_LIMIT_KEY_PREFIX = "verify_code_daily:";
    // 每日发送次数上限
    private static final Integer VERIFY_CODE_DAILY_LIMIT = 10;
    // Redis 中登录失败次数的 key 前缀
    public static final String LOGIN_FAIL_COUNT_KEY_PREFIX = "login_fail_count:";
    // 登录失败次数上限（超过此值则临时锁定账号）
    public static final Integer LOGIN_FAIL_MAX_COUNT = 5;
    // 账号临时锁定时间（分钟）
    private static final Long LOGIN_LOCK_MINUTES = 30L;
    // BCrypt 密码编码器
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();


    // 验证码校验 lua 脚本
    private final DefaultRedisScript<Long> checkAndDeleteVerifyCodeScript;
    // 登录失败计数 lua 脚本
    private final DefaultRedisScript<Long> checkAndIncrementLoginFailScript;
    // 每日发送次数限制 lua 脚本
    private final DefaultRedisScript<Long> checkAndIncrementDailyLimitScript;
    @Autowired
    private ImageCaptchaApplication imageCaptchaApplication;

    public UserServiceImpl() {
        checkAndDeleteVerifyCodeScript = new DefaultRedisScript<>();
        // 从 classpath 的 lua 目录下加载 lua 脚本文件
        checkAndDeleteVerifyCodeScript.setLocation(new ClassPathResource("lua/check_and_delete_verify_code.lua"));
        // 指定脚本返回值类型
        checkAndDeleteVerifyCodeScript.setResultType(Long.class);

        checkAndIncrementLoginFailScript = new DefaultRedisScript<>();
        checkAndIncrementLoginFailScript.setLocation(new ClassPathResource("lua/check_and_increment_login_fail_count.lua"));
        checkAndIncrementLoginFailScript.setResultType(Long.class);

        checkAndIncrementDailyLimitScript = new DefaultRedisScript<>();
        checkAndIncrementDailyLimitScript.setLocation(new ClassPathResource("lua/check_and_increment_verify_code_daily_limit.lua"));
        checkAndIncrementDailyLimitScript.setResultType(Long.class);
    }


    @Override
    public Response<?> register(RegisterUserReqVO registerUserReqVO) {
        String mobile = registerUserReqVO.getMobile();
        String password = registerUserReqVO.getPassword();
        String verifyCode = registerUserReqVO.getVerifyCode();

//        if (!"123456".equals(verifyCode)) {
//            throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
//        }
        checkVerifyCode(verifyCode, mobile, VerifyCodeTypeEnum.REGISTER.getPurpose());

        Long existUserId = userDOMapper.selectIdByMobile(mobile);
        if (Objects.nonNull(existUserId)) {
            throw new BizException(ResponseCodeEnum.USER_MOBILE_EXISTS);
        }

        String encodedPassword = PASSWORD_ENCODER.encode(password);

        UserDO userDO = UserDO.builder()
                .mobile(mobile)
                .password(encodedPassword)
                .nickname(generateNickname())
                .status(UserStatusEnum.ENABLED.getCode())
                .build();

        userDOMapper.insertSelective(userDO);

        log.info("===> 用户注册成功，mobile：{}", mobile);
        return Response.success();
    }

    @Override
    public Response<LoginUserRspVO> login(LoginUserReqVO loginUserReqVO) {
        String mobile = loginUserReqVO.getMobile();
        Integer type = loginUserReqVO.getType();

        UserDO userDO = userDOMapper.selectByMobile(mobile);

        if (Objects.isNull(userDO)) {
            // 密码登录，返回模糊错误提示
            if (Objects.equals(type, LoginTypeEnum.PASSWORD.getCode())) {
                throw new BizException(ResponseCodeEnum.USER_LOGIN_CREDENTIAL_ERROR);
            }
            throw new BizException(ResponseCodeEnum.USER_MOBILE_NOT_REGISTER);
        }

        if (Objects.equals(userDO.getStatus(), UserStatusEnum.DISABLED.getCode())) {
            throw new BizException(ResponseCodeEnum.USER_STATUS_DISABLED);
        }

        if (Objects.equals(type, LoginTypeEnum.PASSWORD.getCode())) {
            // 检查登录失败次数
            checkLoginFailLimit(mobile);

            // 密码登录：校验密码是否正确
            checkPassword(loginUserReqVO.getPassword(), userDO.getPassword(), mobile);
        } else {
            // 验证码登录，校验验证码是否正确
            checkVerifyCode(loginUserReqVO.getVerifyCode(), mobile, VerifyCodeTypeEnum.LOGIN.getPurpose());
        }

        // 调用 SaToken 执行登录，传入用户 ID
        StpUtil.login(userDO.getId());

        String token = StpUtil.getTokenValue();

        LoginUserRspVO loginUserRspVO = LoginUserRspVO.builder()
                .token(token)
                .userInfo(LoginUserRspVO.UserInfo.builder()
                        .id(userDO.getId())
                        .nickname(userDO.getNickname())
                        .avatar(userDO.getAvatar())
                        .build())
                .build();

        log.info("==> 用户登录成功，userId：{}，mobile：{}", userDO.getId(), mobile);

        return Response.success(loginUserRspVO);
    }

    @Override
    public Response<?> sendVerifyCode(SendVerifyCodeReqVO sendVerifyCodeReqVO) {
        String mobile = sendVerifyCodeReqVO.getMobile();
        Integer type = sendVerifyCodeReqVO.getType();

        // 行为验证码二次校验
        String captchaId = sendVerifyCodeReqVO.getCaptchaId();
        if (StrUtil.isBlank(captchaId)) {
            throw new BizException(ResponseCodeEnum.CAPTCHA_VERIFICATION_FAILED);
        }

        // 判断 ImageCaptchaApplication 是否支持二次校验 TODO 行为验证码需要配合前端使用，仅生成无法验证
//        boolean verified = false;
//        if (imageCaptchaApplication instanceof SecondaryVerificationApplication) {
//            verified = ((SecondaryVerificationApplication) imageCaptchaApplication).secondaryVerification(captchaId);
//        }
//        if (!verified) {
//            throw new BizException(ResponseCodeEnum.CAPTCHA_VERIFICATION_FAILED);
//        }

        // 判断验证码类型是否合法
        VerifyCodeTypeEnum verifyCodeType = VerifyCodeTypeEnum.valueOf(type);
        if (Objects.isNull(verifyCodeType)) {
            throw new BizException(ResponseCodeEnum.VERIFY_CODE_TYPE_ERROR);
        }

        String limitKey = VERIFY_CODE_LIMIT_KEY_PREFIX + verifyCodeType.getPurpose() + ":" + mobile;

        // 如果 Key 已存在（60秒内已发送过），返回false，不存在则创建 Key 并返回 true
        Boolean absent = stringRedisTemplate.opsForValue()
                .setIfAbsent(limitKey, "1", VERIFY_CODE_LIMIT_SECONDS, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(absent)) {
            throw new BizException(ResponseCodeEnum.VERIFY_CODE_SEND_TOO_FREQUENT);
        }

        // 每日发送次数限制，同一手机号，同一场景，每天最大发送 10 条
        String dailyLimitKey = VERIFY_CODE_DAILY_LIMIT_KEY_PREFIX + verifyCodeType.getPurpose()
                + ":" + mobile + ":" + LocalDate.now();
        // 计算从当前时间，到第二天凌晨零点之间还剩下多少秒
        long secondsUntilMidnight = Duration.between(
                LocalDateTime.now(),
                LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)
        ).getSeconds();

        // 执行 lua 脚本，原子性地检查每日发送次数并累加
        Long dailyCount = stringRedisTemplate.execute(checkAndIncrementDailyLimitScript,
                Collections.singletonList(dailyLimitKey),
                String.valueOf(VERIFY_CODE_DAILY_LIMIT),
                String.valueOf(secondsUntilMidnight));

        if (Objects.nonNull(dailyCount) && dailyCount == -1) {
            throw new BizException(ResponseCodeEnum.VERIFY_CODE_DAILY_LIMIT_EXCEEDED);
        }

        // 生成6位随机数字验证码
        String verifyCode = RandomUtil.randomNumbers(6);

        String redisKey = VERIFY_CODE_KEY_PREFIX + verifyCodeType.getPurpose() + ":" + mobile;

        // 缓存验证码（5分钟 TTL）
        stringRedisTemplate.opsForValue().set(redisKey, verifyCode, VERIFY_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        // NOTICE 写入：通过 Pipeline 写入频率限制 Key + 验证码 Key -- 由于这种写入limitkey的方式与之前先校验在写入这个逻辑存在非原子问题，在上述改为使用SETNX/setifabsent方式后，这里就不用再进行一次key的写入
//        stringRedisTemplate.executePipelined(new SessionCallback<Void>() {
//            @Override
//            public Void execute(RedisOperations operations) {
//                // 先写频率限制 key（60 秒 TTL）
//                operations.opsForValue().set(limitKey, "1", VERIFY_CODE_LIMIT_SECONDS, TimeUnit.SECONDS);
//                // 再写验证码 key（5 分钟 TTL）
//                operations.opsForValue().set(redisKey, verifyCode, VERIFY_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
//                return null;
//            }
//        });

        // 异步发送短信验证码
        bizExecutor.execute(() -> sendSms(mobile, verifyCode));

        return Response.success();
    }

    @Override
    public Response<?> logout() {
        String tokenValue = StpUtil.getTokenValue();
        Object userId = StpUtil.getLoginId();
        // 调用 SaToken 的退出登录方法
        // 此方法会自动从请求头中获取Token，然后清除该 Token 对应的会话信息
        StpUtil.logout();
        log.info("==> 用户退出登录，userId：{}，token：{}", userId, tokenValue);
        return Response.success();
    }

    private void sendSms(String mobile, String verifyCode) {
        try {
            // TODO 调用短信服务商API发送验证码

            log.info("==> 验证码发送成功，mobile：{}，verifyCode：{}", mobile, verifyCode);
        } catch (Exception e) {
            log.error("==> 验证码发送失败，mobile：{}，verifyCode：{}", mobile, verifyCode, e);
        }
    }

    // 校验密码
    private void checkPassword(String rawPassword, String encodePassword, String mobile) {
        if (StrUtil.isBlank(rawPassword)) {
            // 登录失败次数 +1
            addLoginFailCount(mobile);

            throw new BizException(ResponseCodeEnum.USER_LOGIN_CREDENTIAL_ERROR);
        }

        boolean matches = PASSWORD_ENCODER.matches(rawPassword, encodePassword);
        if (!matches) {
            // 登录失败次数 +1
            addLoginFailCount(mobile);

            throw new BizException(ResponseCodeEnum.USER_LOGIN_CREDENTIAL_ERROR);
        }

        // 密码校验成功，清除登录失败次数
        String failCountKey = LOGIN_FAIL_COUNT_KEY_PREFIX + mobile;
        stringRedisTemplate.delete(failCountKey);
    }

    private void checkVerifyCode(String verifyCode, String mobile, String purpose) {
        if (StrUtil.isBlank(verifyCode)) {
            throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
        }

        // 从 redis 中获取对应手机号，对应场景的验证码
        String redisKey = VERIFY_CODE_KEY_PREFIX + purpose + ":" + mobile;

        // 执行 lua 脚本，原子性地比对验证码并删除（匹配返回 1；不匹配或 key 不存在返回 0）
        Long result = stringRedisTemplate.execute(checkAndDeleteVerifyCodeScript,
                Collections.singletonList(redisKey),
                verifyCode);

        if (result == null || result == 0) {
            throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
        }
        // 能走到这里，说明 result == 1， 验证码比对成功，并且验证码已被删除
    }

    private String generateNickname() {
        return "用户" + RandomUtil.randomNumbers(6);
    }

    // 检查登录失败次数是否超限
    private void checkLoginFailLimit(String mobile) {
        String failCountKey = LOGIN_FAIL_COUNT_KEY_PREFIX + mobile;

        // 查询 Redis 缓存中的计数
        String failCountStr = stringRedisTemplate.opsForValue().get(failCountKey);

        if (StrUtil.isNotBlank(failCountStr)) {
            int failCount = Integer.parseInt(failCountStr);
            if (failCount >= LOGIN_FAIL_MAX_COUNT) {
                throw new BizException(ResponseCodeEnum.LOGIN_FAIL_TOO_MANY);
            }
        }
    }

    // 累加登录失败次数（Lua 脚本原子操作）
    private void addLoginFailCount(String mobile) {
        String failCountKey = LOGIN_FAIL_COUNT_KEY_PREFIX + mobile;

        // 执行 Lua 脚本，原子性地检查失败次数并累加（超限返回 -1，未超限返回累加后的值）
        Long result = stringRedisTemplate.execute(checkAndIncrementLoginFailScript,
                Collections.singletonList(failCountKey),
                String.valueOf(LOGIN_FAIL_MAX_COUNT),
                String.valueOf(LOGIN_LOCK_MINUTES * 60));

        // 失败次数已达上限，直接拒接
        if (Objects.nonNull(result) && result == -1) {
            throw new BizException(ResponseCodeEnum.LOGIN_FAIL_TOO_MANY);
        }
    }
}
