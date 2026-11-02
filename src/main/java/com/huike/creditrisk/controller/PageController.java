package com.huike.creditrisk.controller;

import com.huike.creditrisk.pojo.ClientRisk;
import com.huike.creditrisk.pojo.PageResult;
import com.huike.creditrisk.service.ClientService;
import com.huike.creditrisk.service.RiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
public class PageController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private RiskService riskService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("metrics", riskService.getMetrics());
        model.addAttribute("algorithm", riskService.getAlgorithm());
        model.addAttribute("features", riskService.getFeatureImportance());
        return "index";
    }

    @GetMapping("/clients")
    public String clients(Model model,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "15") int size,
                          @RequestParam(required = false) Integer sex,
                          @RequestParam(required = false) Integer education,
                          @RequestParam(required = false) Integer minAge,
                          @RequestParam(required = false) Integer maxAge,
                          @RequestParam(required = false) Integer minDelay,
                          @RequestParam(required = false) Integer defaultFlag) {
        PageResult<ClientRisk> pr = clientService.page(page, size, sex, education,
                minAge, maxAge, minDelay, defaultFlag);
        Map<String, Object> filters = new HashMap<>();
        filters.put("sex", sex);
        filters.put("education", education);
        filters.put("minAge", minAge);
        filters.put("maxAge", maxAge);
        filters.put("minDelay", minDelay);
        filters.put("defaultFlag", defaultFlag);
        model.addAttribute("page", pr);
        model.addAttribute("filters", filters);
        return "clients";
    }

    @GetMapping("/clients/{id}")
    public String detail(@PathVariable("id") Integer id, Model model) {
        ClientRisk cr = clientService.detail(id);
        if (cr == null) {
            return "redirect:/clients";
        }
        model.addAttribute("cr", cr);
        model.addAttribute("metrics", riskService.getMetrics());
        return "client-detail";
    }

    @GetMapping("/predict")
    public String predict() {
        return "predict";
    }
}
