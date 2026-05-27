# -*- coding: utf-8 -*-
"""
智能问答模块 - BERT 语义理解版本
使用 BERT 模型进行语义相似度计算，从知识库中匹配最佳问题。
阈值由 Java 后端统一管理，通过 Flask 接口传入。
降级逻辑（关键词匹配）由 Java 后端 AiService.qa() 的数据库 like 查询兜底，
本模块仅负责 BERT 语义匹配。
"""
import numpy as np
import os
import json
import time
import torch
from transformers import BertTokenizer, BertModel
from sklearn.metrics.pairwise import cosine_similarity

# ============================================================
# 全局变量缓存
# ============================================================
_bert_model = None
_tokenizer = None
_knowledge_embeddings = None
_knowledge_base = None
_qa_threshold = 0.70
_model_loaded = False
_query_cache = {}


def load_bert_model():
    """加载 BERT 模型和知识库（仅首次调用时加载）"""
    global _bert_model, _tokenizer, _knowledge_embeddings, _knowledge_base, _model_loaded

    if _model_loaded:
        print("⚡ 跳过模型加载（已缓存）")
        return _bert_model, _tokenizer, _knowledge_embeddings, _knowledge_base

    print("=" * 60)
    print("🔍 正在加载 BERT 模型...")
    start_time = time.time()

    local_model_path = os.path.join(os.path.dirname(__file__), 'finetuned_model')
    if not os.path.exists(local_model_path):
        local_model_path = os.path.join(os.path.dirname(__file__), '..', 'finetuned_model')
    local_model_path = os.path.abspath(local_model_path)

    if not os.path.exists(local_model_path):
        print("⚠️ 本地模型目录不存在，BERT 不可用，降级由 Java 后端处理")
        _model_loaded = True
        return None, None, None, None

    print(f"📥 正在加载本地微调 BERT 模型：{local_model_path}")
    try:
        print("   🔧 加载 Tokenizer...")
        _tokenizer = BertTokenizer.from_pretrained(local_model_path, local_files_only=True)

        print("   🔧 加载 BERT 基础模型...")
        _bert_model = BertModel.from_pretrained('bert-base-chinese', local_files_only=False)

        print("   🔧 加载微调权重...")
        state_dict = torch.load(os.path.join(local_model_path, 'pytorch_model.bin'), map_location='cpu')
        _bert_model.load_state_dict(state_dict, strict=False)
        _bert_model.eval()
        print("✅ 本地微调 BERT 模型加载成功！")
    except Exception as e:
        import traceback
        print(f"⚠️ 本地模型加载失败：{e}")
        print(f"   错误详情：{traceback.format_exc()}")
        print(f"   降级由 Java 后端处理")
        _model_loaded = True
        return None, None, None, None

    # 加载知识库并预计算向量
    print("🔍 开始预计算知识库向量...")
    try:
        knowledge_file = os.path.join(os.path.dirname(__file__), 'data', 'ai_knowledge.json')
        if not os.path.exists(knowledge_file):
            print("⚠️ 知识库文件不存在，BERT 不可用，降级由 Java 后端处理")
            _model_loaded = True
            return _bert_model, _tokenizer, None, None

        with open(knowledge_file, 'r', encoding='utf-8') as f:
            _knowledge_base = json.load(f)
        print(f"✅ 知识库加载成功：{len(_knowledge_base)} 条记录")

        _knowledge_embeddings = compute_embeddings(_knowledge_base)
        if _knowledge_embeddings is None:
            print("⚠️ 知识库向量计算失败")
    except Exception as e:
        import traceback
        print(f"⚠️ 知识库向量计算失败：{e}")
        print(f"   错误详情：{traceback.format_exc()}")
        _knowledge_embeddings = None
        _knowledge_base = None

    _model_loaded = True
    elapsed = time.time() - start_time
    print(f"✅ BERT 模型加载总耗时：{elapsed:.2f}秒")
    print("=" * 60)

    return _bert_model, _tokenizer, _knowledge_embeddings, _knowledge_base


def compute_embeddings(knowledge_list):
    """批量计算知识库问题的向量表示"""
    global _bert_model, _tokenizer

    if _bert_model is None or not knowledge_list:
        return None

    batch_size = 32
    all_embeddings = []

    for i in range(0, len(knowledge_list), batch_size):
        batch_items = knowledge_list[i:i + batch_size]
        batch_questions = [item["question"] for item in batch_items]

        inputs = _tokenizer(
            batch_questions,
            return_tensors='pt',
            padding=True,
            truncation=True,
            max_length=128
        )

        with torch.no_grad():
            outputs = _bert_model(**inputs)
            cls_embeddings = outputs.last_hidden_state[:, 0, :].numpy()
            all_embeddings.extend(cls_embeddings)

    return np.array(all_embeddings)


def get_sentence_embedding(sentence):
    """获取单个句子的向量表示"""
    global _bert_model, _tokenizer

    if _bert_model is None:
        return None

    inputs = _tokenizer(
        sentence,
        return_tensors='pt',
        padding=True,
        truncation=True,
        max_length=64
    )

    with torch.no_grad():
        outputs = _bert_model(**inputs)
        cls_embedding = outputs.last_hidden_state[:, 0, :].numpy()

    return cls_embedding.flatten()


def smart_qa_assistant(user_question, threshold=None):
    """
    智能问答主函数 - 使用 BERT 语义匹配。
    若 BERT 不可用或匹配度低于阈值，返回空结果，由 Java 后端降级到数据库 like 匹配。

    参数:
        user_question: 用户输入的问题
        threshold: 相似度阈值（可选，由 Java 后端传入）
    """
    global _bert_model, _knowledge_embeddings, _knowledge_base

    print(f"[BERT QA] 问题：{user_question}")

    current_threshold = float(threshold) if threshold is not None else _qa_threshold

    # 检查缓存
    if user_question in _query_cache:
        print("[BERT QA] 命中缓存")
        return _query_cache[user_question]

    # BERT 或知识库不可用 → 返回空结果，Java 后端降级
    if _bert_model is None or _knowledge_embeddings is None or _knowledge_base is None:
        print("[BERT QA] BERT 不可用，返回空结果，由 Java 后端降级")
        result = {
            "question": user_question,
            "answer": "",
            "similarity": 0.0,
            "category": ""
        }
        return result

    # 计算语义相似度
    user_embedding = get_sentence_embedding(user_question)
    similarities = cosine_similarity([user_embedding], _knowledge_embeddings)[0]

    best_idx = np.argmax(similarities)
    best_similarity = similarities[best_idx]
    best_match = _knowledge_base[best_idx]

    print(f"[BERT QA] 最佳匹配：{best_match['question']}, 相似度：{best_similarity:.4f}, 阈值：{current_threshold}")

    # 低于阈值 → 返回空结果，Java 后端降级
    if best_similarity < current_threshold:
        result = {
            "question": user_question,
            "answer": "",
            "similarity": float(best_similarity),
            "category": ""
        }
        _query_cache[user_question] = result
        return result

    result = {
        "question": user_question,
        "answer": best_match["answer"],
        "similarity": float(best_similarity),
        "category": best_match.get("category", ""),
        "matched_question": best_match["question"]
    }
    _query_cache[user_question] = result
    return result
