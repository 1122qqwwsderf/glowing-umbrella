package com.huike.creditrisk.pojo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 违约预警结果
 */
@Data
public class RiskResult {
    private double probability;      // 违约概率 0~1
    private String riskLevel;        // 低 / 中 / 高
    private String advice;           // 处置建议
    private Map<String, Object> indicators;   // 风险指标
    private List<String> topFactors;          // 主要风险因素
}
