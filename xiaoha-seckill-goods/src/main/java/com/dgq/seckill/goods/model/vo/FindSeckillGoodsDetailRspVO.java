package com.dgq.seckill.goods.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 查询秒杀商品详情出参
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindSeckillGoodsDetailRspVO {

    private Long id;

    private Long goodsId;

    private Long activityId;

    private String goodsName;

    private BigDecimal goodsPrice;

    private List<String> goodsImgs;

    private String goodsDetail;

    private BigDecimal seckillPrice;

    private Integer seckillTotal;

    private Integer seckillStock;

    private Integer activityStatus;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;
}
