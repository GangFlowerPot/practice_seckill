package com.dgq.seckill.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.dgq.seckill.common.domain.dataobject.GoodsDO;
import com.dgq.seckill.common.domain.dataobject.SeckillActivityDO;
import com.dgq.seckill.common.domain.dataobject.SeckillGoodsDO;
import com.dgq.seckill.common.domain.dataobject.SeckillOrderDO;
import com.dgq.seckill.common.domain.mapper.GoodsDOMapper;
import com.dgq.seckill.common.domain.mapper.SeckillActivityDOMapper;
import com.dgq.seckill.common.domain.mapper.SeckillGoodsDOMapper;
import com.dgq.seckill.common.domain.mapper.SeckillOrderDOMapper;
import com.dgq.seckill.common.enums.ResponseCodeEnum;
import com.dgq.seckill.common.exception.BizException;
import com.dgq.seckill.common.utils.Response;
import com.dgq.seckill.order.enums.OrderStatusEnum;
import com.dgq.seckill.order.model.vo.DoSeckillReqVo;
import com.dgq.seckill.order.model.vo.DoSeckillRspVo;
import com.dgq.seckill.order.service.OrderService;
import com.dgq.seckill.order.utils.OrderLockUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private SeckillActivityDOMapper seckillActivityDOMapper;

    @Resource
    private SeckillGoodsDOMapper seckillGoodsDOMapper;

    @Resource
    private GoodsDOMapper goodsDOMapper;

    @Resource
    private SeckillOrderDOMapper seckillOrderDOMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private OrderLockUtils orderLockUtils;

    @Override
//    @Transactional(rollbackFor = Exception.class)
    /**
     * NOTICE
     * @Transactional 注解是声明式事务，它作用于整个方法，无法精确控制方法内部某几行代码的事务边界。
     * 而 TransactionTemplate 是编程式事务，可以在方法内部任意位置开启和提交事务，
     */
    public Response<DoSeckillRspVo> doSeckill(DoSeckillReqVo reqVo) {

        Long activityId = reqVo.getActivityId();
        Long goodsId = reqVo.getGoodsId();

        // 1.获取当前登录用户id
        long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 当前登录用户id：{}", userId);

        // 应用层锁：防止同一用户并发重复下单
        // 构建锁 key "userId:activityId:goodsId"
        String lockKey = userId + ":" + activityId + ":" + goodsId;

        // 尝试获取锁，获取失败，则说明该用户对该商品已经有请求在处理中
        if (!orderLockUtils.tryLock(lockKey)) {
            log.warn("==> 应用层锁拦截重复下单，userId：{}，activityId：{}，goodsId：{}", userId, activityId, goodsId);
            throw new BizException(ResponseCodeEnum.SECKILL_ORDER_PROCESSING);
        }

        try {
            return processSeckill(activityId, goodsId, userId);
        } finally {
            // 无论成功还是失败，都要释放锁
            orderLockUtils.unlock(lockKey);
        }
    }

    public Response<DoSeckillRspVo> processSeckill(Long activityId, Long goodsId, Long userId) {
        // 2.校验活动是否存在
        SeckillActivityDO activityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
        if (Objects.isNull(activityDO)) {
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }

        // 3.校验秒杀活动时间
        LocalDateTime now = LocalDateTime.now();
        // 活动是否还没开始
        if (now.isBefore(activityDO.getBeginTime()))
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_STARTED);

        // 活动已结束
        if (now.isAfter(activityDO.getEndTime()))
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_ENDED);

        // 4.根据活动 ID 和商品 ID 查询秒杀商品，校验次活动下商品是否存在
        SeckillGoodsDO seckillGoodsDO = seckillGoodsDOMapper.selectByActivityIdAndGoodsId(activityId, goodsId);
        if (Objects.isNull(seckillGoodsDO))
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);

        // 5.库存校验，库存必须大于0
        if (seckillGoodsDO.getSeckillStock() <= 0)
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_SOLD_OUT);

        // 6.查询商品信息，用于冗余到订单中
        GoodsDO goodsDO = goodsDOMapper.selectByPrimaryKey(goodsId);
        // 使用 Hutool 提供的工具方法，通过雪花算法生成订单号
        String orderNo = IdUtil.getSnowflakeNextIdStr();
        // 订单过期时间：当前时间 + 30分钟
        LocalDateTime expireTime = now.plusMinutes(30);

        // 编程式事务，精确控制事务边界
        SeckillOrderDO orderDO = transactionTemplate.execute(status -> {
            // 7.扣减库存
            int count = seckillGoodsDOMapper.deductStock(seckillGoodsDO.getId());
            if (count == 0)
                throw new BizException(ResponseCodeEnum.SECKILL_GOODS_SOLD_OUT);

            // 8.创建订单
            SeckillOrderDO order = SeckillOrderDO.builder()
                    .userId(userId)
                    .activityId(activityId)
                    .goodsId(goodsId)
                    .orderNo(orderNo)
                    .seckillPrice(seckillGoodsDO.getSeckillPrice())
                    .goodsName(goodsDO.getGoodsName())
                    .goodsImg(goodsDO.getGoodsImg())
                    .status(OrderStatusEnum.PENDING_PAYMENT.getStatus())
                    .expireTime(expireTime)
                    .isDeleted(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            try {
                seckillOrderDOMapper.insert(order);
            } catch (DuplicateKeyException e) {
                log.warn("==> 重复下单，userId：{}，activityId：{}，goodsId：{}", userId, activityId, goodsId);
                throw new BizException(ResponseCodeEnum.SECKILL_ORDER_DUPLICATE);
            }

            return order;
        });

        log.info("==> 秒杀下单成功，orderId：{}，orderNo：{}", orderDO.getId(), orderNo);

        // 9.组装响应数据
        DoSeckillRspVo rspVo = DoSeckillRspVo.builder()
                .orderId(orderDO.getId())
                .orderNo(orderNo)
                .goodsName(goodsDO.getGoodsName())
                .goodsImg(goodsDO.getGoodsImg())
                .seckillPrice(seckillGoodsDO.getSeckillPrice())
                .status(OrderStatusEnum.PENDING_PAYMENT.getStatus())
                .expireTime(expireTime)
                .build();

        return Response.success(rspVo);
    }
}
