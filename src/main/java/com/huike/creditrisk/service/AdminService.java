package com.huike.creditrisk.service;

import com.huike.creditrisk.mapper.ClientMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理服务：数据概览、客户删除、xls 数据导入
 */
@Service
public class AdminService {

    @Autowired
    private ClientMapper clientMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RiskService riskService;

    /** 后台概览：数据库与模型信息 */
    public Map<String, Object> summary() {
        Map<String, Object> out = new LinkedHashMap<>();
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM t_client", Long.class);
        Long bad = jdbc.queryForObject("SELECT IFNULL(SUM(default_flag),0) FROM t_client", Long.class);
        String dbVersion = jdbc.queryForObject("SELECT VERSION()", String.class);
        String dbName = jdbc.queryForObject("SELECT DATABASE()", String.class);
        out.put("total", total == null ? 0 : total);
        out.put("bad", bad == null ? 0 : bad);
        out.put("defaultRate", total != null && total > 0 ? Math.round(bad * 10000.0 / total) / 100.0 : 0);
        out.put("dbVersion", dbVersion);
        out.put("dbName", dbName);
        out.put("tableName", "t_client");
        out.put("serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        out.put("algorithm", riskService.getAlgorithm());
        out.put("metrics", riskService.getMetrics());
        out.put("thresholds", mapOf("medium", riskService.getThresholdMedium(), "high", riskService.getThresholdHigh()));
        return out;
    }

    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    public void deleteClient(Integer id) {
        clientMapper.deleteById(id);
    }

    /** 导入 UCI 原始 xls：校验表头 → 清空 t_client → 批量写入 */
    public Map<String, Object> importXls(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的 .xls 文件");
        }
        List<Object[]> rows = new ArrayList<>();
        int skipped = 0;
        try (Workbook wb = new HSSFWorkbook(file.getInputStream())) {
            Sheet sh = wb.getSheetAt(0);
            Row hdr = sh.getRow(1);
            String h = (hdr == null || hdr.getCell(1) == null) ? "" : hdr.getCell(1).toString().trim();
            if (!"LIMIT_BAL".equalsIgnoreCase(h)) {
                throw new IllegalArgumentException("文件格式不符：请上传原始 UCI 数据集 default of credit card clients.xls");
            }
            for (int r = 2; r <= sh.getLastRowNum(); r++) {
                Row row = sh.getRow(r);
                if (row == null) continue;
                Object[] rec = new Object[25];
                try {
                    for (int c = 0; c < 25; c++) {
                        Cell cell = row.getCell(c);
                        rec[c] = cell == null ? null : (int) cell.getNumericCellValue();
                    }
                } catch (Exception e) {
                    skipped++;
                    continue;
                }
                if (rec[0] == null) {
                    skipped++;
                    continue;
                }
                rows.add(rec);
            }
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("文件中没有可导入的数据");
        }
        jdbc.update("TRUNCATE TABLE t_client");
        String sql = "INSERT INTO t_client (id,limit_bal,sex,education,marriage,age,pay_0,pay_2,pay_3,pay_4,pay_5,pay_6,"
                + "bill_amt1,bill_amt2,bill_amt3,bill_amt4,bill_amt5,bill_amt6,"
                + "pay_amt1,pay_amt2,pay_amt3,pay_amt4,pay_amt5,pay_amt6,default_flag) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        for (int i = 0; i < rows.size(); i += 1000) {
            jdbc.batchUpdate(sql, rows.subList(i, Math.min(i + 1000, rows.size())));
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM t_client", Long.class);
        Long bad = jdbc.queryForObject("SELECT IFNULL(SUM(default_flag),0) FROM t_client", Long.class);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("total", total);
        res.put("bad", bad);
        res.put("skipped", skipped);
        return res;
    }
}
