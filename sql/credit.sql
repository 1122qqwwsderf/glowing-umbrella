-- ============================================================
-- 实践周大作业：还款延迟行为与违约预警
-- 数据库：credit_default  表：t_client（30000 条客户数据）
-- 说明：数据库与数据由 tools/import_credit.py 自动创建导入；
--       本脚本可作为建表参考，或在 Navicat 中手动执行。
-- ============================================================
CREATE DATABASE IF NOT EXISTS credit_default DEFAULT CHARACTER SET utf8mb4;
USE credit_default;

CREATE TABLE IF NOT EXISTS t_client (
  id           INT PRIMARY KEY COMMENT '客户ID',
  limit_bal    INT           COMMENT '信用额度',
  sex          TINYINT       COMMENT '性别 1男 2女',
  education    TINYINT       COMMENT '教育 1研究生 2大学 3高中 4其他 5/6未知',
  marriage     TINYINT       COMMENT '婚姻 1已婚 2单身 3其他 0未知',
  age          INT           COMMENT '年龄',
  pay_0        TINYINT       COMMENT '当季还款状态(-2~8)',
  pay_2        TINYINT       COMMENT '8月',
  pay_3        TINYINT       COMMENT '7月',
  pay_4        TINYINT       COMMENT '6月',
  pay_5        TINYINT       COMMENT '5月',
  pay_6        TINYINT       COMMENT '4月',
  bill_amt1    INT           COMMENT '9月账单金额',
  bill_amt2    INT           COMMENT '8月',
  bill_amt3    INT           COMMENT '7月',
  bill_amt4    INT           COMMENT '6月',
  bill_amt5    INT           COMMENT '5月',
  bill_amt6    INT           COMMENT '4月',
  pay_amt1     INT           COMMENT '9月还款金额',
  pay_amt2     INT           COMMENT '8月',
  pay_amt3     INT           COMMENT '7月',
  pay_amt4     INT           COMMENT '6月',
  pay_amt5     INT           COMMENT '5月',
  pay_amt6     INT           COMMENT '4月',
  default_flag TINYINT       COMMENT '下月是否违约 1是 0否',
  KEY idx_default (default_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用卡客户数据';

-- 导入数据：py tools/import_credit.py
-- 模型训练：py tools/train_model.py
