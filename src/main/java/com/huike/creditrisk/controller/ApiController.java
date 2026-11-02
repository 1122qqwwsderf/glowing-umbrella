package com.huike.creditrisk.controller;

import com.huike.creditrisk.mapper.ClientMapper;
import com.huike.creditrisk.pojo.Client;
import com.huike.creditrisk.pojo.RiskResult;
import com.huike.creditrisk.service.ClientService;
import com.huike.creditrisk.service.RiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private RiskService riskService;

    @Autowired
    private ClientMapper clientMapper;

    /** 看板统计数据 */
    @GetMapping("/api/stats")
    public Map<String, Object> stats() {
        return clientService.stats();
    }

    /** 对自定义客户特征进行违约预警 */
    @PostMapping("/api/predict")
    public RiskResult predict(@RequestBody Client c) {
        return riskService.predict(c);
    }

    /** 按客户 ID 预警 */
    @GetMapping("/api/clients/{id}/risk")
    public RiskResult risk(@PathVariable("id") Integer id) {
        Client c = clientMapper.selectById(id);
        return c == null ? null : riskService.predict(c);
    }

    /** 查询客户原始数据（预测页"载入示例"用） */
    @GetMapping("/api/clients/{id}")
    public Client client(@PathVariable("id") Integer id) {
        return clientMapper.selectById(id);
    }
}
