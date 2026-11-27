package com.dgq.seckill.common.domain.mapper;

import com.dgq.seckill.common.domain.dataobject.GoodsImgDO;

import java.util.List;

public interface GoodsImgDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(GoodsImgDO record);

    int insertSelective(GoodsImgDO record);

    GoodsImgDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(GoodsImgDO record);

    int updateByPrimaryKey(GoodsImgDO record);

    List<GoodsImgDO> selectByGoodsId(Long goodsId);
}