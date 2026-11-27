package com.dgq.seckill.common.domain.mapper;

import com.dgq.seckill.common.domain.dataobject.GoodsDetailDO;

public interface GoodsDetailDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(GoodsDetailDO record);

    int insertSelective(GoodsDetailDO record);

    GoodsDetailDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(GoodsDetailDO record);

    int updateByPrimaryKeyWithBLOBs(GoodsDetailDO record);

    int updateByPrimaryKey(GoodsDetailDO record);

    GoodsDetailDO selectByGoodsId(Long goodsId);
}