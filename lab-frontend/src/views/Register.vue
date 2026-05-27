<template>
  <div class="register-wrap">
    <div class="register-card">
      <h1 class="title">用户注册</h1>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" size="large">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio label="TEACHER">教师</el-radio>
            <el-radio label="STUDENT">学生</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" placeholder="选填" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="选填" /></el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" :loading="loading" @click="onRegister">注册</el-button>
        </el-form-item>
      </el-form>
      <p class="tip"><router-link to="/login">返回登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '', realName: '', role: 'STUDENT', gender: '', phone: '', email: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function onRegister() {
  await formRef.value?.validate().catch(() => {})
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}
.register-card {
  width: 420px;
  padding: 40px;
  background: rgba(255,255,255,0.95);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.title { text-align: center; margin-bottom: 24px; font-size: 22px; color: #1a1a2e; }
.tip { text-align: center; font-size: 14px; margin-top: 12px; }
.tip a { color: #409eff; }
</style>
