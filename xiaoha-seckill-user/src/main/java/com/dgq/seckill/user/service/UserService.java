package com.dgq.seckill.user.service;

import com.dgq.seckill.common.utils.Response;
import com.dgq.seckill.user.model.vo.LoginUserReqVO;
import com.dgq.seckill.user.model.vo.LoginUserRspVO;
import com.dgq.seckill.user.model.vo.RegisterUserReqVO;
import com.dgq.seckill.user.model.vo.SendVerifyCodeReqVO;

public interface UserService {

    Response<?> register(RegisterUserReqVO registerUserReqVO);

    Response<LoginUserRspVO> login(LoginUserReqVO loginUserReqVO);

    Response<?> sendVerifyCode(SendVerifyCodeReqVO sendVerifyCodeReqVO);

    Response<?> logout();
}
