package com.dgq.seckill.order.model.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀下单入参
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DoSeckillReqVo {

    @NotNull(message = "活动 ID 不能为空")
    @Positive(message = "活动 ID 不合法")
    private Long activityId;

    @NotNull(message = "商品 ID 不能为空")
    @Positive(message = "商品 ID 不合法")
    private Long goodsId;
}
