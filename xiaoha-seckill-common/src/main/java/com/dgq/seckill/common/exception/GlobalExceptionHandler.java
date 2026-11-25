package com.dgq.seckill.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.dgq.seckill.common.enums.ResponseCodeEnum;
import com.dgq.seckill.common.utils.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({BizException.class})
    @ResponseBody
    public Response<Object> handlerBizException(HttpServletRequest request, BizException e) {
        log.warn("{} request fail, errorCode: {}, errorMessage: {}", request.getRequestURI(), e.getErrorCode(), e.getErrorMessage());
        return Response.fail(e);
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class})
    @ResponseBody
    public Response<Object> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException e) {
        String errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();

        BindingResult bindingResult = e.getBindingResult();

        StringBuilder sb = new StringBuilder();

        Optional.ofNullable(bindingResult.getFieldErrors()).ifPresent(errors -> {
            errors.forEach(error ->
                sb.append(error.getField())
                        .append(" ")
                        .append(error.getDefaultMessage())
                        .append("，当前值：'")
                        .append(error.getRejectedValue())
                        .append("'；")
            );
        });

        String errorMessage = sb.toString();

        log.warn("{} request error, errorCode: {}, errorMessage: {}", request.getRequestURI(), errorCode, errorMessage);

        return Response.fail(errorCode, errorMessage);
    }

    @ExceptionHandler({ NotLoginException.class})
    @ResponseBody
    public Response<Object> handleNotLoginException(HttpServletRequest request, NotLoginException e) {
        log.warn("{} request fail，未登录异常：{}", request.getRequestURI(), e.getMessage());
        return Response.fail(ResponseCodeEnum.UNAUTHORIZED);
    }

    @ExceptionHandler({Exception.class})
    @ResponseBody
    public Response<Object> handleOtherException(HttpServletRequest request, Exception e) {
        log.error("{} request error, ", request.getRequestURI(), e);
        return Response.fail(ResponseCodeEnum.SYSTEM_ERROR);
    }
}
