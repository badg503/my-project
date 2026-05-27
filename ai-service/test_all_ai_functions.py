# -*- coding: utf-8 -*-
"""
AI 功能测试脚本
测试所有 AI 接口的完整性和动态参数调整功能
"""

import requests
import json
import time
from datetime import datetime

BASE_URL = "http://localhost:5000"

def print_header(title):
    print("\n" + "=" * 70)
    print(f"  {title}")
    print("=" * 70)

def print_result(name, response, duration):
    status = "✅" if response.status_code == 200 else "❌"
    print(f"\n{status} {name}")
    print(f"   状态码：{response.status_code}")
    print(f"   响应时间：{duration:.2f}ms")
    try:
        data = response.json()
        print(f"   响应数据：{json.dumps(data, ensure_ascii=False, indent=2)[:500]}")
    except:
        print(f"   响应内容：{response.text[:200]}")

def test_qa():
    """测试 1: BERT 智能问答"""
    print_header("测试 1: BERT 智能问答接口")
    
    questions = [
        "如何预约实验室",
        "怎么预约实验室",
        "咋预约实验室",
        "设备坏了",
        "设备报修流程",
        "忘记密码怎么办"
    ]
    
    for q in questions:
        start = time.time()
        response = requests.get(f"{BASE_URL}/ai/qa", params={"q": q})
        duration = (time.time() - start) * 1000
        print_result(f"问题：{q}", response, duration)
    
    # 测试动态阈值
    print("\n📊 测试动态阈值调整")
    response = requests.get(f"{BASE_URL}/ai/qa/threshold", params={"threshold": 0.72})
    print_result("阈值设置 (0.72)", response, 0)

def test_fault_predict():
    """测试 2: 设备故障预测"""
    print_header("测试 2: 设备故障预测接口")
    
    # 测试不同设备
    for device_id in [1, 2, 3]:
        start = time.time()
        response = requests.get(
            f"{BASE_URL}/ai/fault-predict",
            params={"device_id": device_id, "threshold": 0.6}
        )
        duration = (time.time() - start) * 1000
        print_result(f"设备 {device_id} 故障预测", response, duration)
    
    # 测试动态阈值
    print("\n📊 测试动态阈值调整")
    for threshold in [0.5, 0.6, 0.7]:
        response = requests.get(
            f"{BASE_URL}/ai/fault-predict",
            params={"device_id": 1, "threshold": threshold}
        )
        print_result(f"阈值={threshold}", response, 0)

def test_fault_learn():
    """测试 3: 故障反馈学习"""
    print_header("测试 3: 故障反馈学习接口")
    
    import numpy as np
    # 生成模拟历史数据
    history_data = (np.random.rand(30, 3) * 10).tolist()
    
    payload = {
        "deviceId": "1",
        "history": history_data,
        "actualFault": 1
    }
    
    start = time.time()
    response = requests.post(
        f"{BASE_URL}/ai/fault-learn",
        json=payload
    )
    duration = (time.time() - start) * 1000
    print_result("故障反馈学习", response, duration)

def test_safety_detect():
    """测试 4: 安全检测"""
    print_header("测试 4: 安全检测接口")
    
    # 测试不同配置
    configs = [
        {"lab_id": 1, "threshold": 0.8, "is_working_time": True},
        {"lab_id": 2, "threshold": 0.7, "is_working_time": False},
        {"lab_id": 3, "threshold": 0.9, "is_working_time": True}
    ]
    
    for config in configs:
        start = time.time()
        response = requests.get(
            f"{BASE_URL}/ai/safety-detect",
            params={
                "lab_id": config["lab_id"],
                "threshold": config["threshold"],
                "is_working_time": str(config["is_working_time"]).lower()
            }
        )
        duration = (time.time() - start) * 1000
        print_result(
            f"实验室 {config['lab_id']} (阈值:{config['threshold']}, 工作时间:{config['is_working_time']})",
            response,
            duration
        )

def test_schedule():
    """测试 5: 智能预约调度"""
    print_header("测试 5: 智能预约调度接口")
    
    # GET 请求（使用默认数据）
    start = time.time()
    response = requests.get(f"{BASE_URL}/ai/schedule")
    duration = (time.time() - start) * 1000
    print_result("GET - 默认数据", response, duration)
    
    # POST 请求（使用真实数据）
    print("\n📊 测试 POST 请求（真实数据）")
    real_data = {
        "labs": [
            {"id": 1, "name": "实验室 1", "capacity": 30},
            {"id": 2, "name": "实验室 2", "capacity": 40}
        ],
        "reserves": [
            {"id": 1, "labId": 1, "startTime": "2026-03-22 09:00:00", "endTime": "2026-03-22 11:00:00"},
            {"id": 2, "labId": 1, "startTime": "2026-03-22 14:00:00", "endTime": "2026-03-22 16:00:00"}
        ]
    }
    
    start = time.time()
    response = requests.post(
        f"{BASE_URL}/ai/schedule",
        json=real_data
    )
    duration = (time.time() - start) * 1000
    print_result("POST - 真实数据", response, duration)

def test_analysis():
    """测试 6: AI 数据分析"""
    print_header("测试 6: AI 数据分析接口")
    
    # GET 请求（使用默认数据）
    start = time.time()
    response = requests.get(f"{BASE_URL}/ai/analysis")
    duration = (time.time() - start) * 1000
    print_result("GET - 默认数据", response, duration)
    
    # POST 请求（使用真实数据）
    print("\n📊 测试 POST 请求（真实数据）")
    real_data = {
        "reserves": [
            {"id": 1, "labId": 1, "startTime": "2026-03-22 09:00:00", "endTime": "2026-03-22 11:00:00"},
            {"id": 2, "labId": 2, "startTime": "2026-03-22 10:00:00", "endTime": "2026-03-22 12:00:00"}
        ],
        "faults": [
            {"id": 1, "deviceId": 1, "faultType": "硬件故障", "faultTime": "2026-03-21 15:30:00"},
            {"id": 2, "deviceId": 2, "faultType": "软件故障", "faultTime": "2026-03-22 08:45:00"}
        ]
    }
    
    start = time.time()
    response = requests.post(
        f"{BASE_URL}/ai/analysis",
        json=real_data
    )
    duration = (time.time() - start) * 1000
    print_result("POST - 真实数据", response, duration)

def main():
    print("\n" + "🚀" * 35)
    print("🤖 AI 功能完整性测试")
    print(f"⏰ 测试时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("🚀" * 35)
    
    # 检查服务是否可用
    try:
        response = requests.get(f"{BASE_URL}/ai/qa", params={"q": "测试"}, timeout=5)
        print("✅ AI 服务连接成功")
    except Exception as e:
        print(f"❌ AI 服务连接失败：{e}")
        print("💡 请确保 AI 服务已启动：python app.py")
        return
    
    # 执行所有测试
    test_qa()
    test_fault_predict()
    test_fault_learn()
    test_safety_detect()
    test_schedule()
    test_analysis()
    
    # 总结
    print_header("测试总结")
    print("✅ 所有 AI 接口测试完成")
    print("\n📋 接口列表:")
    print("  1. GET  /ai/qa?q=问题 - BERT 智能问答")
    print("  2. GET  /ai/fault-predict?device_id=1 - 设备故障预测")
    print("  3. POST /ai/fault-learn - 故障反馈学习")
    print("  4. GET  /ai/safety-detect?lab_id=1 - 安全检测")
    print("  5. GET  /ai/schedule - 智能预约调度")
    print("  6. GET  /ai/analysis - AI 数据分析")
    print("\n📊 动态参数调整:")
    print("  - BERT 问答：threshold 参数")
    print("  - 故障预测：threshold 参数")
    print("  - 安全检测：threshold, is_working_time 参数")
    print("\n" + "🎉" * 35)

if __name__ == "__main__":
    main()
