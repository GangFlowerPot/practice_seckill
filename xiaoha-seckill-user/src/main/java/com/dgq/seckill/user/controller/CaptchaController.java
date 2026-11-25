package com.dgq.seckill.user.controller;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.generator.common.model.dto.GenerateParam;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.dgq.seckill.common.aspect.ApiOperationLog;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/captcha")
@Slf4j
public class CaptchaController {

    @Autowired
    private ImageCaptchaApplication imageCaptchaApplication;

    /**
     * 生成行为验证码接口
     * @param type
     * @return
     */
    @PostMapping("/gen")
    @ApiOperationLog(description = "生成行为验证码")
    public ApiResponse<ImageCaptchaVO> genCaptcha(@RequestParam(value = "type", required = false)String type) {
        // 如果前端没有传递 type 参数，则默认使用滑块验证码类型
        if (StringUtils.isBlank(type)) {
            type = CaptchaTypeConstant.SLIDER;
        }

        // 如果前端传递的是 RANDOM，则随机生成一种验证码类型
        if ("RANDOM".equals(type)) {
            // 生成 0-3 的随机整数（包含0，不包含4）
            int i = ThreadLocalRandom.current().nextInt(0, 4);

            // 根据随机数选择对应的验证码类型
            switch (i) {
                case 0:
                    type = CaptchaTypeConstant.SLIDER;
                    break;
                case 1:
                    type = CaptchaTypeConstant.CONCAT;
                    break;
                case 2:
                    type = CaptchaTypeConstant.ROTATE;
                    break;
                case 3:
                    type = CaptchaTypeConstant.WORD_IMAGE_CLICK;
                    break;
            }

        }
        // 构建验证码生成参数对象
        GenerateParam generateParam = new GenerateParam();
        // 设置要生成的验证码类型
        generateParam.setType(type);

        // 生成验证码，返回验证码数据（包含背景图、模板图、验证码ID等）
        ApiResponse<ImageCaptchaVO> response = imageCaptchaApplication.generateCaptcha(generateParam);
        return response;
    }

    /**
     * 检验行为验证码接口
     * @param data
     * @return
     */
    @PostMapping("/check")
    @ApiOperationLog(description = "检验行为验证码")
    public ApiResponse<?> checkCaptcha(@RequestBody CaptchaData data) {
        // 验证码校验，内部会校验滑块是否对齐了，以及滑动轨迹是否是真人
        ApiResponse<?> response = imageCaptchaApplication.matching(data.getId(), data.getData());

        // 校验成功，返回验证码ID
        if (response.isSuccess()) {
            return ApiResponse.ofSuccess(Collections.singletonMap("id", data.getId()));
        }

        // 校验失败，返回错误响应
        return response;
    }

    /**
     * 验证码校验请求数据
     */
    @Data
    public static class CaptchaData{
        private String id;
        private ImageCaptchaTrack data;
    }
}
