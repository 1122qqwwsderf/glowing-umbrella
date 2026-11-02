# -*- coding: utf-8 -*-
"""
从 MySQL 读取数据 → 构建还款延迟风险指标 → 训练逻辑回归违约预警模型
输出 model.json（Spring Boot 加载该文件在 Java 中做预测）
"""
import json
import os
import numpy as np
import pandas as pd
import pymysql
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import (accuracy_score, precision_score, recall_score,
                             f1_score, roc_auc_score)

OUT_DIRS = [r"D:\yelang\credit-risk\model", r"D:\yelang\credit-risk\src\main\resources\model"]

DB = dict(host="127.0.0.1", port=3306, user="root", password="123456", database="credit_default", charset="utf8mb4")

FEATURES = ["limit_bal", "sex", "education", "marriage", "age",
            "pay_0", "pay_2", "pay_3", "pay_4", "pay_5", "pay_6",
            "bill_amt1", "bill_amt2", "bill_amt3", "bill_amt4", "bill_amt5", "bill_amt6",
            "pay_amt1", "pay_amt2", "pay_amt3", "pay_amt4", "pay_amt5", "pay_amt6",
            "max_delay", "delay_months", "avg_delay", "util_ratio", "pay_ratio"]


def build(df):
    """原始字段 → 特征矩阵（含还款延迟行为衍生指标）"""
    pay_cols = ["pay_0", "pay_2", "pay_3", "pay_4", "pay_5", "pay_6"]
    bill_cols = [f"bill_amt{i}" for i in range(1, 7)]
    pay_amt_cols = [f"pay_amt{i}" for i in range(1, 7)]

    df = df.copy()
    df["max_delay"] = df[pay_cols].max(axis=1)                       # 观察期最大延迟月数
    df["delay_months"] = (df[pay_cols] > 0).sum(axis=1)              # 有延迟的月份数
    df["avg_delay"] = df[pay_cols].mean(axis=1)                      # 平均延迟月数
    mean_bill = df[bill_cols].mean(axis=1)
    mean_pay = df[pay_amt_cols].mean(axis=1)
    df["util_ratio"] = (mean_bill / df["limit_bal"].clip(lower=1)).clip(lower=0)  # 额度使用率
    df["pay_ratio"] = (mean_pay / (mean_bill.clip(lower=0) + 1)).clip(0, 2)       # 还款覆盖比例
    return df


def main():
    conn = pymysql.connect(**DB)
    df = pd.read_sql("SELECT * FROM t_client", conn)
    conn.close()
    print("数据:", df.shape)

    df = build(df)
    X = df[FEATURES].replace([np.inf, -np.inf], 0).fillna(0).values.astype(float)
    y = df["default_flag"].values.astype(int)

    X_tr, X_te, y_tr, y_te = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y)
    scaler = StandardScaler().fit(X_tr)
    Xtr = scaler.transform(X_tr)
    Xte = scaler.transform(X_te)

    # —— 逻辑回归（预警模型） ——
    lr = LogisticRegression(max_iter=3000, C=1.0).fit(Xtr, y_tr)
    p = lr.predict_proba(Xte)[:, 1]
    yp = (p >= 0.5).astype(int)
    metrics = {
        "accuracy": round(accuracy_score(y_te, yp), 4),
        "precision": round(precision_score(y_te, yp), 4),
        "recall": round(recall_score(y_te, yp), 4),
        "f1": round(f1_score(y_te, yp), 4),
        "auc": round(roc_auc_score(y_te, p), 4),
        "n_total": int(len(df)),
        "n_default": int(y.sum()),
        "default_rate": round(float(y.mean()), 4),
    }
    print("逻辑回归:", metrics)

    # —— 随机森林（对比，仅报告指标） ——
    rf = RandomForestClassifier(n_estimators=200, random_state=42, n_jobs=-1).fit(Xtr, y_tr)
    p2 = rf.predict_proba(Xte)[:, 1]
    y2 = (p2 >= 0.5).astype(int)
    print("随机森林:", {
        "accuracy": round(accuracy_score(y_te, y2), 4),
        "precision": round(precision_score(y_te, y2), 4),
        "recall": round(recall_score(y_te, y2), 4),
        "f1": round(f1_score(y_te, y2), 4),
        "auc": round(roc_auc_score(y_te, p2), 4),
    })

    coef = lr.coef_[0].tolist()
    imp = sorted(zip(FEATURES, coef), key=lambda t: -abs(t[1]))
    print("特征重要性(前8):", [(n, round(c, 3)) for n, c in imp[:8]])

    model = {
        "features": FEATURES,
        "mean": scaler.mean_.tolist(),
        "scale": scaler.scale_.tolist(),
        "coef": coef,
        "intercept": float(lr.intercept_[0]),
        "thresholds": {"medium": 0.2, "high": 0.5},
        "metrics": metrics,
        "algorithm": "LogisticRegression (StandardScaler)",
        "trained_at": "2026-11-02",
    }
    for d in OUT_DIRS:
        os.makedirs(d, exist_ok=True)
        with open(os.path.join(d, "model.json"), "w", encoding="utf-8") as f:
            json.dump(model, f, ensure_ascii=False, indent=2)
    print("model.json 已保存:", OUT_DIRS)


if __name__ == "__main__":
    main()
