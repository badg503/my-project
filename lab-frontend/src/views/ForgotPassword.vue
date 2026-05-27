<template>
  <div class="forgot-password-container">
    <div class="card">
      <h2>找回密码</h2>
      <p class="subtitle">通过邮箱验证码重置密码</p>
      
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <!-- 步骤 1：输入邮箱 -->
        <div v-if="step === 1">
          <el-form-item label="邮箱" prop="email">
            <el-input 
              v-model="form.email" 
              placeholder="请输入注册邮箱"
              clearable
            >
              <template #prefix>
                <el-icon><Message /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item>
            <el-button 
              type="primary" 
              :loading="sending" 
              @click="sendCode"
              :disabled="countdown > 0"
              style="width: 100%"
            >
              {{ countdown > 0 ? `${countdown}秒后重试` : '获取验证码' }}
            </el-button>
          </el-form-item>
        </div>
        
        <!-- 步骤 2：输入验证码和新密码 -->
        <div v-if="step === 2">
          <el-form-item label="验证码" prop="code">
            <el-input 
              v-model="form.code" 
              placeholder="请输入 6 位验证码"
              maxlength="6"
              clearable
            >
              <template #prefix>
                <el-icon><Key /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item label="新密码" prop="newPassword">
            <el-input 
              v-model="form.newPassword" 
              type="password"
              placeholder="请输入新密码"
              show-password
              clearable
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input 
              v-model="form.confirmPassword" 
              type="password"
              placeholder="请再次输入新密码"
              show-password
              clearable
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item>
            <el-button 
              type="primary" 
              :loading="resetting" 
              @click="resetPassword"
              style="width: 100%"
            >
              重置密码
            </el-button>
          </el-form-item>
        </div>
        
        <!-- 返回登录 -->
        <el-form-item>
          <el-button @click="goToLogin" style="width: 100%">
            返回登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Message, Key, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const formRef = ref(null)
const step = ref(1)
const sending = ref(false)
const resetting = ref(false)
const countdown = ref(0)

const form = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为 6 位数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 发送验证码
const sendCode = async () => {
  if (!formRef.value) return
  
  try {
    // 先验证邮箱字段
    await formRef.value.validateField(['email'])
    
    sending.value = true
    const response = await axios.post('http://localhost:8080/api/auth/send-reset-code', null, {
      params: { email: form.email }
    })
    
    if (response.data.code === 200) {
      ElMessage.success('验证码已发送，请查收邮箱')
      step.value = 2
      startCountdown()
    } else {
      ElMessage.error(response.data.message || '发送失败')
    }
  } catch (error) {
    if (error.response && error.response.data) {
      ElMessage.error(error.response.data.message || '发送失败')
    } else if (error.message !== '验证失败') {
      ElMessage.error('发送失败，请检查网络连接')
    }
  } finally {
    sending.value = false
  }
}

// 重置密码
const resetPassword = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    resetting.value = true
    try {
      const params = new URLSearchParams()
      params.append('email', form.email)
      params.append('code', form.code)
      params.append('newPassword', form.newPassword)
      
      const response = await axios.post(
        'http://localhost:8080/api/auth/reset-password',
        params,
        {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          }
        }
      )
      
      if (response.data.code === 200) {
        ElMessage.success('密码重置成功，即将跳转到登录页')
        setTimeout(() => {
          router.push('/login')
        }, 1500)
      } else {
        ElMessage.error(response.data.message || '重置失败')
      }
    } catch (error) {
      ElMessage.error('重置失败，请检查网络连接')
    } finally {
      resetting.value = false
    }
  })
}

// 开始倒计时
const startCountdown = () => {
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

// 返回登录
const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.forgot-password-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.card {
  background: white;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 450px;
}

h2 {
  text-align: center;
  color: #333;
  margin-bottom: 10px;
}

.subtitle {
  text-align: center;
  color: #666;
  margin-bottom: 30px;
  font-size: 14px;
}

:deep(.el-input__wrapper) {
  border-radius: 5px;
}

:deep(.el-button) {
  border-radius: 5px;
}
</style>
