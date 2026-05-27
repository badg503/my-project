<template>
  <div class="login-wrap">
    <div class="login-card">
      <h1 class="title">开放实验室管理系统</h1>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="账号" :prefix-icon="UserIcon" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="LockIcon" @keyup.enter="onLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" :loading="loading" @click="onLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <p class="tip">  <router-link to="/register" style="color:#409eff">用户注册</router-link></p>
      <p class="tip" style="margin-top: 8px">
        <router-link to="/forgot-password" style="color:#409eff; font-size: 13px">忘记密码？</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { User as UserIcon, Lock as LockIcon } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onLogin() {
  await formRef.value?.validate().catch(() => {})
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}
.login-card {
  width: 380px;
  padding: 40px;
  background: rgba(255,255,255,0.95);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.title {
  text-align: center;
  margin-bottom: 28px;
  font-size: 22px;
  color: #1a1a2e;
}
.tip {
  text-align: center;
  font-size: 12px;
  color: #888;
  margin-top: 12px;
}
</style>
