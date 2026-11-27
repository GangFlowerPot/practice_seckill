package com.dgq.seckill.order.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀下单出参
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DoSeckillRspVo {

    private Long orderId;

    private String orderNo;

    private String goodsName;

    private String goodsImg;

    private BigDecimal seckillPrice;

    private Integer status;

    private LocalDateTime expireTime;
}
