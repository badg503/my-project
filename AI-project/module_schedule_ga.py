# -*- coding: utf-8 -*-
"""
智能预约调度模块 - 遗传算法版本
使用遗传算法优化实验室资源分配
"""
import numpy as np
import pandas as pd
import os
import json

class GeneticAlgorithmScheduler:
    """基于遗传算法的实验室调度器"""
    
    def __init__(self, n_labs=3, n_time_slots=6, population_size=50):
        self.n_labs = n_labs
        self.n_time_slots = n_time_slots
        self.population_size = population_size
        
        # 遗传算法参数
        self.crossover_rate = 0.8
        self.mutation_rate = 0.1
        self.generations = 100
        
        # 加载数据
        self.lab_data = None
        self.reserve_data = None
    
    def load_data(self):
        """加载实验室数据"""
        try:
            lab_info = pd.read_csv(os.path.join(os.path.dirname(__file__), 'data', 'lab_info.csv'))
            lab_reserve = pd.read_csv(os.path.join(os.path.dirname(__file__), 'data', 'lab_reserve.csv'))
            print("✅ 实验室数据加载成功")
            return lab_info, lab_reserve
        except:
            # 生成模拟数据
            lab_info = pd.DataFrame({
                "lab_id": [1, 2, 3],
                "capacity": [20, 30, 15],
                "status": [1, 1, 0]
            })
            lab_reserve = pd.DataFrame({
                "user_id": [101, 102, 103, 104],
                "time_slot": [1, 2, 1, 3],
                "lab_need": [1, 2, 1, 2]
            })
            print("⚠️ 使用模拟数据")
            return lab_info, lab_reserve
    
    def initialize_population(self):
        """初始化种群"""
        population = []
        for _ in range(self.population_size):
            # 每个个体是一个调度方案：[lab1_slots, lab2_slots, lab3_slots, ...]
            individual = np.random.randint(0, self.n_time_slots, self.n_labs)
            population.append(individual)
        return population
    
    def fitness(self, individual, lab_demands, lab_capacities):
        """适应度函数"""
        # 目标 1: 满足需求
        satisfaction = 0
        for i in range(self.n_labs):
            if lab_demands[i] > 0:
                satisfaction += min(individual[i], lab_demands[i]) / lab_demands[i]
        
        # 目标 2: 负载均衡（惩罚不均衡分配）
        load_variance = np.var(individual)
        
        # 目标 3: 不超过容量
        capacity_penalty = 0
        for i in range(self.n_labs):
            if individual[i] * 10 > lab_capacities[i]:  # 简化：假设每个时段 10 人
                capacity_penalty += (individual[i] * 10 - lab_capacities[i])
        
        # 综合适应度
        fitness_score = satisfaction * 100 - load_variance * 5 - capacity_penalty * 2
        return fitness_score
    
    def select(self, population, fitnesses):
        """锦标赛选择"""
        selected = []
        for _ in range(len(population)):
            # 随机选 2 个个体
            idx1, idx2 = np.random.choice(len(population), 2, replace=False)
            # 选择适应度高的
            if fitnesses[idx1] > fitnesses[idx2]:
                selected.append(population[idx1].copy())
            else:
                selected.append(population[idx2].copy())
        return selected
    
    def crossover(self, parent1, parent2):
        """单点交叉"""
        if np.random.random() < self.crossover_rate:
            point = np.random.randint(1, self.n_labs)
            child1 = np.concatenate([parent1[:point], parent2[point:]])
            child2 = np.concatenate([parent2[:point], parent1[point:]])
            return child1, child2
        return parent1.copy(), parent2.copy()
    
    def mutate(self, individual):
        """变异"""
        if np.random.random() < self.mutation_rate:
            gene_idx = np.random.randint(0, self.n_labs)
            individual[gene_idx] = np.random.randint(0, self.n_time_slots)
        return individual
    
    def train(self, lab_demands, lab_capacities):
        """训练遗传算法"""
        print(f"🚀 开始训练遗传算法，共 {self.generations} 代...")
        
        # 初始化种群
        population = self.initialize_population()
        best_fitness = -np.inf
        best_individual = None
        
        for generation in range(self.generations):
            # 计算适应度
            fitnesses = [
                self.fitness(ind, lab_demands, lab_capacities) 
                for ind in population
            ]
            
            # 记录最优个体
            max_fitness = max(fitnesses)
            if max_fitness > best_fitness:
                best_fitness = max_fitness
                best_individual = population[np.argmax(fitnesses)].copy()
            
            if (generation + 1) % 20 == 0:
                print(f"  第 {generation + 1}/{self.generations} 代，最优适应度：{best_fitness:.2f}")
            
            # 选择
            selected = self.select(population, fitnesses)
            
            # 交叉
            new_population = []
            for i in range(0, len(selected), 2):
                if i + 1 < len(selected):
                    child1, child2 = self.crossover(selected[i], selected[i + 1])
                    new_population.extend([child1, child2])
                else:
                    new_population.append(selected[i].copy())
            
            # 变异
            population = [self.mutate(ind) for ind in new_population]
        
        print(f"✅ 训练完成，最优适应度：{best_fitness:.2f}")
        return best_individual
    
    def predict(self, lab_demands, lab_capacities):
        """使用训练好的模型进行预测"""
        # 简单训练一次
        best_individual = self.train(lab_demands, lab_capacities)
        return best_individual


def smart_reserve_schedule():
    """
    智能预约调度主函数（使用模拟数据）
    供 AI 服务调用，不依赖外部数据
    """
    print("=" * 50)
    print("开始运行：智能预约调度模块（模拟数据）")
    
    # 生成模拟数据
    labs = [
        {"labId": 1, "name": "实验室 A", "capacity": 30},
        {"labId": 2, "name": "实验室 B", "capacity": 25},
        {"labId": 3, "name": "实验室 C", "capacity": 20}
    ]
    
    reserves = [
        {"userId": 101, "labId": 1, "timeSlot": 1},
        {"userId": 102, "labId": 1, "timeSlot": 2},
        {"userId": 103, "labId": 2, "timeSlot": 1},
        {"userId": 104, "labId": 2, "timeSlot": 3},
        {"userId": 105, "labId": 3, "timeSlot": 2}
    ]
    
    return smart_reserve_schedule_with_data(labs, reserves)


def smart_reserve_schedule_with_data(labs, reserves):
    """
    智能预约调度主函数（使用真实数据）
    接收 Java 后端传递的实验室和预约数据
    """
    print("=" * 50)
    print("开始运行：智能预约调度模块（真实数据）")
    
    # 将 Java 传递的数据转换为 DataFrame
    import pandas as pd
    
    # 实验室数据（兼容多种字段名）
    lab_df = pd.DataFrame(labs)
    print(f"✅ 实验室数据：{len(lab_df)} 个实验室")
    print(f"📋 字段名：{lab_df.columns.tolist()}")
    if len(lab_df) > 0:
        print(lab_df)
    
    # 字段名映射（兼容 Java 驼峰命名）
    # Java 传递的字段：labId, userId, lab_need
    # Python 期望的字段：lab_id, user_id
    if 'labId' in lab_df.columns:
        lab_df = lab_df.rename(columns={'labId': 'lab_id'})
    if 'id' in lab_df.columns and 'lab_id' not in lab_df.columns:
        lab_df = lab_df.rename(columns={'id': 'lab_id'})
    
    # 预约数据
    reserve_df = pd.DataFrame(reserves)
    print(f"✅ 预约数据：{len(reserve_df)} 条")
    print(f"📋 字段名：{reserve_df.columns.tolist()}")
    if len(reserve_df) > 0:
        print(reserve_df)
    
    # 字段名映射
    if 'labId' in reserve_df.columns:
        reserve_df = reserve_df.rename(columns={'labId': 'lab_id'})
    if 'userId' in reserve_df.columns:
        reserve_df = reserve_df.rename(columns={'userId': 'user_id'})
    
    # 统计各实验室的容量
    n_labs = len(lab_df)
    # 确保有 lab_id 列
    lab_id_col = 'lab_id' if 'lab_id' in lab_df.columns else 'id'
    lab_ids = lab_df[lab_id_col].tolist()
    lab_capacities = lab_df['capacity'].values.astype(float) if 'capacity' in lab_df.columns else np.ones(n_labs) * 30
    
    # 统计各实验室的需求（预约次数）
    if len(reserve_df) > 0:
        # 确保有 lab_id 列
        reserve_lab_id_col = 'lab_id' if 'lab_id' in reserve_df.columns else 'id'
        lab_demands = reserve_df.groupby(reserve_lab_id_col).size().reindex(lab_ids, fill_value=0).values.astype(float)
    else:
        lab_demands = np.zeros(n_labs)
    
    print(f"📊 实验室需求：{lab_demands}")
    print(f"📊 实验室容量：{lab_capacities}")
    
    # 创建调度器
    scheduler = GeneticAlgorithmScheduler(n_labs=n_labs, n_time_slots=6, population_size=50)
    
    # 训练并预测
    best_plan = scheduler.predict(lab_demands, lab_capacities)
    
    # 生成调度建议
    schedule_plan = []
    for i, lab_id in enumerate(lab_ids):
        # 获取实验室名称（兼容 lab_id 和 id）
        name_col = 'lab_id' if 'lab_id' in lab_df.columns else 'id'
        lab_name_row = lab_df[lab_df[name_col] == lab_id]
        lab_name = lab_name_row['name'].values[0] if 'name' in lab_df.columns and len(lab_name_row) > 0 else f"实验室{lab_id}"
        
        schedule_plan.append({
            "labId": int(lab_id),
            "labName": lab_name,
            "assignedSlots": int(best_plan[i]),
            "utilization": round(float(best_plan[i]) / 6 * 100, 2),
            "capacity": int(lab_capacities[i]),
            "demand": int(lab_demands[i])
        })
    
    print(f"🏆 最优实验室调度方案：{best_plan}")
    print(f"📋 详细调度计划：{schedule_plan}")
    print("✅ 智能预约调度模块调用完成\n")
    
    return {
        "schedulePlan": schedule_plan,
        "totalAssignments": int(np.sum(best_plan)),
        "avgUtilization": round(np.mean(best_plan) / 6 * 100, 2),
        "algorithm": "Genetic Algorithm"
    }



# 测试代码
if __name__ == "__main__":
    result = smart_reserve_schedule()
    print("\n调度结果:")
    print(json.dumps(result, indent=2, ensure_ascii=False))
