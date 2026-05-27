# -*- coding: utf-8 -*-
"""
LSTM 故障预测 - 在线学习版本
特点：边预测边学习，自动积累数据并更新模型
"""

import os
import json
import torch
import torch.nn as nn
import numpy as np
from datetime import datetime, timedelta
from torch.utils.data import Dataset, DataLoader
import pickle

class LSTMFaultPredictor(nn.Module):
    """LSTM 故障预测模型"""
    
    def __init__(self, input_size=3, hidden_size=64, num_layers=2, dropout=0.2):
        super(LSTMFaultPredictor, self).__init__()
        self.hidden_size = hidden_size
        self.num_layers = num_layers
        
        self.lstm = nn.LSTM(
            input_size=input_size,
            hidden_size=hidden_size,
            num_layers=num_layers,
            batch_first=True,
            dropout=dropout if num_layers > 1 else 0
        )
        
        self.fc = nn.Linear(hidden_size, 1)
        self.sigmoid = nn.Sigmoid()
        
    def forward(self, x):
        # x shape: (batch, seq_len, input_size)
        lstm_out, _ = self.lstm(x)
        # 取最后一个时间步的输出
        output = self.fc(lstm_out[:, -1, :])
        output = self.sigmoid(output)
        return output
    
    def predict(self, sequence):
        """预测单个序列"""
        self.eval()
        with torch.no_grad():
            # sequence shape: (seq_len, input_size)
            if len(sequence.shape) == 2:
                sequence = sequence.unsqueeze(0)  # (1, seq_len, input_size)
            
            output = self.forward(sequence.float())
            return output.item()


class OnlineFaultDataset(Dataset):
    """在线学习数据集"""
    
    def __init__(self, data_list, seq_length=30):
        """
        data_list: [
            {
                "history": [[temp, vibration, current], ...],  # seq_length 天
                "label": 0/1  # 是否故障
            },
            ...
        ]
        """
        self.data_list = data_list
        self.seq_length = seq_length
    
    def __len__(self):
        return len(self.data_list)
    
    def __getitem__(self, idx):
        item = self.data_list[idx]
        history = torch.FloatTensor(item["history"][-self.seq_length:])
        label = torch.FloatTensor([item["label"]])[0]
        return history, label


class OnlineLSTMFaultPredictor:
    """支持在线学习的 LSTM 故障预测器"""
    
    def __init__(self, model_path=None, seq_length=30):
        self.seq_length = seq_length
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        
        # 初始化或加载模型
        if model_path and os.path.exists(model_path):
            print(f" 加载已有模型：{model_path}")
            self.model = LSTMFaultPredictor()
            self.model.load_state_dict(torch.load(model_path, map_location=self.device))
        else:
            print("🆕 初始化新模型（随机权重）")
            self.model = LSTMFaultPredictor()
        
        self.model = self.model.to(self.device)
        self.model.eval()
        
        # 在线学习缓存
        self.learning_buffer = []
        self.buffer_size_threshold = 50  # 积累 50 条数据后训练
        self.learning_rate = 0.001
        self.num_epochs = 5
        
        # 数据持久化
        self.data_file = "fault_learning_data.json"
        self.load_data()
    
    def predict(self, device_id, history_data):
        """
        预测设备故障概率
        
        Args:
            device_id: 设备 ID
            history_data: 过去 N 天的传感器数据 [[temp, vibration, current], ...]
        
        Returns:
            fault_probability: 故障概率 (0-1)
        """
        if len(history_data) < self.seq_length:
            # 数据不足，返回默认值
            return 0.5
        
        # 转换为 tensor
        sequence = torch.FloatTensor(history_data[-self.seq_length:]).to(self.device)
        
        # 预测
        fault_prob = self.model.predict(sequence)
        
        print(f"🔮 设备 {device_id} 故障概率：{fault_prob:.4f}")
        
        return fault_prob
    
    def add_feedback(self, device_id, history_data, actual_fault):
        """
        添加用户反馈（用于在线学习）
        
        Args:
            device_id: 设备 ID
            history_data: 预测时的历史数据
            actual_fault: 实际是否故障 (0/1)
        """
        feedback = {
            "timestamp": datetime.now().isoformat(),
            "device_id": device_id,
            "history": history_data[-self.seq_length:].tolist() if hasattr(history_data, 'tolist') else history_data,
            "label": int(actual_fault)
        }
        
        self.learning_buffer.append(feedback)
        self.save_data(feedback)
        
        print(f"📝 记录反馈：设备 {device_id}, 实际故障={actual_fault}")
        print(f"   缓存大小：{len(self.learning_buffer)}/{self.buffer_size_threshold}")
        
        # 检查是否需要训练
        if len(self.learning_buffer) >= self.buffer_size_threshold:
            print("🎯 达到训练阈值，开始在线学习...")
            self.train_online()
    
    def train_online(self):
        """在线学习：用缓存的数据微调模型"""
        # 加载所有历史数据
        all_data = self.load_all_data()
        
        if len(all_data) < self.buffer_size_threshold:
            print(f"⚠️ 数据量不足：{len(all_data)} < {self.buffer_size_threshold}")
            return
        
        # 创建数据集
        dataset = OnlineFaultDataset(all_data, self.seq_length)
        dataloader = DataLoader(dataset, batch_size=8, shuffle=True)
        
        # 优化器
        optimizer = torch.optim.Adam(self.model.parameters(), lr=self.learning_rate)
        criterion = nn.BCELoss()
        
        # 训练
        self.model.train()
        total_loss = 0
        
        print(f"📚 开始训练，数据量：{len(all_data)}, Epochs: {self.num_epochs}")
        
        for epoch in range(self.num_epochs):
            epoch_loss = 0
            for batch_x, batch_y in dataloader:
                batch_x = batch_x.to(self.device)
                batch_y = batch_y.to(self.device)
                
                # 前向传播
                outputs = self.model(batch_x).squeeze()
                
                # 计算损失
                loss = criterion(outputs, batch_y)
                
                # 反向传播
                optimizer.zero_grad()
                loss.backward()
                optimizer.step()
                
                epoch_loss += loss.item()
            
            avg_loss = epoch_loss / len(dataloader)
            total_loss += avg_loss
            print(f"   Epoch {epoch+1}/{self.num_epochs}, 损失：{avg_loss:.4f}")
        
        # 保存模型
        self.save_model()
        
        # 清空缓存
        self.learning_buffer = []
        
        avg_loss = total_loss / self.num_epochs
        print(f"✅ 在线学习完成！平均损失：{avg_loss:.4f}")
    
    def save_model(self, model_path=None):
        """保存模型"""
        if model_path is None:
            model_path = "fault_model_online.pth"
        
        torch.save(self.model.state_dict(), model_path)
        print(f"💾 模型已保存：{model_path}")
    
    def save_data(self, feedback):
        """保存反馈数据"""
        # 追加到文件
        with open(self.data_file, 'a', encoding='utf-8') as f:
            json.dump(feedback, f, ensure_ascii=False)
            f.write('\n')
    
    def load_data(self):
        """加载数据文件"""
        if not os.path.exists(self.data_file):
            return []
        
        data = []
        with open(self.data_file, 'r', encoding='utf-8') as f:
            for line in f:
                data.append(json.loads(line.strip()))
        
        print(f"📂 加载历史数据：{len(data)} 条")
        return data
    
    def load_all_data(self):
        """加载所有数据（包括缓存）"""
        all_data = self.load_data()
        all_data.extend(self.learning_buffer)
        return all_data


# ==================== 使用示例 ====================
if __name__ == "__main__":
    # 初始化预测器
    predictor = OnlineLSTMFaultPredictor(model_path="fault_model_online.pth")
    
    # 模拟预测
    device_id = "离心机-A"
    history = np.random.rand(30, 3) * 10  # 30 天的随机数据
    
    fault_prob = predictor.predict(device_id, history)
    print(f"\n预测结果：{fault_prob:.4f}")
    
    # 模拟用户反馈
    actual_fault = 1  # 实际故障了
    predictor.add_feedback(device_id, history, actual_fault)
    
    print("\n✅ 在线学习系统就绪！")
