package com.dgq.seckill.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 秒杀活动状态枚举
 */
@Getter
@AllArgsConstructor
public enum ActivityStatusEnum {

    NOT_STARTED(0, "未开始"),
    ING(1, "进行中"),
    ENDED(2, "已结束"),
    ;

    private final Integer status;

    private final String description;
}
