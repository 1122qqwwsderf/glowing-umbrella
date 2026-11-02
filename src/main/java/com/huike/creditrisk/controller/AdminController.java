package com.huike.creditrisk.controller;

import com.huike.creditrisk.pojo.ClientRisk;
import com.huike.creditrisk.pojo.PageResult;
import com.huike.creditrisk.service.AdminService;
import com.huike.creditrisk.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

/**
 * 后台管理页面
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private ClientService clientService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("summary", adminService.summary());
        return "admin/index";
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
        return "admin/clients";
    }

    @GetMapping("/import")
    public String importPage() {
        return "admin/import";
    }
}
