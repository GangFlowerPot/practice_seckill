package com.dgq.seckill.order.service;

import com.dgq.seckill.common.utils.Response;
import com.dgq.seckill.order.model.vo.DoSeckillReqVo;
import com.dgq.seckill.order.model.vo.DoSeckillRspVo;

public interface OrderService {

    Response<DoSeckillRspVo> doSeckill(DoSeckillReqVo reqVo);
}
