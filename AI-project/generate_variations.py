#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
BERT 知识库问题自动变形脚本
为每个原始问题生成 30 个变型问题
"""

import json
import random
from datetime import datetime

# 同义词替换规则
SYNONYM_RULES = {
    '如何': ['怎么', '怎样', '如何能够', '怎么样', '咋'],
    '预约': ['预订', '预定', '申请使用', '登记'],
    '实验室': ['实验中心', '实验楼', '实验室场所'],
    '设备': ['仪器', '器材', '设施'],
    '报修': ['维修', '修理', '报故障', '申请维修'],
    '开放': ['开门', '营业', '可用'],
    '时间': ['时段', '时候', '光阴'],
    '忘记': ['忘掉', '遗失', '不记得'],
    '密码': ['口令', 'pass'],
    '取消': ['撤销', '废除', '解除'],
    '联系': ['联络', '找', '对接'],
    '管理员': ['老师', '负责人', '管理者'],
    '流程': ['步骤', '程序', '手续', '方法'],
    '提交': ['上传', '递交', '上报'],
    '实验报告': ['报告', '实验总结', '实训报告'],
    '签到': ['打卡', '登记', '记录考勤'],
    '迟到': ['晚到', '来晚', '超时'],
    '处理': ['处置', '解决', '应对'],
    '查看': ['查询', '浏览', '检阅', '看看'],
    '成绩': ['分数', '评分', '得分', '绩点'],
    '安全': ['平安', '防护'],
    '损坏': ['破损', '毁坏', '故障'],
    '赔偿': ['赔付', '补偿', '赔钱'],
    '自带': ['携带', '带', '自备'],
    'wifi': ['WiFi', '无线网络', '网络'],
    '打印': ['印刷', '输出'],
    '节假日': ['假期', '法定假日', '休息日'],
    '申请': ['请求', '报名', '填报'],
    '使用': ['运用', '利用', '采用'],
    '材料': ['物资', '用品', '原料'],
    '丢失': ['遗失', '不见', '没了'],
    '单独': ['一个人', '独自', '独立'],
    '延长': ['延期', '推迟', '拉长时间'],
    '反馈': ['反映', '投诉', '建议'],
    '查询': ['查找', '检索', '查看'],
    '参加': ['参与', '加入'],
    '故障': ['问题', '异常', '毛病'],
    '进度': ['进展', '状态', '情况'],
    '注册': ['开户', '创建', '开通'],
    '锁定': ['冻结', '封禁', '限制'],
    '修改': ['更改', '变更', '调整'],
    '找回': ['取回', '恢复', '重新获得'],
    '绑定': ['关联', '绑定手机', '绑定邮箱'],
    '个人信息': ['个人资料', '用户信息', '档案'],
    '注销': ['取消', '删除', '停用']
}

# 句式变换模板
SENTENCE_PATTERNS = [
    # 疑问词前置
    "{keyword} 怎么 {action}",
    "{keyword} 如何 {action}",
    "{keyword} 怎样 {action}",
    "{keyword} 的方法",
    "{keyword} 的流程",
    "{keyword} 的步骤",
    
    # 口语化表达
    "我想 {action}{keyword} 怎么做",
    "{action}{keyword} 要怎么弄",
    "{action}{keyword} 是什么流程",
    "咋 {action}{keyword}",
    
    # 场景化表达
    "第一次怎么 {action}{keyword}",
    "新生如何 {action}{keyword}",
    "{action}{keyword} 在哪个页面",
    "手机上怎么 {action}{keyword}",
    "电脑上如何 {action}{keyword}",
    "{action}{keyword} 入口在哪里",
    "{action}{keyword} 系统怎么用",
    "怎么在系统{action}{keyword}",
    "{action}{keyword} 的操作",
    "{action}{keyword} 怎么做",
    
    # 扩展表达
    "{action}{keyword} 需要什么条件",
    "{action}{keyword} 要提前多久",
    "{action}{keyword} 有什么要求",
    "学生怎么 {action}{keyword}",
    "{action}{keyword} 的途径",
    "{keyword} 可以通过什么方式{action}",
    "{action}{keyword} 的渠道",
    "如何申请{keyword}",
    "{keyword} 使用怎么{action}",
    "{action}{keyword} 的具体步骤"
]


def load_knowledge_base(file_path):
    """加载知识库"""
    with open(file_path, 'r', encoding='utf-8') as f:
        return json.load(f)


def save_knowledge_base(file_path, data):
    """保存知识库"""
    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def extract_keywords(question):
    """从问题中提取关键词"""
    keywords = []
    for keyword in SYNONYM_RULES.keys():
        if keyword in question:
            keywords.append(keyword)
    return keywords


def apply_synonym_replacement(question, variation_id):
    """应用同义词替换生成变型"""
    variations = []
    keywords = extract_keywords(question)
    
    if not keywords:
        return [question]
    
    # 选择不同的同义词替换组合
    for i, keyword in enumerate(keywords):
        synonyms = SYNONYM_RULES[keyword]
        synonym_index = variation_id % len(synonyms)
        new_question = question.replace(keyword, synonyms[synonym_index], 1)
        variations.append(new_question)
    
    return variations


def apply_sentence_pattern(question, pattern_id):
    """应用句式变换生成变型"""
    variations = []
    
    # 简单提取动作和对象
    words = question.split()
    if len(words) >= 2:
        action = words[-1] if len(words[-1]) > 1 else words[-2] if len(words) > 1 else words[0]
        keyword = ' '.join(words[:-1]) if len(words) > 1 else words[0]
    else:
        action = question
        keyword = question
    
    # 应用句式模板
    pattern = SENTENCE_PATTERNS[pattern_id % len(SENTENCE_PATTERNS)]
    try:
        new_question = pattern.format(keyword=keyword, action=action)
        variations.append(new_question)
    except KeyError:
        # 如果模板格式化失败，使用简单替换
        variations.append(f"{question} 的方法")
    
    return variations


def generate_simple_variations(question):
    """生成简单的变型问题（高质量）"""
    variations = []
    
    # 1. 同义词替换（只替换一次，避免累积）
    for keyword, synonyms in SYNONYM_RULES.items():
        if keyword in question:
            for synonym in synonyms[:3]:  # 每个关键词取前 3 个同义词
                var = question.replace(keyword, synonym)
                if var != question and var not in variations and len(var) < 40:
                    variations.append(var)
    
    # 2. 语序调整（仅针对"如何/怎么 + 动词 + 宾语"结构）
    for q_word in ['如何', '怎么', '怎样']:
        if q_word in question:
            parts = question.split(q_word, 1)  # 只分割一次
            if len(parts) == 2 and parts[0].strip() and parts[1].strip():
                # "如何预约实验室" -> "实验室如何预约"
                var = parts[1].strip() + q_word + parts[0].strip()
                if var != question and var not in variations and len(var) < 40:
                    variations.append(var)
    
    # 3. 添加语气词
    if not question.endswith('吗') and not question.endswith('?') and len(question) < 35:
        variations.append(question + '吗')
    
    # 4. 简化表达（"咋"）
    if '如何' in question:
        var = question.replace('如何', '咋')
        if var != question and var not in variations and len(var) < 40:
            variations.append(var)
    if '怎么' in question:
        var = question.replace('怎么', '咋')
        if var != question and var not in variations and len(var) < 40:
            variations.append(var)
    
    return variations


def generate_variations(original_question, num_variations=30):
    """为单个问题生成指定数量的变型"""
    variations = set()
    variations.add(original_question)
    
    # 使用简单变型策略（保证质量）
    simple_vars = generate_simple_variations(original_question)
    for var in simple_vars:
        variations.add(var)
    
    # 如果还不够，使用简单的句式模板（保证质量）
    if len(variations) < num_variations:
        question_clean = original_question.replace('吗', '').replace('？', '').strip()
        
        # 简单模板：只添加前缀/后缀
        simple_templates = [
            "请问{}",
            "{}？",
            "我想{}",
            "求助：{}",
            "{} 的方法",
            "{} 的流程",
            "{} 的步骤",
            "怎么{}",
            "如何{}",
            "{} 怎么办"
        ]
        
        for template in simple_templates:
            if len(variations) >= num_variations:
                break
            new_var = template.format(question_clean)
            if new_var != original_question and len(new_var) < 40:
                variations.add(new_var)
    
    # 补充策略：如果还不够，使用同义词多次替换
    while len(variations) < num_variations:
        base_var = random.choice(list(variations))
        keywords = extract_keywords(base_var)
        if keywords:
            keyword = random.choice(keywords)
            synonyms = SYNONYM_RULES.get(keyword, [keyword])
            new_synonym = random.choice(synonyms)
            new_var = base_var.replace(keyword, new_synonym, 1)
            if new_var != base_var and len(new_var) < 50:
                variations.add(new_var)
        else:
            break
    
    return list(variations)[:num_variations]


def generate_all_variations(knowledge_data, variations_per_question=30):
    """为所有问题生成变型"""
    all_variations = []
    
    print(f"📚 原始问题数量：{len(knowledge_data)}")
    print(f"🎯 每个问题变型数量：{variations_per_question}")
    print(f"📦 预计生成问题总数：{len(knowledge_data) * variations_per_question}")
    print("-" * 60)
    
    for i, item in enumerate(knowledge_data, 1):
        original_question = item['question']
        variations = generate_variations(original_question, variations_per_question)
        
        # 为每个变型创建完整的知识项
        for j, var_question in enumerate(variations, 1):
            new_item = {
                'question': var_question,
                'answer': item['answer'],
                'category': item['category']
            }
            all_variations.append(new_item)
        
        print(f"✅ [{i}/{len(knowledge_data)}] {original_question} → 生成 {len(variations)} 个变型")
    
    print("-" * 60)
    print(f"🎉 完成！共生成 {len(all_variations)} 个问题")
    
    return all_variations


def main():
    """主函数"""
    print("=" * 60)
    print("🚀 BERT 知识库问题自动变形脚本")
    print(f"⏰ 开始时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)
    
    # 文件路径
    input_file = r'd:\桌面\开放实验室管理系统的设计与实现\AI-project\data\ai_knowledge.json'
    output_file = r'd:\桌面\开放实验室管理系统的设计与实现\AI-project\data\ai_knowledge_variations.json'
    backup_file = r'd:\桌面\开放实验室管理系统的设计与实现\AI-project\data\ai_knowledge_backup.json'
    
    # 加载原始知识库
    print("\n📖 正在加载知识库...")
    knowledge_data = load_knowledge_base(input_file)
    
    # 备份原始文件
    print(f"💾 正在备份原始文件到：{backup_file}")
    save_knowledge_base(backup_file, knowledge_data)
    print("✅ 备份完成")
    
    # 生成变型
    print("\n🔄 正在生成变型问题...")
    all_variations = generate_all_variations(knowledge_data, variations_per_question=30)
    
    # 保存结果
    print(f"\n💾 正在保存变型结果到：{output_file}")
    save_knowledge_base(output_file, all_variations)
    print("✅ 保存完成")
    
    # 统计信息
    print("\n" + "=" * 60)
    print("📊 统计信息")
    print("=" * 60)
    print(f"原始问题数：{len(knowledge_data)}")
    print(f"变型后问题数：{len(all_variations)}")
    print(f"平均每个问题变型：{len(all_variations) / len(knowledge_data):.1f} 个")
    print(f"输出文件：{output_file}")
    print(f"备份文件：{backup_file}")
    print("=" * 60)
    
    print(f"\n⏰ 完成时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("🎉 所有操作完成！")
    
    # 询问是否替换原文件
    print("\n❓ 是否将变型后的文件替换原始文件？")
    print("提示：建议先检查 ai_knowledge_variations.json 的质量")
    print("确认无误后，手动替换或使用以下命令:")
    print(f"  copy \"{output_file}\" \"{input_file}\"")


if __name__ == '__main__':
    main()
