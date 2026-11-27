package com.dgq.seckill.goods.model.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询秒杀商品列表入参
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindSeckillGoodsListReqVO {

    @NotNull(message = "活动 ID 不能为空")
    @Positive(message = "活动 ID 不合法") // 活动 ID 必须是正整数
    private Long activityId;
}
