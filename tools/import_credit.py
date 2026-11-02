# -*- coding: utf-8 -*-
"""
把 UCI 信用卡违约数据集导入 MySQL
库：credit_default  表：t_client（30000 条）
"""
import pandas as pd
import pymysql

XLS = r"C:\Users\叶浪\Desktop\作业\实践周大作业\default of credit card clients.xls"
DB = dict(host="127.0.0.1", port=3306, user="root", password="123456", charset="utf8mb4")

COLS = ["id", "limit_bal", "sex", "education", "marriage", "age",
        "pay_0", "pay_2", "pay_3", "pay_4", "pay_5", "pay_6",
        "bill_amt1", "bill_amt2", "bill_amt3", "bill_amt4", "bill_amt5", "bill_amt6",
        "pay_amt1", "pay_amt2", "pay_amt3", "pay_amt4", "pay_amt5", "pay_amt6",
        "default_flag"]

DDL = """
CREATE TABLE IF NOT EXISTS t_client (
  id           INT PRIMARY KEY COMMENT '客户ID',
  limit_bal    INT           COMMENT '信用额度',
  sex          TINYINT       COMMENT '性别 1男 2女',
  education    TINYINT       COMMENT '教育 1研究生 2大学 3高中 4其他 5/6未知',
  marriage     TINYINT       COMMENT '婚姻 1已婚 2单身 3其他 0未知',
  age          INT           COMMENT '年龄',
  pay_0        TINYINT       COMMENT '2005年9月还款状态(-2~8)',
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
"""


def main():
    df = pd.read_excel(XLS, header=1)
    df = df.iloc[:, :25]
    df.columns = COLS
    df = df.dropna()
    for c in COLS:
        df[c] = df[c].astype(int)
    print("读取数据:", df.shape)
    print("违约分布:", df["default_flag"].value_counts().to_dict())

    conn = pymysql.connect(**DB)
    cur = conn.cursor()
    cur.execute("CREATE DATABASE IF NOT EXISTS credit_default DEFAULT CHARACTER SET utf8mb4")
    cur.execute("USE credit_default")
    cur.execute(DDL)
    cur.execute("TRUNCATE TABLE t_client")

    rows = [tuple(r) for r in df[COLS].itertuples(index=False, name=None)]
    sql = "INSERT INTO t_client (" + ",".join(COLS) + ") VALUES (" + ",".join(["%s"] * len(COLS)) + ")"
    B = 1000
    for i in range(0, len(rows), B):
        cur.executemany(sql, rows[i:i + B])
        conn.commit()
    cur.execute("SELECT COUNT(*), SUM(default_flag) FROM t_client")
    total, bad = cur.fetchone()
    print(f"导入完成: 共 {total} 条, 违约 {bad} 条, 违约率 {bad / total * 100:.2f}%")
    conn.close()


if __name__ == "__main__":
    main()
