# 还款延迟行为与违约预警系统

实践周大作业：**利用各月还款延迟状态构建风险指标，并建立违约预警模型**。
Spring Boot + MyBatis + MySQL + Thymeleaf + ECharts 前后端一体。

## 一、功能概览

| 页面 | 地址 | 功能 |
|---|---|---|
| 数据看板 | `/` | 深色科技风数据大屏：发光 KPI 卡、5 张暗色图表、实时时钟、60 秒自动刷新；支持 `?anim=0` 关闭数字滚动动画（方便截图/做 PPT） |
| 客户风险列表 | `/clients` | 深色科技风列表：筛选（性别/教育/年龄/最大延迟/违约）+ 分页，每行显示模型预测概率与风险等级 |
| 客户详情 | `/clients/{id}` | 深色科技风：6 个月还款状态（颜色标注）、账单与还款对照、风险指标、预警结果与建议 |
| 预警预测 | `/predict` | 深色科技风表单：手工输入客户特征 → 实时输出违约概率/风险等级/主要风险因素（支持"载入示例客户"） |
| 后台管理 | `/admin` | 数据库信息（MySQL 版本/库名/表行数）、模型信息（算法/指标/阈值）、快捷操作入口 |
| 数据管理 | `/admin/clients` | 客户数据筛选/分页/删除（物理删除） |
| 数据导入 | `/admin/import` | 上传 UCI 原始 `.xls` 文件 → 校验表头 → 清空重建 t_client 并批量导入（已实测 30000 行） |

### 后台 API

| 接口 | 说明 |
|---|---|
| `GET /api/admin/summary` | 后台概览（数据库 + 模型信息） |
| `DELETE /api/admin/clients/{id}` | 删除客户 |
| `POST /api/admin/import` | multipart 上传 .xls 导入（字段名 file） |

后端接口：`GET /api/stats`（看板数据）、`POST /api/predict`（预警预测）、`GET /api/clients/{id}`。

## 二、数据库（MySQL，root/123456）

- 库：`credit_default`，表：`t_client`（30000 条，UCI credit card default 数据集）
- 建表/数据导入：
  ```bash
  py tools/import_credit.py
  ```
  （或参照 `sql/credit.sql` 手动建表，再用 Navicat 导入 Excel 数据）

## 三、预警模型

- **特征工程（还款延迟风险指标）**：`max_delay`（观察期最大延迟月数）、`delay_months`（逾期月份数）、`avg_delay`（平均延迟月数）、`util_ratio`（额度使用率）、`pay_ratio`（还款覆盖比例），叠加额度/账单/还款等原始字段，共 29 维特征。
- **模型**：逻辑回归（StandardScaler 标准化），训练/测试按 8:2 划分。
- **指标**：准确率 0.8065，精确率 0.637，召回率 0.291，F1 0.399，**AUC 0.7415**；随机森林对比 AUC 0.7591。
- **特征重要性**（|系数|）：delay_months > max_delay > limit_bal > pay_0 ...
- 模型系数由 `tools/train_model.py` 导出为 `src/main/resources/model/model.json`，Java 端 `RiskService` 加载后在线计算 sigmoid 概率，**无需 Python 运行环境**。
- 风险等级阈值：概率 ≥0.5 高、≥0.2 中、否则低。

## 四、运行

1. 确认 MySQL 已启动，且已执行 `tools/import_credit.py`（库 credit_default 存在）；
2. IDEA 打开本目录（或命令行）：
   ```bash
   mvn spring-boot:run
   ```
3. 浏览器访问 <http://localhost:8082/>

> 端口在 `application.properties` 中配置（默认 8082）。

## 五、目录结构

```
credit-risk/
├── pom.xml                     # Spring Boot 2.7.6 + MyBatis + MySQL + Druid
├── sql/credit.sql              # 建表脚本（参考）
├── tools/
│   ├── import_credit.py        # xls → MySQL 数据导入
│   └── train_model.py          # 特征工程 + 逻辑回归训练 → model.json
├── model/model.json            # 模型系数（与 resources 下同名文件一致）
└── src/main/
    ├── java/com/huike/creditrisk/
    │   ├── CreditRiskApplication.java
    │   ├── controller/PageController.java   ApiController.java
    │   ├── mapper/ClientMapper.java
    │   ├── service/RiskService.java（模型加载+预测）
    │   ├── service/ClientService.java + impl
    │   └── pojo/（Client / RiskResult / ClientRisk / PageResult）
    └── resources/
        ├── application.properties
        ├── mapper/ClientMapper.xml
        ├── model/model.json
        ├── templates/（index / clients / client-detail / predict）
        └── static/（bootstrap、echarts、app.css）
```

## 六、答辩要点提示

1. **题目对应**：风险指标完全围绕"各月还款延迟状态"构建（最大延迟、逾期月份数、平均延迟），并用图表验证"延迟越严重、违约率越高"——见看板前两张图；
2. **模型可解释**：逻辑回归系数直接给出各因素方向与强度，特征重要性图中 delay_months、max_delay 排名第一、第二；
3. **工程完整**：数据导入 → 数据库 → 后端接口 → 前端可视化 → 在线预警，全链路可运行；
4. 可现场演示：在 `/predict` 把近 3 个月还款状态改成"5 延迟"，概率会从 ~14% 升到 ~67%（高危预警）。
