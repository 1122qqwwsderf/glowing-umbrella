package com.huike.creditrisk.pojo;

import lombok.Data;

/**
 * 信用卡客户（对应表 t_client）
 */
@Data
public class Client {
    private Integer id;
    private Integer limitBal;      // 信用额度
    private Integer sex;           // 1男 2女
    private Integer education;     // 1研究生 2大学 3高中 4其他 5/6未知
    private Integer marriage;      // 1已婚 2单身 3其他 0未知
    private Integer age;

    private Integer pay0;          // 9月还款状态（-2~8）
    private Integer pay2;
    private Integer pay3;
    private Integer pay4;
    private Integer pay5;
    private Integer pay6;

    private Integer billAmt1;
    private Integer billAmt2;
    private Integer billAmt3;
    private Integer billAmt4;
    private Integer billAmt5;
    private Integer billAmt6;

    private Integer payAmt1;
    private Integer payAmt2;
    private Integer payAmt3;
    private Integer payAmt4;
    private Integer payAmt5;
    private Integer payAmt6;

    private Integer defaultFlag;   // 1 违约 / 0 正常
}
