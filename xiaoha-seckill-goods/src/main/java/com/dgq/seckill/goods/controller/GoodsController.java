package com.dgq.seckill.goods.controller;

import com.dgq.seckill.common.aspect.ApiOperationLog;
import com.dgq.seckill.common.utils.Response;
import com.dgq.seckill.goods.model.vo.FindSeckillGoodsDetailReqVO;
import com.dgq.seckill.goods.model.vo.FindSeckillGoodsDetailRspVO;
import com.dgq.seckill.goods.model.vo.FindSeckillGoodsListReqVO;
import com.dgq.seckill.goods.model.vo.FindSeckillGoodsListRspVO;
import com.dgq.seckill.goods.service.GoodsService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/seckill/goods")
public class GoodsController {

    @Resource
    private GoodsService goodsService;

    @PostMapping("/list")
    @ApiOperationLog(description = "查询秒杀商品列表")
    public Response<List<FindSeckillGoodsListRspVO>> getSeckillGoodsList(
            @RequestBody @Validated FindSeckillGoodsListReqVO reqVO) {
        return goodsService.findSeckillGoodsList(reqVO);
    }

    @PostMapping("/detail")
    @ApiOperationLog(description = "查询秒杀商品详情")
    public Response<FindSeckillGoodsDetailRspVO> getSeckillGoodsDetail(
            @RequestBody @Validated FindSeckillGoodsDetailReqVO reqVO) {
        return goodsService.findSeckillGoodsDetail(reqVO);
    }
}
