<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">开放实验室</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#1a1a2e"
        text-color="#a0aec0"
        active-text-color="#fff"
      >
        <!-- 公共模块 -->
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>首页概览</span>
        </el-menu-item>
        
        <el-menu-item index="/ai-qa">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 智能问答</span>
        </el-menu-item>

        <!-- 学生端功能 -->
        <el-sub-menu v-if="isStudent || isAdmin" index="student">
          <template #title>
            <el-icon><User /></el-icon>
            <span>学生功能</span>
          </template>
          <el-menu-item index="/lab">
            <el-icon><OfficeBuilding /></el-icon>
            <span>实验室查询</span>
          </el-menu-item>
          <el-menu-item index="/reserve">
            <el-icon><Calendar /></el-icon>
            <span>预约实验室</span>
          </el-menu-item>
          <el-menu-item index="/lab-reserve">
            <el-icon><List /></el-icon>
            <span>我的预约记录</span>
          </el-menu-item>
          <el-menu-item index="/check">
            <el-icon><Check /></el-icon>
            <span>考勤签到</span>
          </el-menu-item>
          <el-menu-item index="/repair">
            <el-icon><FirstAidKit /></el-icon>
            <span>设备报修</span>
          </el-menu-item>
          <el-menu-item index="/experiment-record">
            <el-icon><DocumentChecked /></el-icon>
            <span>实验记录</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 教师端功能 -->
        <el-sub-menu v-if="isTeacher || isAdmin" index="teacher">
          <template #title>
            <el-icon><Reading /></el-icon>
            <span>教师功能</span>
          </template>
          <el-menu-item index="/lab">
            <el-icon><OfficeBuilding /></el-icon>
            <span>实验室查询</span>
          </el-menu-item>
          <el-menu-item index="/reserve-manage">
            <el-icon><List /></el-icon>
            <span>预约审核</span>
          </el-menu-item>
          <el-menu-item index="/teacher-student-manage">
            <el-icon><UserFilled /></el-icon>
            <span>学员管理</span>
          </el-menu-item>
          <el-menu-item index="/task-manage">
            <el-icon><DocumentChecked /></el-icon>
            <span>实验管理</span>
          </el-menu-item>
          <el-menu-item index="/attendance-manage">
            <el-icon><DataAnalysis /></el-icon>
            <span>实验统计</span>
          </el-menu-item>
          <el-menu-item index="/repair">
            <el-icon><FirstAidKit /></el-icon>
            <span>设备报修</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 实验室管理员功能 -->
        <el-sub-menu v-if="isDeviceAdmin || isAdmin" index="lab-admin">
          <template #title>
            <el-icon><Management /></el-icon>
            <span>实验室管理功能</span>
          </template>
          <el-menu-item index="/lab-manage">
            <el-icon><School /></el-icon>
            <span>实验室管理</span>
          </el-menu-item>
          <el-menu-item index="/device">
            <el-icon><Cpu /></el-icon>
            <span>设备管理</span>
          </el-menu-item>
          <el-menu-item index="/reserve-all">
            <el-icon><Tickets /></el-icon>
            <span>预约管理</span>
          </el-menu-item>
          <el-menu-item index="/check-all">
            <el-icon><Checked /></el-icon>
            <span>考勤管理</span>
          </el-menu-item>
          <el-menu-item index="/announcement">
            <el-icon><Bell /></el-icon>
            <span>公告管理</span>
          </el-menu-item>
          <el-menu-item index="/statistics">
            <el-icon><TrendCharts /></el-icon>
            <span>数据统计</span>
          </el-menu-item>
          <el-menu-item index="/user">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 系统管理员功能（最高权限） -->
        <el-sub-menu v-if="isAdmin" index="sys-admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system-config">
            <el-icon><Tools /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="header-title">{{ pageTitle }}</span>
        <div class="user">
          <span>{{ user?.realName || user?.username }}</span>
          <el-tag :type="roleTagType" size="small" class="role-tag">{{ roleName }}</el-tag>
          <el-button link @click="openEditProfile">修改信息</el-button>
          <el-button link type="danger" @click="logout">退出</el-button>
        </div>

        <!-- 个人信息修改对话框 -->
        <el-dialog
          v-model="editProfileVisible"
          title="修改个人信息"
          width="500px"
        >
          <el-form :model="profileForm" label-width="100px">
            <el-form-item label="真实姓名">
              <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="手机号码">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号码" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="profileForm.gender">
                <el-radio label="男">男</el-radio>
                <el-radio label="女">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="班级">
              <el-select v-model="profileForm.classId" placeholder="请选择班级" style="width: 100%">
                <el-option v-for="cls in classes" :key="cls.id" :label="cls.className" :value="cls.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="学院">
              <el-select v-model="profileForm.department" placeholder="请选择学院" style="width: 100%">
                <el-option v-for="dept in departments" :key="dept.id" :label="dept.departmentName" :value="dept.departmentName" />
              </el-select>
            </el-form-item>
            <el-divider content-position="left">修改密码</el-divider>
            <el-form-item label="原密码">
              <el-input v-model="profileForm.oldPassword" type="password" placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="profileForm.newPassword" type="password" placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="profileForm.confirmPassword" type="password" placeholder="请确认新密码" />
            </el-form-item>
          </el-form>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="editProfileVisible = false">取消</el-button>
              <el-button type="primary" @click="updateUserProfile">保存修改</el-button>
            </span>
          </template>
        </el-dialog>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { updateProfile as updateProfileApi, changePassword as changePasswordApi } from '@/api/user'
import { getInfo } from '@/api/auth'
import { getClasses, getDepartments } from '@/api/user'
import { 
  DataAnalysis, OfficeBuilding, Calendar, Check, Setting, 
  Document, EditPen, ChatDotRound, List, Cpu, User, Bell,
  UserFilled, Reading, DocumentChecked, Histogram, Management,
  TrendCharts, Tools, FirstAidKit, School, Tickets, Checked, View
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const user = computed(() => userStore.user)

const isAdmin = computed(() => userStore.isAdmin())
const isDeviceAdmin = computed(() => userStore.isDeviceAdmin())
const isTeacher = computed(() => userStore.isTeacher())
const isStudent = computed(() => userStore.isStudent())

const roleName = computed(() => {
  const roleMap = {
    'SYS_ADMIN': '系统管理员',
    'LAB_ADMIN': '实验室管理员',
    'TEACHER': '教师',
    'STUDENT': '学生'
  }
  return roleMap[user.value?.role] || '未知角色'
})

const roleTagType = computed(() => {
  const typeMap = {
    'SYS_ADMIN': 'danger',
    'LAB_ADMIN': 'warning',
    'TEACHER': 'success',
    'STUDENT': 'info'
  }
  return typeMap[user.value?.role] || 'info'
})

const pageTitle = computed(() => {
  const map = {
    '/dashboard': '首页概览',
    '/lab': '实验室查询',
    '/reserve': '预约实验室',
    '/lab-reserve': '我的预约记录',
    '/check': '考勤签到',
    '/repair': '设备报修',
    '/experiment-record': '实验记录',
    '/ai-qa': 'AI 智能问答',
    '/reserve-manage': '预约审核',
    '/task-manage': '实验管理',
    '/check-manage': '考勤管理',
    '/attendance-manage': '考勤管理',
    '/device-view': '设备查看',
    '/user': '用户管理',
    '/lab-manage': '实验室管理',
    '/device': '设备管理',
    '/reserve-all': '预约管理',
    '/check-all': '考勤管理',
    '/announcement': '公告管理',
    '/statistics': '数据统计',
    '/log-manage': '日志管理',
    '/system-config': '系统设置'
  }
  return map[route.path] || ''
})

// 个人信息修改
const editProfileVisible = ref(false)
const profileForm = reactive({
  realName: '',
  phone: '',
  gender: '',
  email: '',
  major: '',
  department: '',
  classId: null,
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const classes = ref([])
const departments = ref([])

const openEditProfile = async () => {
  try {
    // 获取用户详细信息
    const res = await getInfo()
    const userInfo = res.data || res
    
    // 填充表单数据
    profileForm.realName = userInfo.realName || ''
    profileForm.phone = userInfo.phone || ''
    profileForm.gender = userInfo.gender || ''
    profileForm.email = userInfo.email || ''
    profileForm.major = userInfo.major || ''
    profileForm.department = userInfo.department || ''
    profileForm.classId = userInfo.classId || null
    
    // 加载学院和专业列表
    const [classesRes, departmentsRes] = await Promise.all([
      getClasses(),
      getDepartments()
    ])
    classes.value = classesRes.data || classesRes || []
    departments.value = departmentsRes.data || departmentsRes || []
    
    editProfileVisible.value = true
  } catch (error) {
    console.error('获取用户信息失败', error)
    ElMessage.error('获取用户信息失败')
  }
}

const updateUserProfile = async () => {
  try {
    // 只提交用户基本信息，不包含密码字段
    const submitData = {
      realName: profileForm.realName,
      phone: profileForm.phone,
      gender: profileForm.gender,
      email: profileForm.email,
      major: profileForm.major,
      department: profileForm.department
    }
    
    // 只有选择了班级才提交 classId
    if (profileForm.classId) {
      submitData.classId = profileForm.classId
    }
    
    const res = await updateProfileApi(submitData)
    // 拦截器已经处理了错误，能到这里说明请求成功
    
    // 如果需要修改密码
    if (profileForm.newPassword) {
      // 密码验证
      if (!profileForm.oldPassword) {
        ElMessage.warning('请输入原密码')
        return
      }
      if (profileForm.newPassword !== profileForm.confirmPassword) {
        ElMessage.warning('两次输入的密码不一致')
        return
      }
      
      // 调用修改密码接口
      await changePasswordApi({
        oldPassword: profileForm.oldPassword,
        newPassword: profileForm.newPassword
      })
      // 能执行到这里说明密码修改成功
    }
    
    ElMessage.success('个人信息修改成功' + (profileForm.newPassword ? '，密码已更新' : ''))
    // 更新用户信息
    userStore.updateUser({
      ...user.value,
      realName: profileForm.realName,
      phone: profileForm.phone,
      gender: profileForm.gender,
      email: profileForm.email,
      major: profileForm.major,
      department: profileForm.department
    })
    editProfileVisible.value = false
  } catch (error) {
    console.error('修改失败', error)
    ElMessage.error('修改失败，请稍后重试')
  }
}

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: #1a1a2e;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #2d2d44;
}

.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
}

.user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-tag {
  margin: 0 8px;
}

.main {
  background: #f5f7fa;
  padding: 20px;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:hover) {
  background-color: #252542 !important;
}

:deep(.el-menu-item.is-active) {
  background-color: #409eff !important;
}
</style>
