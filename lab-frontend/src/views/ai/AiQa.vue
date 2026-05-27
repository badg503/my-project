<template>
  <div class="ai-qa">
    <h2 class="page-title">AI 智能问答助手</h2>
    <el-card>
      <p class="desc">解答实验原理、设备操作、预约规则等常见问题</p>
      <div class="chat-area">
        <div v-for="(item, i) in messages" :key="i" :class="['msg', item.role]">
          <span class="label">{{ item.role === 'user' ? '我' : 'AI' }}：</span>
          <span class="text">{{ item.text }}</span>
        </div>
      </div>
      <el-input
        v-model="question"
        type="textarea"
        :rows="2"
        placeholder="输入您的问题..."
        @keydown.enter.ctrl="send"
      />
      <div style="margin-top:8px">
        <el-button type="primary" @click="send" :loading="sending">发送</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { aiQa } from '@/api/ai'
import { ElMessage } from 'element-plus'

const question = ref('')
const messages = ref([])
const sending = ref(false)

async function send() {
  const q = question.value?.trim()
  if (!q) return
  messages.value.push({ role: 'user', text: q })
  question.value = ''
  sending.value = true
  try {
    const res = await aiQa(q)
    messages.value.push({ role: 'assistant', text: res.answer || '暂无回复' })
  } catch (e) {
    messages.value.push({ role: 'assistant', text: '请求失败，请稍后重试。' })
  } finally {
    sending.value = false
  }
}
</script>

<style scoped>
.page-title { margin-bottom: 16px; font-size: 18px; }
.desc { color: #666; margin-bottom: 16px; }
.chat-area {
  min-height: 200px;
  max-height: 400px;
  overflow-y: auto;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 12px;
}
.msg { margin-bottom: 10px; }
.msg.user { text-align: right; }
.msg .label { font-weight: bold; color: #409eff; margin-right: 6px; }
.msg.assistant .label { color: #67c23a; }
</style>
