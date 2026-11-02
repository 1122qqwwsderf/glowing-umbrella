package com.huike.creditrisk.pojo;

import lombok.Data;

/**
 * 客户 + 预警结果（列表页使用）
 */
@Data
public class ClientRisk {
    private Client client;
    private RiskResult risk;
    private Integer maxDelay;      // 观察期最大延迟月数
    private Integer delayMonths;   // 延迟月份数
}
