package com.dgq.seckill.goods.service;

import com.dgq.seckill.common.utils.Response;
import com.dgq.seckill.goods.model.vo.FindSeckillGoodsDetailReqVO;
import com.dgq.seckill.goods.model.vo.FindSeckillGoodsDetailRspVO;
import com.dgq.seckill.goods.model.vo.FindSeckillGoodsListReqVO;
import com.dgq.seckill.goods.model.vo.FindSeckillGoodsListRspVO;

import java.util.List;

public interface GoodsService {
    /**
     * 查询秒杀商品列表
     */
    Response<List<FindSeckillGoodsListRspVO>> findSeckillGoodsList(FindSeckillGoodsListReqVO reqVO);

    /**
     * 查询秒杀商品详情
     */
    Response<FindSeckillGoodsDetailRspVO> findSeckillGoodsDetail(FindSeckillGoodsDetailReqVO reqVO);

    /**
     * 预热指定活动的商品缓存
     * @param activityId
     * @return
     */
    Response<?> preheatActivityGoods(Long activityId);
}
