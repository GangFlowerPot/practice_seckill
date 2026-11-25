package com.dgq.seckill.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.dgq.seckill.common.aspect.ApiOperationLog;
import com.dgq.seckill.common.enums.ResponseCodeEnum;
import com.dgq.seckill.common.exception.BizException;
import com.dgq.seckill.common.utils.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class TestController {

    @GetMapping("/test/response")
    @ApiOperationLog(description = "测试公共返参")
    public Response<String> testResponse(@RequestParam String name) {
        return Response.success("Hello, " + name + " !");
    }

    @GetMapping("/test/bizException")
    @ApiOperationLog(description = "测试业务异常捕获")
    public Response<String> testBizException() {
        throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID);
    }

    @GetMapping("/test/systemException")
    @ApiOperationLog(description = "测试系统异常捕获")
    public Response<String> testSystemException() {
        int i = 1 / 0;
        return Response.success();
    }

    @GetMapping("/test/checkLogger")
    public Response<String> checkLogger() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        String loggerClass = ctx.getRootLogger().getClass().getName();
        return Response.success("Root Logger 实现类：" + loggerClass);
    }

    @GetMapping("/test/isLogin")
    public Response<?> isLogin() {
        boolean isLogin = StpUtil.isLogin();

        if (isLogin) {
            long loginId = StpUtil.getLoginIdAsLong();
            log.info("==> 当前已登录，userId：{}", loginId);
            return Response.success("当前登录用户 ID：" + loginId);
        } else {
            return Response.success("当前未登录");
        }
    }
}
