package com.dgq.seckill.goods.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.dgq.seckill.common.domain.dataobject.*;
import com.dgq.seckill.common.domain.mapper.*;
import com.dgq.seckill.common.enums.ActivityStatusEnum;
import com.dgq.seckill.common.enums.ResponseCodeEnum;
import com.dgq.seckill.common.exception.BizException;
import com.dgq.seckill.common.utils.Response;
import com.dgq.seckill.goods.model.vo.FindSeckillGoodsDetailReqVO;
import com.dgq.seckill.goods.model.vo.FindSeckillGoodsDetailRspVO;
import com.dgq.seckill.goods.model.vo.FindSeckillGoodsListReqVO;
import com.dgq.seckill.goods.model.vo.FindSeckillGoodsListRspVO;
import com.dgq.seckill.goods.service.GoodsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品模块业务
 */
@Service
@Slf4j
public class GoodsServiceImpl implements GoodsService {

    @Resource
    private SeckillGoodsDOMapper seckillGoodsDOMapper;

    @Resource
    private SeckillActivityDOMapper seckillActivityDOMapper;

    @Resource
    private GoodsDOMapper goodsDOMapper;

    @Resource
    private GoodsImgDOMapper goodsImgDOMapper;

    @Resource
    private GoodsDetailDOMapper goodsDetailDOMapper;

    @Override
    public Response<List<FindSeckillGoodsListRspVO>> findSeckillGoodsList(FindSeckillGoodsListReqVO reqVO) {

        Long activityId = reqVO.getActivityId();
        log.info("==> 查询秒杀商品列表，activityId：{}", activityId);

        // 1. 查询活动信息
        SeckillActivityDO activityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
        if (Objects.isNull(activityDO))
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);

        // 2. 根据活动ID查询该活动下的所有秒杀商品
        List<SeckillGoodsDO> seckillGoodsDOS = seckillGoodsDOMapper.selectByActivityId(activityId);
        if (CollUtil.isEmpty(seckillGoodsDOS)) {
            log.info("==> 该活动下暂无秒杀商品，activityId：{}", activityId);
            return Response.success(Collections.emptyList());
        }

        // 3. 批量查询关联的商品信息（目的是为了获取对应秒杀商品的原价）
        List<Long> goodsIds = seckillGoodsDOS.stream()
                .map(SeckillGoodsDO :: getGoodsId)
                .collect(Collectors.toList());

        // 一次性批量查询所有商品，提示查询性能
        List<GoodsDO> goodsDOS = goodsDOMapper.selectByIds(goodsIds);
        // 将商品 ID 和商品信息映射为 map，方便后续查找
        Map<Long, GoodsDO> goodsMap = goodsDOS.stream()
                .collect(Collectors.toMap(GoodsDO::getId, goodsDO -> goodsDO));

        // 4. 计算活动状态（基于当前时间动态判断）
        ActivityStatusEnum activityStatusEnum = calculateActivityStatus(activityDO);

        // 5. 组装响应数据
        List<FindSeckillGoodsListRspVO> rspVOS = new ArrayList<>();
        for (SeckillGoodsDO seckillGoodsDO : seckillGoodsDOS) {
            FindSeckillGoodsListRspVO rspVO = new FindSeckillGoodsListRspVO();
            rspVO.setId(seckillGoodsDO.getId());
            rspVO.setGoodsId(seckillGoodsDO.getGoodsId());
            rspVO.setActivityId(seckillGoodsDO.getActivityId());
            rspVO.setSeckillTitle(seckillGoodsDO.getSeckillTitle());
            rspVO.setSeckillImg(seckillGoodsDO.getSeckillImg());
            rspVO.setSeckillPrice(seckillGoodsDO.getSeckillPrice());
            rspVO.setSeckillTotal(seckillGoodsDO.getSeckillTotal());
            rspVO.setSeckillStock(seckillGoodsDO.getSeckillStock());
            rspVO.setActivityStatus(activityStatusEnum.getStatus());
            rspVO.setBeginTime(activityDO.getBeginTime());
            rspVO.setEndTime(activityDO.getEndTime());

            // 设置商品原价
            GoodsDO goodsDO = goodsMap.get(seckillGoodsDO.getGoodsId());
            if (Objects.nonNull(goodsDO))
                rspVO.setGoodsPrice(goodsDO.getGoodsPrice());
            rspVOS.add(rspVO);
        }
        return Response.success(rspVOS);
    }

    @Override
    public Response<FindSeckillGoodsDetailRspVO> findSeckillGoodsDetail(FindSeckillGoodsDetailReqVO reqVO) {
        Long goodsId = reqVO.getGoodsId();
        Long activityId = reqVO.getActivityId();
        log.info("==> 查询秒杀商品详情，goodsId：{}，activityId：{}", goodsId, activityId);

        // 1. 根据活动 ID 查询活动信息，校验活动是否存在
        SeckillActivityDO activityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
        if (Objects.isNull(activityDO))
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);

        // 2. 根据活动 ID 和 商品 ID 查询秒杀商品
        SeckillGoodsDO seckillGoodsDO = seckillGoodsDOMapper.selectByActivityIdAndGoodsId(activityId, goodsId);
        if (Objects.isNull(seckillGoodsDO))
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);

        // 3. 根据 goodsId 查询商品基本信息，如商品名称、原价
        GoodsDO goodsDO = goodsDOMapper.selectByPrimaryKey(goodsId);

        // 4. 根据 goodsId 查询商品轮播图列表
        List<GoodsImgDO> goodsImgDOS = goodsImgDOMapper.selectByGoodsId(goodsId);
        List<String> goodsImgs = null;
        if (CollUtil.isNotEmpty(goodsImgDOS)) {
            goodsImgs = goodsImgDOS.stream()
                    .map(GoodsImgDO::getImgUrl)
                    .toList();
        }

        // 5. 根据 goodsId 查询商品详情 HTML
        GoodsDetailDO goodsDetailDO = goodsDetailDOMapper.selectByGoodsId(goodsId);

        // 6. 计算活动状态
        ActivityStatusEnum activityStatusEnum = calculateActivityStatus(activityDO);

        // 7. 组装响应数据
        FindSeckillGoodsDetailRspVO rspVO = FindSeckillGoodsDetailRspVO.builder()
                .id(seckillGoodsDO.getId())
                .goodsId(seckillGoodsDO.getGoodsId())
                .activityId(seckillGoodsDO.getActivityId())
                .seckillPrice(seckillGoodsDO.getSeckillPrice())
                .seckillTotal(seckillGoodsDO.getSeckillTotal())
                .seckillStock(seckillGoodsDO.getSeckillStock())
                .activityStatus(activityStatusEnum.getStatus())
                .beginTime(activityDO.getBeginTime())
                .endTime(activityDO.getEndTime())
                .goodsImgs(goodsImgs)
                .build();

        // 设置商品基本信息
        if (Objects.nonNull(goodsDO)) {
            rspVO.setGoodsName(goodsDO.getGoodsName());
            rspVO.setGoodsPrice(goodsDO.getGoodsPrice());
        }

        // 设置商品详情
        if (Objects.nonNull(goodsDetailDO)) {
            rspVO.setGoodsDetail(goodsDetailDO.getDetailContent());
        }

        return Response.success(rspVO);
    }

    /**
     * 根据当前时间动态计算活动状态
     * @param activityDO
     * @return
     */
    private ActivityStatusEnum calculateActivityStatus(SeckillActivityDO activityDO) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activityDO.getBeginTime())) {
            return ActivityStatusEnum.NOT_STARTED;
        } else if (now.isAfter(activityDO.getEndTime())) {
            return ActivityStatusEnum.ENDED;
        } else {
            return ActivityStatusEnum.ING;
        }
    }
}
