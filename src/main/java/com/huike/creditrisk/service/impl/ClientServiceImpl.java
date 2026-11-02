package com.huike.creditrisk.service.impl;

import com.huike.creditrisk.mapper.ClientMapper;
import com.huike.creditrisk.pojo.Client;
import com.huike.creditrisk.pojo.ClientRisk;
import com.huike.creditrisk.pojo.PageResult;
import com.huike.creditrisk.pojo.RiskResult;
import com.huike.creditrisk.service.ClientService;
import com.huike.creditrisk.service.RiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    private ClientMapper clientMapper;

    @Autowired
    private RiskService riskService;

    @Override
    public PageResult<ClientRisk> page(int page, int size,
                                       Integer sex, Integer education,
                                       Integer minAge, Integer maxAge,
                                       Integer minDelay, Integer defaultFlag) {
        page = Math.max(1, page);
        size = Math.min(50, Math.max(5, size));
        long total = clientMapper.countPage(sex, education, minAge, maxAge, minDelay, defaultFlag);
        int pages = (int) Math.max(1, (total + size - 1) / size);
        page = Math.min(page, pages);
        List<Client> clients = clientMapper.selectPage(sex, education, minAge, maxAge, minDelay,
                defaultFlag, (page - 1) * size, size);
        List<ClientRisk> list = new ArrayList<>();
        for (Client c : clients) {
            ClientRisk cr = new ClientRisk();
            cr.setClient(c);
            RiskResult r = riskService.predict(c);
            cr.setRisk(r);
            cr.setMaxDelay((Integer) r.getIndicators().get("maxDelay"));
            cr.setDelayMonths((Integer) r.getIndicators().get("delayMonths"));
            list.add(cr);
        }
        PageResult<ClientRisk> pr = new PageResult<>();
        pr.setList(list);
        pr.setTotal(total);
        pr.setPage(page);
        pr.setSize(size);
        pr.setPages(pages);
        return pr;
    }

    @Override
    public ClientRisk detail(Integer id) {
        Client c = clientMapper.selectById(id);
        if (c == null) {
            return null;
        }
        ClientRisk cr = new ClientRisk();
        cr.setClient(c);
        RiskResult r = riskService.predict(c);
        cr.setRisk(r);
        cr.setMaxDelay((Integer) r.getIndicators().get("maxDelay"));
        cr.setDelayMonths((Integer) r.getIndicators().get("delayMonths"));
        return cr;
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> summary = new LinkedHashMap<>(clientMapper.summary());
        double total = num(summary.get("total"));
        double bad = num(summary.get("bad"));
        summary.put("defaultRate", total > 0 ? round2(bad / total * 100) : 0);
        out.put("summary", summary);

        out.put("byMaxDelay", convert(clientMapper.byMaxDelay(), "d"));
        out.put("byDelayMonths", convert(clientMapper.byDelayMonths(), "k"));
        out.put("bySex", convert(clientMapper.bySex(), "sex"));
        out.put("byAgeGroup", convert(clientMapper.byAgeGroup(), "g"));
        out.put("byPay0", convert(clientMapper.byPay0(), "s"));

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("algorithm", riskService.getAlgorithm());
        model.put("metrics", riskService.getMetrics());
        model.put("thresholds", mapOf("medium", riskService.getThresholdMedium(), "high", riskService.getThresholdHigh()));
        model.put("topFeatures", riskService.getFeatureImportance().subList(0, Math.min(8, riskService.getFeatureImportance().size())));
        out.put("model", model);
        return out;
    }

    private List<Map<String, Object>> convert(List<Map<String, Object>> rows, String keyName) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put(keyName, r.get(keyName));
            double n = num(r.get("n"));
            double bad = num(r.get("bad"));
            m.put("n", (long) n);
            m.put("bad", (long) bad);
            m.put("rate", n > 0 ? round2(bad / n * 100) : 0);
            out.add(m);
        }
        return out;
    }

    private static double num(Object o) {
        return o == null ? 0 : ((Number) o).doubleValue();
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }
}
