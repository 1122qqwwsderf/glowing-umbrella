package com.huike.creditrisk.controller;

import com.huike.creditrisk.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 后台 API：概览 / 删除客户 / 导入 xls
 */
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return adminService.summary();
    }

    @DeleteMapping("/clients/{id}")
    public Map<String, Object> delete(@PathVariable("id") Integer id) {
        adminService.deleteClient(id);
        Map<String, Object> m = new HashMap<>();
        m.put("ok", true);
        return m;
    }

    @PostMapping("/import")
    public Map<String, Object> importXls(@RequestParam("file") MultipartFile file) {
        try {
            return adminService.importXls(file);
        } catch (IllegalArgumentException e) {
            Map<String, Object> m = new HashMap<>();
            m.put("ok", false);
            m.put("error", e.getMessage());
            return m;
        } catch (Exception e) {
            Map<String, Object> m = new HashMap<>();
            m.put("ok", false);
            m.put("error", "导入失败：" + e.getMessage());
            return m;
        }
    }
}
