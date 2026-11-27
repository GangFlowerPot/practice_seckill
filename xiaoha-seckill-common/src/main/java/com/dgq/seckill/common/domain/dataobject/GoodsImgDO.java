package com.dgq.seckill.common.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 商品轮播图表 DO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoodsImgDO {
    private Long id;

    private Long goodsId;

    private String imgUrl;

    private Integer sort;

    private LocalDateTime createTime;
}