package com.dgq.seckill.common.constant;

import java.time.format.DateTimeFormatter;

/**
 * 日期常量
 */
public class DateConstants {

    public static final DateTimeFormatter DATE_FORMAT_Y_M_D_H_M_S = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final DateTimeFormatter DATE_FORMAT_Y_M_D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final DateTimeFormatter DATE_FORMAT_H_M_S = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static final DateTimeFormatter DATE_FORMAT_Y_M = DateTimeFormatter.ofPattern("yyyy-MM");
}
