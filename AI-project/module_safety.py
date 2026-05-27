# -*- coding: utf-8 -*-
"""
智能安全预警模块：基于 YOLOv8 的目标检测
功能：
- 实时摄像头监控
- 危险物体检测
- 安全评分评估
- 检测结果保存
"""
import cv2
import numpy as np
import json
import os
from datetime import datetime
from ultralytics import YOLO

# 全局变量
model = None

# 危险状态跟踪（按实验室 ID 分别跟踪）
danger_status = {}  # {lab_id: {'is_danger': bool, 'danger_start_time': datetime, 'photo_count': int}}

# 危险物体类别映射（移除了 person，只保留真正的危险物品）
danger_classes = {
    'fire': '火焰',
    'smoke': '烟雾',
    'knife': '刀具',
    'gun': '枪支',
    'lighter': '打火机',
    'match': '火柴'
}


def load_yolo_model(model_name="yolov8s.pt"):
    """加载 YOLO 模型"""
    global model
    try:
        # 如果模型名称不包含路径，则使用 AI-project 目录下的模型
        if not os.path.isabs(model_name):
            # 获取当前模块所在目录
            module_dir = os.path.dirname(os.path.abspath(__file__))
            model_path = os.path.join(module_dir, model_name)
        else:
            model_path = model_name
            
        print(f"🔍 尝试加载模型：{model_path}")
        model = YOLO(model_path)
        print(f"✅ YOLO 模型 {model_name} 加载成功")
        return True
    except Exception as e:
        print(f"⚠️ 模型加载失败：{e}")
        return False


def detect_objects(image=None, image_path=None, confidence=0.4, danger_classes_list=None):
    """检测目标"""
    global model
    
    if model is None:
        if not load_yolo_model():
            return [], None
    
    # 读取图像 - 优先使用传入的图像帧
    img = None
    if image is not None:
        # 直接使用传入的图像帧
        print(f"📷 使用传入的图像帧进行检测")
        img = image
    elif image_path is not None and isinstance(image_path, str) and os.path.exists(image_path):
        # 从文件路径读取
        img = cv2.imread(image_path)
        print(f"📷 正在分析图片：{image_path}")
    else:
        # 使用摄像头
        print("⚠️ 使用摄像头实时检测")
        cap = cv2.VideoCapture(0)
        ret, img = cap.read()
        if not ret:
            print("❌ 摄像头读取失败，创建空白图片")
            # 创建一张空白图片（黑色背景，白色文字提示）
            img = np.zeros((480, 640, 3), dtype=np.uint8)
            cv2.putText(img, "Camera Read Failed", (150, 240), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
            cap.release()
    
    # 检测
    try:
        results = model(img, conf=confidence)
    except Exception as e:
        print(f"❌ 检测失败：{e}")
        return [], img
    
    # 解析结果
    detect_items = []
    try:
        for r in results:
            print(f"🔍 检测结果：检测到 {len(r.boxes.cls)} 个目标")
            for i, c in enumerate(r.boxes.cls):
                cls_name = model.names[int(c)]
                conf_value = float(r.boxes.conf[i].item())
                box = r.boxes.xyxy[i].cpu().numpy()
                
                # 使用传入的危险类别列表，如果没有则使用全局字典的键
                if danger_classes_list:
                    # 确保是列表形式
                    if isinstance(danger_classes_list, np.ndarray):
                        check_danger_classes = danger_classes_list.tolist()
                    else:
                        check_danger_classes = list(danger_classes_list)
                else:
                    check_danger_classes = list(danger_classes.keys())
                
                print(f"📌 检测目标：{cls_name}, 置信度：{conf_value}, 危险类别列表：{check_danger_classes}")
                
                # 判断是否是危险物体
                is_danger = bool(str(cls_name) in [str(x) for x in check_danger_classes])
                print(f"  是否危险：{is_danger}, 类型：{type(is_danger)}")
                
                item = {
                    "class": str(cls_name),
                    "confidence": round(float(conf_value), 2),
                    "box": [float(x) for x in box.tolist()],
                    "is_danger": is_danger
                }
                
                detect_items.append(item)
    except Exception as e:
        print(f"❌ 解析结果失败：{e}")
        import traceback
        traceback.print_exc()
        return [], img
    
    return detect_items, img


def analyze_safety(detect_items):
    """安全分析"""
    safety_analysis = {
        "total_objects": len(detect_items),
        "danger_objects": [],
        "safety_score": 100,
        "recommendations": []
    }
    
    print(f"🔍 分析安全：共检测到 {len(detect_items)} 个目标")
    
    for item in detect_items:
        is_danger = item.get("is_danger", False)
        # 确保 is_danger 是布尔值
        if isinstance(is_danger, np.ndarray):
            is_danger = bool(is_danger.any())
        elif not isinstance(is_danger, bool):
            is_danger = bool(is_danger)
        
        print(f"  目标：{item.get('class')}, 是否危险：{is_danger}, 类型：{type(is_danger)}")
        
        # 使用明确的布尔判断
        if is_danger is True or (isinstance(is_danger, bool) and is_danger):
            safety_analysis["danger_objects"].append(item)
            # 降低安全分数
            safety_analysis["safety_score"] -= 10
    
    # 确保分数在 0-100 之间
    safety_analysis["safety_score"] = max(0, safety_analysis["safety_score"])
    
    # 生成建议
    if safety_analysis["danger_objects"]:
        safety_analysis["recommendations"].append("发现危险物品，请注意安全！")
    else:
        safety_analysis["recommendations"].append("未发现明显安全隐患。")
    
    return safety_analysis


def smart_safety_warning(camera_config=None, image_path=None):
    """
    智能安全预警主函数
    
    参数:
        camera_config: 摄像头配置 (从 Java 传递)
        image_path: 图片文件路径 (优先使用)
    """
    print("=" * 50)
    print("开始运行：智能安全预警模块 (YOLOv8)")
    
    # ============================================
    # 使用 Java 传递的摄像头配置
    # ============================================
    if camera_config:
        print(f"📷 使用 Java 传递的摄像头配置：{camera_config.get('name', 'Unknown')}")
        
        # 检查摄像头类型
        camera_type = camera_config.get('type', 'usb')
        camera_url = camera_config.get('url')
        camera_index = camera_config.get('index', 0)
        
        # 获取危险类别列表
        danger_classes_list = camera_config.get('danger_classes', ['person', 'fire', 'smoke'])
        threshold = camera_config.get('alert_threshold', 0.5)
        
        if camera_type == 'usb':
            print("⚠️ 使用 USB 摄像头实时检测")
            # 打开本地摄像头
            cap = cv2.VideoCapture(camera_index)
            
            if cap.isOpened():
                print("📸 摄像头已打开，正在读取帧...")
                ret, frame = cap.read()
                print(f"📸 帧读取结果：ret={ret}, frame shape={frame.shape if ret else 'None'}")
                cap.release()
                
                if ret:
                    print("🔍 开始检测对象...")
                    detect_items, img = detect_objects(frame, confidence=0.4, danger_classes_list=danger_classes_list)
                    print(f"🔍 检测完成，检测到 {len(detect_items)} 个项目")
                else:
                    print("⚠️ 摄像头读取失败，使用图片")
                    detect_items, img = detect_objects(image_path, danger_classes_list=danger_classes_list)
            else:
                print("⚠️ 摄像头打开失败，使用图片")
                detect_items, img = detect_objects(image_path, danger_classes_list=danger_classes_list)
        elif camera_type in ['rtsp', 'http'] and camera_url:
            print("⚠️ 使用网络摄像头实时检测")
            # 打开网络摄像头
            cap = cv2.VideoCapture(camera_url)
            
            if cap.isOpened():
                ret, frame = cap.read()
                cap.release()
                
                if ret:
                    # 检测...
                    detect_items, img = detect_objects(frame, confidence=0.4, danger_classes_list=danger_classes_list)
                else:
                    print("⚠️ 摄像头读取失败，使用图片")
                    detect_items, img = detect_objects(image_path, danger_classes_list=danger_classes_list)
            else:
                print("⚠️ 摄像头打开失败，使用图片")
                detect_items, img = detect_objects(image_path, danger_classes_list=danger_classes_list)
        else:
            # 其他类型摄像头
            print("⚠️ 摄像头类型暂不支持，使用图片")
            detect_items, img = detect_objects(image_path, danger_classes_list=danger_classes_list)
    else:
        # 默认使用本地摄像头
        print("⚠️ 未指定摄像头配置，使用本地摄像头")
        cap = cv2.VideoCapture(0)
        
        if cap.isOpened():
            ret, frame = cap.read()
            cap.release()
            
            if ret:
                detect_items, img = detect_objects(frame, danger_classes_list=['person', 'fire', 'smoke'])
            else:
                print("⚠️ 摄像头读取失败，使用默认图片")
                detect_items, img = detect_objects(image_path, danger_classes_list=['person', 'fire', 'smoke'])
        else:
            print("⚠️ 摄像头打开失败，使用默认图片")
            detect_items, img = detect_objects(image_path, danger_classes_list=['person', 'fire', 'smoke'])
    
    # 2. 安全分析
    print(f"🔍 准备进行安全分析，检测到 {len(detect_items)} 个目标")
    try:
        safety_analysis = analyze_safety(detect_items)
    except Exception as e:
        print(f"❌ 安全分析失败：{e}")
        import traceback
        traceback.print_exc()
        # 返回空结果
        safety_analysis = {
            "total_objects": 0,
            "danger_objects": [],
            "safety_score": 100,
            "recommendations": ["安全分析失败"]
        }
    
    # 3. 显示结果
    print(f"📷 检测到 {len(detect_items)} 个目标")
    print(f"⚠️ 危险目标：{len(safety_analysis['danger_objects'])} 个")
    print(f"📊 安全评分：{safety_analysis['safety_score']}/100")
    
    # 4. 打印危险目标
    for item in safety_analysis["danger_objects"]:
        print(f"🚨 检测到【{item['class']}】,置信度：{item['confidence']}")
    
    # 5. 打印建议
    for rec in safety_analysis["recommendations"]:
        print(f"💡 建议：{rec}")
    
    # 6. 保存检测结果和图片
    photo_info = None
    try:
        photo_info = save_detection_result(detect_items, safety_analysis, img, camera_config)
    except Exception as e:
        print(f"❌ 保存检测结果失败：{e}")
    
    print("✅ 智能安全预警 (YOLOv8) 模块调用完成\n")
    
    # 返回完整结果
    return {
        "detect_items": detect_items,
        "danger_objects": safety_analysis["danger_objects"],
        "safety_score": safety_analysis["safety_score"],
        "recommendations": safety_analysis["recommendations"],
        "photo_info": photo_info
    }


def save_detection_result(detect_items, safety_analysis, img=None, camera_config=None):
    """保存检测结果和图片
    
    照片保存策略：
    1. 安全时：只保留一张最新安全照片（覆盖），删除之前的危险照片
    2. 危险时：
       - 第一次检测到危险：保存危险发生时的照片
       - 危险期间：每 5 秒保存一张新照片（不覆盖）
       - 危险解除：删除危险期间的照片，只保留最新的安全照片
    """
    global danger_status
    
    # 获取脚本所在目录
    import os
    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = os.path.join(script_dir, 'data')
    captures_dir = os.path.join(base_dir, 'captures')
    
    os.makedirs(base_dir, exist_ok=True)
    os.makedirs(captures_dir, exist_ok=True)
    
    # 生成时间戳
    timestamp = datetime.now()
    timestamp_str = timestamp.strftime("%Y%m%d_%H%M%S")
    
    # 获取实验室信息
    lab_id = camera_config.get('lab_id') if camera_config else None
    lab_name = camera_config.get('lab_name') if camera_config else 'unknown'
    
    # 清理实验室名称
    safe_lab_name = str(lab_name).replace(' ', '_').replace('/', '_').replace('\\', '_')
    
    # 获取今天的日期（用于按天保存）
    today_str = timestamp.strftime("%Y%m%d")
    
    # 判断当前是否危险
    danger_objects = safety_analysis.get('danger_objects', [])
    is_danger = len(danger_objects) > 0
    
    # 初始化该实验室的危险状态（如果不存在）
    if lab_id not in danger_status:
        danger_status[lab_id] = {
            'is_danger': False,
            'danger_start_time': None,
            'last_photo_time': None,
            'last_danger_photo_count': 0
        }
    
    current_status = danger_status[lab_id]
    was_danger = current_status['is_danger']
    
    # 保存图片 (如果有)
    if img is not None:
        # 在图片上绘制检测结果
        img_with_boxes = draw_detection_boxes(img.copy(), detect_items)
        
        if is_danger:
            # ========== 危险情况 ==========
            print(f"🚨 检测到危险！危险物体：{[obj['class'] for obj in danger_objects]}")
            
            # 创建今天的文件夹
            today_dir = os.path.join(captures_dir, today_str)
            os.makedirs(today_dir, exist_ok=True)
            
            # 危险照片精确到秒
            timestamp_seconds = timestamp.strftime("%H%M%S")
            
            if not was_danger:
                # 第一次检测到危险：保存"危险发生前"的照片
                print("📸 第一次检测到危险，保存危险发生时的照片...")
                
                # 查找该实验室今天的安全照片作为"危险前"照片
                import glob as glob_module
                safe_pattern = os.path.join(today_dir, f'lab_{lab_id}_{safe_lab_name}_safe.jpg')
                safe_photos = glob_module.glob(safe_pattern)
                if safe_photos:
                    # 使用今天最新的安全照片
                    latest_safe_photo = max(safe_photos, key=os.path.getctime)
                    before_danger_path = os.path.join(today_dir, f'lab_{lab_id}_{safe_lab_name}_danger_before_{timestamp_seconds}.jpg')
                    import shutil
                    shutil.copy(latest_safe_photo, before_danger_path)
                    print(f"📸 已保存危险前照片：{before_danger_path}")
                
                # 保存危险发生时的第一张照片（精确到秒）
                danger_photo_path = os.path.join(today_dir, f'lab_{lab_id}_{safe_lab_name}_danger_start_{timestamp_seconds}.jpg')
                cv2.imwrite(danger_photo_path, img_with_boxes)
                print(f"📸 已保存危险发生时照片：{danger_photo_path}")
                
                # 更新危险状态
                current_status['is_danger'] = True
                current_status['danger_start_time'] = timestamp
                current_status['last_photo_time'] = timestamp
                current_status['last_danger_photo_count'] = 1
            
            else:
                # 危险持续中：每 5 秒保存一张新照片（精确到秒，最多 10 张）
                last_photo_time = current_status.get('last_photo_time')
                time_diff = (timestamp - last_photo_time).total_seconds() if last_photo_time else 999
                
                if time_diff >= 5:
                    # 每 5 秒保存一张新照片，最多保存 10 张
                    photo_count = current_status.get('last_danger_photo_count', 0) + 1
                    
                    # 限制最多 10 张
                    if photo_count > 10:
                        print(f"⚠️ 已达到最大危险照片数量（10 张），不再保存新照片")
                    else:
                        danger_photo_path = os.path.join(today_dir, f'lab_{lab_id}_{safe_lab_name}_danger_{photo_count}_{timestamp_seconds}.jpg')
                        cv2.imwrite(danger_photo_path, img_with_boxes)
                        print(f"📸 危险持续中，保存照片 {photo_count}：{danger_photo_path}")
                        
                        current_status['last_danger_photo_count'] = photo_count
                    
                    current_status['last_photo_time'] = timestamp
        
        else:
            # ========== 安全情况 ==========
            print("✅ 检测到安全，无危险物体")
            
            # 创建今天的文件夹
            today_dir = os.path.join(captures_dir, today_str)
            os.makedirs(today_dir, exist_ok=True)
            
            # 安全照片只按天保存，每天一张（覆盖）
            safe_photo_path = os.path.join(today_dir, f'lab_{lab_id}_{safe_lab_name}_safe.jpg')
            cv2.imwrite(safe_photo_path, img_with_boxes)
            print(f"📸 已保存安全照片（按天）：{safe_photo_path}")
            
            # 如果之前是危险状态，现在解除了
            if was_danger:
                print("🔔 危险解除！保留所有危险照片作为历史记录")
                # 不清理危险照片，保留作为历史记录
                current_status['danger_start_time'] = None
            
            # 更新最后拍照时间
            current_status['last_photo_time'] = timestamp
    
    # 返回照片路径信息（使用相对路径，方便前端访问）
    photo_info = {
        "lab_id": lab_id,
        "lab_name": lab_name,
        "is_danger": is_danger,
        "safe_photo": f'data/captures/{today_str}/lab_{lab_id}_{safe_lab_name}_safe.jpg' if not is_danger else None,
        "danger_photos": []
    }
    
    if is_danger:
        # 返回所有危险照片路径
        import glob as glob_module
        danger_pattern = f'data/captures/{today_str}/lab_{lab_id}_{safe_lab_name}_danger*.jpg'
        danger_photos = glob_module.glob(danger_pattern)
        photo_info["danger_photos"] = danger_photos
    
    return photo_info


def draw_detection_boxes(img, detect_items):
    """在图片上绘制检测框"""
    for item in detect_items:
        box = item.get('box')
        cls_name = item.get('class', 'unknown')
        confidence = item.get('confidence', 0)
        is_danger = item.get('is_danger', False)
        
        if box is not None:
            x1, y1, x2, y2 = map(int, box)
            
            # 危险物体用红色，其他用绿色
            color = (0, 0, 255) if is_danger else (0, 255, 0)
            
            # 绘制矩形框
            cv2.rectangle(img, (x1, y1), (x2, y2), color, 2)
            
            # 绘制标签
            label = f"{cls_name} {confidence:.2f}"
            label_size, _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.5, 2)
            label_y = y1 - 10 if y1 - 10 > 10 else y1 + 20
            
            cv2.rectangle(img, (x1, label_y - label_size[1] - 5), 
                         (x1 + label_size[0], label_y + 5), color, -1)
            cv2.putText(img, label, (x1, label_y), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 2)
    
    return img


if __name__ == "__main__":
    # 测试：使用本地摄像头
    smart_safety_warning(camera_code="local")
