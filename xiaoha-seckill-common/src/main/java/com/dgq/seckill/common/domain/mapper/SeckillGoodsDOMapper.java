package com.dgq.seckill.common.domain.mapper;

import com.dgq.seckill.common.domain.dataobject.SeckillGoodsDO;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;

public interface SeckillGoodsDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SeckillGoodsDO record);

    int insertSelective(SeckillGoodsDO record);

    SeckillGoodsDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(SeckillGoodsDO record);

    int updateByPrimaryKey(SeckillGoodsDO record);

    List<SeckillGoodsDO> selectByActivityId(@Param("activityId") Long activityId);

    SeckillGoodsDO selectByActivityIdAndGoodsId(@Param("activityId") Long activityId,
                                                @Param("goodsId") Long goodsId);

    int deductStock(@Param("id") Long id);
}