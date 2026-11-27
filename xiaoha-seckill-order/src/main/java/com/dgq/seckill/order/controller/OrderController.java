package com.dgq.seckill.order.controller;

import com.dgq.seckill.common.aspect.ApiOperationLog;
import com.dgq.seckill.common.utils.Response;
import com.dgq.seckill.order.model.vo.DoSeckillReqVo;
import com.dgq.seckill.order.model.vo.DoSeckillRspVo;
import com.dgq.seckill.order.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seckill/order")
@Slf4j
public class OrderController {

    @Resource
    private OrderService orderService;

    @PostMapping
    @ApiOperationLog(description = "秒杀下单")
    public Response<DoSeckillRspVo> doSeckill(@RequestBody @Validated DoSeckillReqVo reqVo) {
        return orderService.doSeckill(reqVo);
    }
}
