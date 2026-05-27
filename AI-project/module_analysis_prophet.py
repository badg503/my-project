# -*- coding: utf-8 -*-
"""
AI 数据分析模块 - Prophet 时间序列预测版本
使用 Facebook Prophet 进行趋势预测和异常检测
"""
import pandas as pd
import numpy as np
from pyecharts import options as opts
from pyecharts.charts import Bar, Line, Grid
import os
import json

try:
    from prophet import Prophet
    PROPHET_AVAILABLE = True
    print("✅ Prophet 库加载成功")
except ImportError:
    PROPHET_AVAILABLE = False
    print("⚠️ Prophet 库未安装，使用简化版本")


def prophet_forecast(df, lab_id, periods=7):
    """使用 Prophet 进行预测"""
    if not PROPHET_AVAILABLE:
        return None, None
    
    try:
        # 准备数据
        lab_data = df[df['lab_id'] == lab_id][['ds', 'usage_rate']].copy()
        lab_data.columns = ['ds', 'y']
        
        # 创建并训练模型
        model = Prophet(
            daily_seasonality=False,
            weekly_seasonality=True,
            yearly_seasonality=False
        )
        model.fit(lab_data)
        
        # 预测未来 7 天：从今天开始，而不是从数据最后一天开始
        today = pd.Timestamp.today().normalize()  # 获取今天的日期（去掉时间部分）
        future_dates = pd.date_range(start=today, periods=periods, freq='D')
        future = pd.DataFrame({'ds': future_dates})
        
        forecast = model.predict(future)
        
        # 提取预测结果
        forecast_result = forecast[['ds', 'yhat', 'yhat_lower', 'yhat_upper']]
        
        # 确保预测值在合理范围内 (0-1 之间)
        forecast_result['yhat'] = forecast_result['yhat'].clip(0, 1)
        forecast_result['yhat_lower'] = forecast_result['yhat_lower'].clip(0, 1)
        forecast_result['yhat_upper'] = forecast_result['yhat_upper'].clip(0, 1)
        
        return forecast_result, model
        
    except Exception as e:
        print(f"⚠️ Prophet 预测失败：{e}")
        return None, None

def detect_anomalies(df, lab_id):
    """异常检测：使用 3σ原则"""
    lab_data = df[df['lab_id'] == lab_id]['usage_rate']
    
    mean = lab_data.mean()
    std = lab_data.std()
    
    # 异常点：超过 3 个标准差
    anomalies = lab_data[(lab_data > mean + 3*std) | (lab_data < mean - 3*std)]
    
    return {
        'mean': round(mean, 4),
        'std': round(std, 4),
        'anomaly_count': len(anomalies),
        'anomaly_dates': anomalies.index.tolist() if len(anomalies) > 0 else []
    }

def load_historical_data():
    """加载历史数据（模拟）"""
    # 生成过去 30 天的模拟数据
    dates = pd.date_range(end=pd.Timestamp.today(), periods=30, freq='D')
    data = []
    for lab_id in [1, 2, 3]:
        for date in dates:
            data.append({
                'ds': date,
                'lab_id': lab_id,
                'usage_rate': np.random.uniform(0.3, 0.8),
                'user_count': np.random.randint(10, 50),
                'fault_count': np.random.randint(0, 3)
            })
    return pd.DataFrame(data)


def smart_data_analysis():
    """
    AI 数据分析主函数
    使用 Prophet 进行时间序列预测和趋势分析
    """
    print("=" * 50)
    print("开始运行：AI 数据分析模块（Prophet 时间序列预测）")
    
    # 1. 加载历史数据
    df = load_historical_data()
    
    # 2. 基础统计分析
    stats = {}
    for lab_id in [1, 2, 3]:
        lab_data = df[df['lab_id'] == lab_id]
        stats[lab_id] = {
            'avg_usage_rate': round(lab_data['usage_rate'].mean(), 4),
            'max_usage_rate': round(lab_data['usage_rate'].max(), 4),
            'min_usage_rate': round(lab_data['usage_rate'].min(), 4),
            'total_users': int(lab_data['user_count'].sum()),
            'total_faults': int(lab_data['fault_count'].sum()),
            'anomalies': detect_anomalies(df, lab_id)
        }
    
    print("\n📊 实验室运行统计:")
    for lab_id, stat in stats.items():
        print(f"  实验室{lab_id}:")
        print(f"    - 平均使用率：{stat['avg_usage_rate']:.2%}")
        print(f"    - 总用户数：{stat['total_users']}")
        print(f"    - 故障次数：{stat['total_faults']}")
        print(f"    - 异常天数：{stat['anomalies']['anomaly_count']}")
    
    # 3. Prophet 预测
    forecasts = {}
    if PROPHET_AVAILABLE:
        print("\n🔮 开始预测未来 7 天使用率...")
        for lab_id in [1, 2, 3]:
            forecast, model = prophet_forecast(df, lab_id, periods=7)
            if forecast is not None:
                forecasts[lab_id] = forecast.to_dict('records')
                print(f"  实验室{lab_id}预测完成")
    else:
        print("\n⚠️ 使用简化预测（移动平均）")
        for lab_id in [1, 2, 3]:
            # 简单移动平均预测
            recent_avg = df[df['lab_id'] == lab_id]['usage_rate'].tail(7).mean()
            future_dates = pd.date_range(start=pd.Timestamp.today(), periods=7, freq='D')
            forecasts[lab_id] = [
                {
                    'ds': date.strftime('%Y-%m-%d'),
                    'yhat': round(recent_avg * np.random.uniform(0.9, 1.1), 4)
                }
                for date in future_dates
            ]
    
    # 4. 生成可视化报告
    report_path = generate_visualization_report(df, stats, forecasts)
    
    # 5. 生成决策建议
    suggestions = generate_suggestions(stats, forecasts)
    
    print(f"\n📄 分析报告已保存：{report_path}")
    print("✅ AI 数据分析模块调用完成\n")
    
    return {
        'statistics': stats,
        'forecasts': forecasts,
        'report_path': report_path,
        'suggestions': suggestions,
        'prophet_available': PROPHET_AVAILABLE
    }


def smart_data_analysis_with_data(reserves, faults):
    """
    AI 数据分析主函数（使用真实数据）
    接收 Java 后端传递的预约和故障数据
    """
    print("=" * 50)
    print("开始运行：AI 数据分析模块（真实数据）")
    
    import pandas as pd
    from datetime import datetime, timedelta
    
    # 1. 处理预约数据（兼容多种字段名）
    if len(reserves) > 0:
        reserve_df = pd.DataFrame(reserves)
        print(f"✅ 预约数据：{len(reserve_df)} 条记录")
        print(f"📋 字段名：{reserve_df.columns.tolist()}")
        
        # 字段名映射（兼容 Java 驼峰命名）
        column_mapping = {
            'labId': 'lab_id',
            'userId': 'user_id',
            'reserveDate': 'reserve_date',
            'timeSlotStart': 'time_slot_start',
            'timeSlotEnd': 'time_slot_end',
            'startTime': 'reserve_date',
            'endTime': 'end_time'
        }
        reserve_df = reserve_df.rename(columns=column_mapping)
        
        # 确保有必要的字段
        if 'reserve_date' not in reserve_df.columns:
            # 尝试从 startTime 提取日期
            if 'startTime' in reserve_df.columns:
                reserve_df['reserve_date'] = pd.to_datetime(reserve_df['startTime']).dt.date
            else:
                reserve_df['reserve_date'] = pd.Timestamp.today().date()
        
        # 计算每天的使用率
        reserve_df['reserve_date'] = pd.to_datetime(reserve_df['reserve_date'])
        
        # 计算时长（如果有时间字段）
        if 'time_slot_start' in reserve_df.columns and 'time_slot_end' in reserve_df.columns:
            reserve_df['duration'] = reserve_df.apply(
                lambda row: calculate_duration_hours(row['time_slot_start'], row['time_slot_end']), 
                axis=1
            )
        else:
            # 默认每个预约 2 小时
            reserve_df['duration'] = 2.0
        
        # 按日期和实验室分组统计
        daily_stats = reserve_df.groupby(['reserve_date', 'lab_id']).agg({
            'duration': 'sum',
        }).reset_index()
        
        # 计算使用率（假设实验室每天开放 14 小时）
        daily_stats['usage_rate'] = daily_stats.apply(
            lambda row: min(row['duration'] / 14.0, 1.0), 
            axis=1
        )
        
        # 统计用户数
        if 'user_id' in reserve_df.columns:
            user_stats = reserve_df.groupby(['reserve_date', 'lab_id'])['user_id'].count().reset_index()
            user_stats.columns = ['reserve_date', 'lab_id', 'user_count']
            # 合并数据
            df = daily_stats.merge(user_stats, on=['reserve_date', 'lab_id'], how='left')
        else:
            df = daily_stats.copy()
            df['user_count'] = 0
        
        df = df.rename(columns={'reserve_date': 'ds'})
        df['ds'] = df['ds'].dt.strftime('%Y-%m-%d')
    else:
        # 没有预约数据，生成空的 DataFrame
        df = pd.DataFrame(columns=['ds', 'lab_id', 'usage_rate', 'user_count'])
        print("⚠️ 没有预约数据")
    
    # 2. 处理故障数据
    if len(faults) > 0:
        fault_df = pd.DataFrame(faults)
        print(f"✅ 故障数据：{len(fault_df)} 条记录")
        
        # 按实验室统计故障次数
        fault_stats = fault_df.groupby('lab_id').size().reset_index(name='fault_count')
    else:
        fault_stats = pd.DataFrame(columns=['lab_id', 'fault_count'])
        print("⚠️ 没有故障数据")
    
    # 3. 基础统计分析
    stats = {}
    for lab_id in [1, 2, 3, 4]:  # 支持 4 个实验室
        lab_data = df[df['lab_id'] == lab_id] if len(df) > 0 else pd.DataFrame()
        
        if len(lab_data) > 0:
            lab_fault_count = fault_stats[fault_stats['lab_id'] == lab_id]['fault_count'].values
            fault_count = int(lab_fault_count[0]) if len(lab_fault_count) > 0 else 0
            
            stats[lab_id] = {
                'avg_usage_rate': round(lab_data['usage_rate'].mean(), 4),
                'max_usage_rate': round(lab_data['usage_rate'].max(), 4),
                'min_usage_rate': round(lab_data['usage_rate'].min(), 4),
                'total_users': int(lab_data['user_count'].sum()),
                'total_faults': fault_count,
                'anomalies': detect_anomalies(df, lab_id)
            }
        else:
            # 没有数据时使用默认值
            stats[lab_id] = {
                'avg_usage_rate': 0.0,
                'max_usage_rate': 0.0,
                'min_usage_rate': 0.0,
                'total_users': 0,
                'total_faults': 0,
                'anomalies': {'anomaly_count': 0, 'anomaly_dates': []}
            }
    
    print("\n📊 实验室运行统计:")
    for lab_id, stat in stats.items():
        print(f"  实验室{lab_id}:")
        print(f"    - 平均使用率：{stat['avg_usage_rate']:.2%}")
        print(f"    - 总用户数：{stat['total_users']}")
        print(f"    - 故障次数：{stat['total_faults']}")
        print(f"    - 异常天数：{stat['anomalies']['anomaly_count']}")
    
    # 4. Prophet 预测
    forecasts = {}
    if PROPHET_AVAILABLE and len(df) > 0:
        print("\n🔮 开始预测未来 7 天使用率...")
        for lab_id in [1, 2, 3, 4]:
            lab_data = df[df['lab_id'] == lab_id]
            if len(lab_data) > 0:
                forecast, model = prophet_forecast(df, lab_id, periods=7)
                if forecast is not None:
                    forecasts[lab_id] = forecast.to_dict('records')
                    print(f"  实验室{lab_id}预测完成")
    else:
        print("\n⚠️ 使用简化预测（移动平均）")
        for lab_id in [1, 2, 3, 4]:
            lab_data = df[df['lab_id'] == lab_id]
            if len(lab_data) > 0:
                recent_avg = lab_data['usage_rate'].tail(7).mean()
            else:
                recent_avg = 0.5
            future_dates = pd.date_range(start=pd.Timestamp.today(), periods=7, freq='D')
            forecasts[lab_id] = [
                {
                    'ds': date.strftime('%Y-%m-%d'),
                    'yhat': round(recent_avg * np.random.uniform(0.9, 1.1), 4)
                }
                for date in future_dates
            ]
    
    # 5. 生成可视化报告
    report_path = generate_visualization_report(df, stats, forecasts)
    
    # 6. 生成决策建议
    suggestions = generate_suggestions(stats, forecasts)
    
    print(f"\n📄 分析报告已保存：{report_path}")
    print("✅ AI 数据分析模块调用完成\n")
    
    return {
        'statistics': stats,
        'forecasts': forecasts,
        'report_path': report_path,
        'suggestions': suggestions,
        'prophet_available': PROPHET_AVAILABLE
    }


def calculate_duration_hours(time_start, time_end):
    """计算时间段的时长（小时）"""
    try:
        # 处理字符串格式的时间
        if isinstance(time_start, str):
            time_start = datetime.strptime(time_start, '%H:%M:%S').time()
        if isinstance(time_end, str):
            time_end = datetime.strptime(time_end, '%H:%M:%S').time()
        
        # 计算时长
        start_minutes = time_start.hour * 60 + time_start.minute
        end_minutes = time_end.hour * 60 + time_end.minute
        
        duration_minutes = end_minutes - start_minutes
        if duration_minutes < 0:
            duration_minutes += 24 * 60  # 跨天
        
        return duration_minutes / 60.0
    except Exception as e:
        print(f"⚠️ 计算时长失败：{e}")
        return 2.0  # 默认 2 小时


def generate_visualization_report(df, stats, forecasts):
    """生成可视化报告"""
    # 创建图表
    line = Line()
    line.add_xaxis(df['ds'].unique().tolist())
    
    for lab_id in [1, 2, 3]:
        lab_data = df[df['lab_id'] == lab_id]
        line.add_yaxis(
            f"实验室{lab_id}",
            lab_data['usage_rate'].tolist(),
            is_smooth=True
        )
    
    line.set_global_opts(
        title_opts=opts.TitleOpts(title="实验室使用率趋势"),
        xaxis_opts=opts.AxisOpts(name="日期"),
        yaxis_opts=opts.AxisOpts(name="使用率")
    )
    
    # 生成决策建议
    suggestions = generate_suggestions(stats, forecasts)
    
    # 保存报告
    report_path = os.path.join(os.path.dirname(__file__), 'data', 'analysis_report_prophet.html')
    os.makedirs(os.path.dirname(report_path), exist_ok=True)
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("<html><head><meta charset='utf-8'><title>AI 数据分析报告</title></head><body>")
        f.write("<h1>实验室 AI 数据分析报告</h1>")
        
        # 添加统计摘要
        f.write("<h2>📊 实验室运行统计</h2>")
        f.write("<table border='1' style='border-collapse: collapse; width: 100%;'>")
        f.write("<tr><th>实验室</th><th>平均使用率</th><th>总用户数</th><th>故障次数</th><th>异常天数</th></tr>")
        for lab_id, stat in stats.items():
            f.write(f"<tr>")
            f.write(f"<td>实验室{lab_id}</td>")
            f.write(f"<td>{stat['avg_usage_rate']:.2%}</td>")
            f.write(f"<td>{stat['total_users']}</td>")
            f.write(f"<td>{stat['total_faults']}</td>")
            f.write(f"<td>{stat['anomalies']['anomaly_count']}</td>")
            f.write(f"</tr>")
        f.write("</table>")
        
        f.write("<h2>历史使用率趋势</h2>")
        f.write(line.render_embed())
        
        # 添加预测结果
        f.write("<h2>🔮 未来 7 天使用率预测</h2>")
        f.write("<table border='1' style='border-collapse: collapse; width: 100%;'>")
        f.write("<tr><th>日期</th>")
        for lab_id in [1, 2, 3]:
            f.write(f"<th>实验室{lab_id}预测值</th>")
        f.write("</tr>")
        
        # 获取所有日期（从 forecasts 中获取）
        if forecasts and len(forecasts[1]) > 0:
            dates = [record['ds'] for record in forecasts[1]]
            for i, date in enumerate(dates):
                # 格式化日期：如果是 datetime 对象则转为字符串
                if hasattr(date, 'strftime'):
                    date_str = date.strftime('%Y-%m-%d')
                else:
                    date_str = str(date).split()[0]  # 去掉时间部分
                
                f.write(f"<tr><td>{date_str}</td>")
                for lab_id in [1, 2, 3]:
                    if i < len(forecasts[lab_id]):
                        val = forecasts[lab_id][i].get('yhat', 'N/A')
                        f.write(f"<td>{val:.2%}</td>" if isinstance(val, (float, int)) else f"<td>{val}</td>")
                f.write("</tr>")
        
        f.write("</table>")
        
        # 添加决策建议
        f.write("<h2>💡 决策建议</h2>")
        f.write("<ul style='list-style-type: disc; padding-left: 20px;'>")
        for suggestion in suggestions:
            f.write(f"<li style='margin: 10px 0;'>{suggestion}</li>")
        f.write("</ul>")
        
        f.write("</body></html>")
    
    return report_path

def generate_suggestions(stats, forecasts):
    """生成决策建议"""
    suggestions = []
    
    for lab_id, stat in stats.items():
        # 使用率过高建议
        if stat['avg_usage_rate'] > 0.8:
            suggestions.append(f"实验室{lab_id}使用率较高（{stat['avg_usage_rate']:.1%}），建议增加开放时段或分流用户")
        
        # 使用率过低建议
        if stat['avg_usage_rate'] < 0.3:
            suggestions.append(f"实验室{lab_id}使用率较低（{stat['avg_usage_rate']:.1%}），建议加强宣传或调整开放时间")
        
        # 故障率过高建议
        if stat['total_faults'] > 5:
            suggestions.append(f"实验室{lab_id}故障频发（{stat['total_faults']}次），建议安排设备检修")
        
        # 异常检测建议
        if stat['anomalies']['anomaly_count'] > 0:
            suggestions.append(f"实验室{lab_id}检测到{stat['anomalies']['anomaly_count']}天异常使用，建议核查原因")
    
    if not suggestions:
        suggestions.append("各实验室运行正常，无需特别调整")
    
    return suggestions

# 测试代码
if __name__ == "__main__":
    result = smart_data_analysis()
    print("\n分析结果:")
    print(json.dumps(result, indent=2, ensure_ascii=False))
