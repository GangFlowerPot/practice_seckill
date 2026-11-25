package com.dgq.seckill.user.controller;

import com.dgq.seckill.common.aspect.ApiOperationLog;
import com.dgq.seckill.common.utils.Response;
import com.dgq.seckill.user.model.vo.LoginUserReqVO;
import com.dgq.seckill.user.model.vo.LoginUserRspVO;
import com.dgq.seckill.user.model.vo.RegisterUserReqVO;
import com.dgq.seckill.user.model.vo.SendVerifyCodeReqVO;
import com.dgq.seckill.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    @ApiOperationLog(description = "用户注册")
    public Response<?> register(@Validated @RequestBody RegisterUserReqVO registerUserReqVO) {
        return userService.register(registerUserReqVO);
    }

    @PostMapping("/login")
    @ApiOperationLog(description = "用户登录")
    public Response<LoginUserRspVO> login(@Validated @RequestBody LoginUserReqVO loginUserReqVO) {
        return userService.login(loginUserReqVO);
    }

    @PostMapping("/code/send")
    @ApiOperationLog(description = "发送验证码")
    public Response<?> sendVerifyCode(@Validated @RequestBody SendVerifyCodeReqVO sendVerifyCodeReqVO) {
        return userService.sendVerifyCode(sendVerifyCodeReqVO);
    }

    @PostMapping("/logout")
    @ApiOperationLog(description = "退出登录")
    public Response<?> logout() {
        return userService.logout();
    }
}
