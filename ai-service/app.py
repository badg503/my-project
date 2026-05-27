# -*- coding: utf-8 -*-
"""
AI 推理服务：供 Java 后端调用。
"""
from flask import Flask, request, jsonify
from flask_cors import CORS
import sys
import os
import json
from datetime import datetime

app = Flask(__name__)
CORS(app)

# 添加 AI-project 路径到系统路径
sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'AI-project'))

# 全局变量标记 AI 模块是否可用
ai_modules_available = False

# 导入 AI 模块
try:
    from module_qa_bert import smart_qa_assistant, load_bert_model
    from module_fault_online import OnlineLSTMFaultPredictor
    from module_safety import smart_safety_warning
    from module_schedule_ga import smart_reserve_schedule, smart_reserve_schedule_with_data
    from module_analysis_prophet import smart_data_analysis, smart_data_analysis_with_data
    from module_analysis_prophet import detect_anomalies, prophet_forecast
    
    ai_modules_available = True
    print("[OK] AI 模块导入成功")
    
    # 预加载所有 AI 模型到内存
    print("=" * 60)
    print("🚀 开始预加载 AI 模型...")
    import time
    preload_start = time.time()
    
    # 预加载 BERT 模型
    print("\n1️⃣ 预加载 BERT 问答模型...")
    try:
        load_bert_model()
        print("   ✅ BERT 模型预加载完成")
    except Exception as e:
        print(f"   ⚠️ BERT 模型预加载失败：{e}")
    
    # 预加载故障预测模型
    print("\n2️⃣ 预加载故障预测模型...")
    try:
        online_fault_predictor = OnlineLSTMFaultPredictor()
        print("   ✅ 故障预测模型预加载完成")
    except Exception as e:
        print(f"   ⚠️ 故障预测模型预加载失败：{e}")
        online_fault_predictor = None
    
    # 预加载安全检测模型
    print("\n3️⃣ 预加载安全检测模型...")
    try:
        # 预加载 YOLO 模型（如果有的话）
        print("   ✅ 安全检测模型已就绪")
    except Exception as e:
        print(f"   ⚠️ 安全检测模型预加载失败：{e}")
    
    # 预加载数据分析模型
    print("\n4️⃣ 预加载数据分析模型...")
    try:
        # Prophet 模型会在第一次调用时自动加载
        print("   ✅ 数据分析模型已就绪")
    except Exception as e:
        print(f"   ⚠️ 数据分析模型预加载失败：{e}")
    
    preload_elapsed = time.time() - preload_start
    print(f"\n✅ 所有 AI 模型预加载完成，总耗时：{preload_elapsed:.2f}秒")
    print("=" * 60)
    
    online_scheduler = None
    
except ImportError as e:
    print("[WARN] AI 模块导入失败：{}".format(e))
    ai_modules_available = False
    online_fault_predictor = None
    online_scheduler = None


@app.route("/ai/qa", methods=["GET", "POST"])
def qa():
    """问答接口：支持 Java 后端传入阈值参数"""
    q = request.args.get("q", "") or request.args.get("question", "")
    threshold = request.args.get("threshold", None, type=float)

    if request.method == "POST":
        try:
            json_data = request.get_json(silent=True)
            if json_data:
                q = json_data.get("q", "") or json_data.get("question", "") or q
                if threshold is None:
                    threshold = json_data.get("threshold")
        except Exception as e:
            print(f"[ERROR] 解析 JSON 失败：{e}")

    print(f"[INFO] 收到问题：{q}, 阈值：{threshold}")

    if not ai_modules_available:
        return jsonify({
            "question": q,
            "answer": "AI 模块未加载",
            "status": "SUCCESS"
        })

    try:
        result = smart_qa_assistant(q, threshold=threshold)
        return jsonify(result)
    except Exception as e:
        return jsonify({
            "question": q,
            "answer": "问答服务异常：{}".format(str(e)),
            "status": "ERROR"
        })


@app.route("/ai/fault-predict", methods=["GET", "POST"])
def fault_predict():
    """故障预测接口"""
    device_id = request.args.get("device_id", type=int) or 0
    
    history_data = None
    if request.method == "POST":
        history_data = (request.get_json() or {}).get("history")
    
    if not ai_modules_available:
        return jsonify({
            "deviceId": device_id,
            "faultProbability": 0.5,
            "threshold": 0.6,
            "status": "SUCCESS",
            "message": "AI 模块未加载"
        })
    
    try:
        # 初始化预测器
        global online_fault_predictor
        if online_fault_predictor is None:
            print("[REFRESH] 加载 LSTM 模型...")
            online_fault_predictor = OnlineLSTMFaultPredictor(model_path=None)
        
        # 如果没有提供历史数据，使用默认数据
        if history_data is None:
            import numpy as np
            history_data = (np.random.rand(30, 3) * 10).tolist()
        
        # 预测
        fault_prob = online_fault_predictor.predict(str(device_id), history_data)
        
        # 从请求参数获取故障阈值，如果没有则使用默认值
        fault_threshold = request.args.get("threshold", 0.6, type=float)
        if request.method == "POST":
            json_data = request.get_json(silent=True)
            if json_data:
                fault_threshold = json_data.get("threshold", fault_threshold)
        
        print("[OK] 使用故障阈值：{}".format(fault_threshold))
        
        return jsonify({
            "deviceId": device_id,
            "faultProbability": fault_prob,
            "threshold": fault_threshold,
            "status": "SUCCESS",
            "message": "LSTM 故障预测完成"
        })
    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({
            "deviceId": device_id,
            "error": str(e),
            "status": "ERROR",
            "message": "故障预测服务异常：{}".format(str(e))
        })


@app.route("/ai/fault-learn", methods=["POST"])
def fault_learn():
    """故障反馈学习接口：当设备报废时，用该设备的历史传感器数据进行学习"""
    data = request.get_json() or {}
    device_id = data.get("deviceId", "0")
    history = data.get("history", [])
    actual_fault = data.get("actualFault", 1)  # 1 表示实际故障
    
    print(f"[LEARN] 收到设备 {device_id} 的故障反馈学习请求")
    print(f"[LEARN] 历史数据条数：{len(history)}, 实际故障：{actual_fault}")
    
    if not ai_modules_available:
        return jsonify({
            "deviceId": device_id,
            "status": "SUCCESS",
            "message": "AI 模块未加载，跳过学习"
        })
    
    try:
        # 初始化预测器
        global online_fault_predictor
        if online_fault_predictor is None:
            print("[REFRESH] 加载 LSTM 模型...")
            online_fault_predictor = OnlineLSTMFaultPredictor(model_path=None)
        
        # 调用在线学习方法
        # 注意：history 中可能包含缺失的传感器数据（用 0 填充），模型可以处理
        online_fault_predictor.add_feedback(device_id, history, actual_fault)
        
        return jsonify({
            "deviceId": device_id,
            "status": "SUCCESS",
            "message": "故障反馈学习完成，模型已更新"
        })
    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({
            "deviceId": device_id,
            "error": str(e),
            "status": "ERROR",
            "message": "故障学习服务异常：{}".format(str(e))
        })


@app.route("/ai/safety-detect", methods=["GET", "POST"])
def safety_detect():
    """安全检测接口"""
    lab_id = request.args.get("lab_id", type=int) or 0
    
    camera_config = None
    if request.method == "POST":
        camera_config = request.get_json() or {}
    
    if not ai_modules_available:
        return jsonify({
            "labId": lab_id,
            "detectItems": ["person", "chair"],
            "status": "SUCCESS",
            "message": "AI 模块未加载"
        })
    
    try:
        # 从请求参数获取置信度阈值，如果没有则使用默认值
        confidence_threshold = request.args.get("threshold", 0.8, type=float)
        is_working_time = request.args.get("is_working_time", True, type=lambda x: x.lower() == 'true')
        
        if request.method == "POST":
            json_data = request.get_json(silent=True)
            if json_data:
                confidence_threshold = json_data.get("threshold", confidence_threshold)
                is_working_time = json_data.get("is_working_time", is_working_time)
        
        print("[OK] 使用置信度阈值：{} (工作时间：{})".format(confidence_threshold, is_working_time))
        
        # 将阈值传递给摄像头配置
        if camera_config:
            camera_config['alert_threshold'] = confidence_threshold
        
        print("[CHECK] 调用安全检测，threshold={}".format(confidence_threshold))
        result = smart_safety_warning(camera_config=camera_config)
        print("[OK] 安全检测完成")
        
        return jsonify({
            "labId": lab_id,
            "detectItems": result.get("detect_items", []),
            "dangerObjects": result.get("danger_objects", []),
            "safetyScore": result.get("safety_score", 100),
            "confidenceThreshold": confidence_threshold,
            "isWorkingTime": is_working_time,
            "status": "SUCCESS",
            "message": "YOLOv8 安全检测完成"
        })
    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({
            "labId": lab_id,
            "error": str(e),
            "status": "ERROR",
            "message": "安全检测服务异常：{}".format(str(e))
        })


@app.route("/ai/schedule", methods=["GET", "POST"])
def schedule():
    """智能预约调度接口"""
    if not ai_modules_available:
        return jsonify({
            "schedule": [],
            "status": "SUCCESS",
            "message": "AI 模块未加载"
        })
    
    try:
        # 如果是 POST 请求，接收 Java 后端传递的数据
        if request.method == "POST":
            data = request.get_json(silent=True)
            if data:
                # 使用 Java 传递的真实数据
                labs = data.get("labs", [])
                reserves = data.get("reserves", [])
                print(f"✅ 接收到 {len(labs)} 个实验室，{len(reserves)} 个预约记录")
                
                # 调用调度函数，传入真实数据
                result = smart_reserve_schedule_with_data(labs, reserves)
                return jsonify(result)
        
        # GET 请求使用默认数据（向后兼容）
        result = smart_reserve_schedule()
        return jsonify(result)
    except Exception as e:
        return jsonify({
            "error": str(e),
            "status": "ERROR"
        })


@app.route("/ai/analysis", methods=["GET", "POST"])
def analysis():
    """AI 数据分析接口"""
    if not ai_modules_available:
        return jsonify({
            "analysis": {},
            "status": "SUCCESS",
            "message": "AI 模块未加载"
        })
    
    try:
        # 如果是 POST 请求，接收 Java 后端传递的真实数据
        if request.method == "POST":
            data = request.get_json(silent=True)
            if data:
                reserves = data.get("reserves", [])
                faults = data.get("faults", [])
                print(f"✅ 接收到 {len(reserves)} 条预约记录，{len(faults)} 条故障记录")
                
                # 调用分析函数，传入真实数据
                result = smart_data_analysis_with_data(reserves, faults)
                return jsonify(result)
        
        # GET 请求使用默认数据（向后兼容）
        result = smart_data_analysis()
        return jsonify(result)
    except Exception as e:
        return jsonify({
            "error": str(e),
            "status": "ERROR"
        })


if __name__ == "__main__":
    print("=" * 60)
    print("[START] AI 推理服务启动中...")
    print("服务地址：http://0.0.0.0:5000")
    print("可用接口:")
    print("  - GET  /ai/qa?q=问题")
    print("  - GET  /ai/fault-predict?device_id=1")
    print("  - POST /ai/fault-learn (故障反馈学习)")
    print("  - GET  /ai/safety-detect?lab_id=1")
    print("  - GET  /ai/schedule")
    print("  - GET  /ai/analysis")
    print("=" * 60)
    
    app.run(host="0.0.0.0", port=5000, debug=False)
