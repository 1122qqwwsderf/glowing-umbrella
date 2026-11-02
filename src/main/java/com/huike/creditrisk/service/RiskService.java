package com.huike.creditrisk.service;

import com.huike.creditrisk.pojo.Client;
import com.huike.creditrisk.pojo.RiskResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 违约预警服务：加载 model.json（逻辑回归系数），在 Java 中完成预测。
 */
@Service
public class RiskService {

    private String[] features;
    private double[] mean;
    private double[] scale;
    private double[] coef;
    private double intercept;
    private double thresholdMedium;
    private double thresholdHigh;
    private Map<String, Object> metrics = new LinkedHashMap<>();
    private String algorithm;
    private List<Map<String, Object>> featureImportance = new ArrayList<>();

    @PostConstruct
    public void load() throws Exception {
        ObjectMapper om = new ObjectMapper();
        try (InputStream in = new ClassPathResource("model/model.json").getInputStream()) {
            JsonNode root = om.readTree(in);
            features = om.convertValue(root.get("features"), String[].class);
            mean = om.convertValue(root.get("mean"), double[].class);
            scale = om.convertValue(root.get("scale"), double[].class);
            coef = om.convertValue(root.get("coef"), double[].class);
            intercept = root.get("intercept").asDouble();
            JsonNode th = root.get("thresholds");
            thresholdMedium = th.get("medium").asDouble();
            thresholdHigh = th.get("high").asDouble();
            metrics = om.convertValue(root.get("metrics"), Map.class);
            algorithm = root.get("algorithm").asText();
            List<Map<String, Object>> imp = new ArrayList<>();
            for (int i = 0; i < features.length; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", features[i]);
                m.put("coef", Math.round(coef[i] * 1000) / 1000.0);
                m.put("abs", Math.abs(coef[i]));
                imp.add(m);
            }
            imp.sort((a, b) -> Double.compare((Double) b.get("abs"), (Double) a.get("abs")));
            featureImportance = imp;
        }
    }

    /** 由客户数据构造特征向量（与训练时顺序一致，含衍生指标） */
    public double[] buildFeatures(Client c) {
        int[] pay = {c.getPay0(), c.getPay2(), c.getPay3(), c.getPay4(), c.getPay5(), c.getPay6()};
        int[] bill = {c.getBillAmt1(), c.getBillAmt2(), c.getBillAmt3(), c.getBillAmt4(), c.getBillAmt5(), c.getBillAmt6()};
        int[] payAmt = {c.getPayAmt1(), c.getPayAmt2(), c.getPayAmt3(), c.getPayAmt4(), c.getPayAmt5(), c.getPayAmt6()};

        double maxDelay = Integer.MIN_VALUE;
        int delayMonths = 0;
        double paySum = 0, billSum = 0, payAmtSum = 0;
        for (int i = 0; i < 6; i++) {
            maxDelay = Math.max(maxDelay, pay[i]);
            if (pay[i] > 0) delayMonths++;
            paySum += pay[i];
            billSum += bill[i];
            payAmtSum += payAmt[i];
        }
        double meanPay = paySum / 6.0;
        double meanBill = billSum / 6.0;
        double meanPayAmt = payAmtSum / 6.0;
        double utilRatio = Math.max(0, meanBill / Math.max(1, c.getLimitBal()));
        double payRatio = Math.min(2, Math.max(0, meanPayAmt / (Math.max(0, meanBill) + 1)));

        double[] v = new double[features.length];
        v[0] = c.getLimitBal();
        v[1] = c.getSex();
        v[2] = c.getEducation();
        v[3] = c.getMarriage();
        v[4] = c.getAge();
        v[5] = c.getPay0();
        v[6] = c.getPay2();
        v[7] = c.getPay3();
        v[8] = c.getPay4();
        v[9] = c.getPay5();
        v[10] = c.getPay6();
        v[11] = c.getBillAmt1();
        v[12] = c.getBillAmt2();
        v[13] = c.getBillAmt3();
        v[14] = c.getBillAmt4();
        v[15] = c.getBillAmt5();
        v[16] = c.getBillAmt6();
        v[17] = c.getPayAmt1();
        v[18] = c.getPayAmt2();
        v[19] = c.getPayAmt3();
        v[20] = c.getPayAmt4();
        v[21] = c.getPayAmt5();
        v[22] = c.getPayAmt6();
        v[23] = maxDelay;
        v[24] = delayMonths;
        v[25] = meanPay;
        v[26] = utilRatio;
        v[27] = payRatio;
        return v;
    }

    /** 预测违约概率与风险等级 */
    public RiskResult predict(Client c) {
        double[] x = buildFeatures(c);
        double z = intercept;
        for (int i = 0; i < features.length; i++) {
            z += coef[i] * (x[i] - mean[i]) / scale[i];
        }
        double p = 1.0 / (1.0 + Math.exp(-z));

        RiskResult r = new RiskResult();
        r.setProbability(Math.round(p * 10000) / 10000.0);
        if (p >= thresholdHigh) {
            r.setRiskLevel("高");
            r.setAdvice("违约风险高，建议下调授信额度、加强还款提醒或要求补充担保。");
        } else if (p >= thresholdMedium) {
            r.setRiskLevel("中");
            r.setAdvice("存在一定违约风险，建议关注近期还款行为，必要时进行电话催收。");
        } else {
            r.setRiskLevel("低");
            r.setAdvice("违约风险较低，维持常规管理即可。");
        }
        Map<String, Object> ind = new LinkedHashMap<>();
        ind.put("maxDelay", (int) x[23]);
        ind.put("delayMonths", (int) x[24]);
        ind.put("avgDelay", Math.round(x[25] * 100) / 100.0);
        ind.put("utilRatio", Math.round(x[26] * 1000) / 1000.0);
        ind.put("payRatio", Math.round(x[27] * 1000) / 1000.0);
        r.setIndicators(ind);

        List<String> factors = new ArrayList<>();
        if (x[24] >= 3) factors.add("逾期月份数达 " + (int) x[24] + " 个月");
        else if (x[24] > 0) factors.add("近 6 个月存在 " + (int) x[24] + " 个月逾期");
        if (x[23] >= 3) factors.add("最大延迟 " + (int) x[23] + " 个月");
        if (x[26] > 0.8) factors.add("额度使用率较高（" + Math.round(x[26] * 100) + "%）");
        if (x[27] < 0.1 && x[26] > 0.3) factors.add("还款覆盖比例偏低");
        if (factors.isEmpty()) factors.add("各项指标处于正常范围");
        r.setTopFactors(factors);
        return r;
    }

    public Map<String, Object> getMetrics() { return metrics; }
    public String getAlgorithm() { return algorithm; }
    public List<Map<String, Object>> getFeatureImportance() { return featureImportance; }
    public double getThresholdMedium() { return thresholdMedium; }
    public double getThresholdHigh() { return thresholdHigh; }
    public String[] getFeatures() { return features; }
    public double[] getCoef() { return Arrays.copyOf(coef, coef.length); }
}
