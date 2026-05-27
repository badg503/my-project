-- 删除未使用的 AI 模型配置参数
-- 这些参数在代码中从未被使用，只是摆设
-- 执行时间：2026-03-25

-- 删除 LSTM 模型参数（故障预测实际使用的是实时数据窗口和阈值）
DELETE FROM ai_model_config 
WHERE param_key IN ('lstm_seq_length', 'lstm_hidden_size');

-- 删除 Q-Learning 参数（智能排课功能未实现）
DELETE FROM ai_model_config 
WHERE param_key IN ('q_learning_rate', 'q_discount_factor', 'q_epsilon');

