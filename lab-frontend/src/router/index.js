import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { guest: true } },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue'), meta: { guest: true } },
  { path: '/forgot-password', name: 'ForgotPassword', component: () => import('@/views/ForgotPassword.vue'), meta: { guest: true } },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      // 公共模块
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') },
      { path: 'ai-qa', name: 'AiQa', component: () => import('@/views/ai/AiQa.vue') },
      
      // 学生端功能
      { path: 'lab', name: 'Lab', component: () => import('@/views/lab/LabList.vue') },
      { path: 'reserve', name: 'StudentReserve', component: () => import('@/views/reserve/StudentReserve.vue') },
      { path: 'lab-reserve', name: 'LabReserve', component: () => import('@/views/reserve/ReserveList.vue') },
      { path: 'check', name: 'Check', component: () => import('@/views/check/CheckList.vue') },
      { path: 'repair', name: 'Repair', component: () => import('@/views/repair/RepairList.vue') },
      { path: 'experiment-record', name: 'ExperimentRecord', component: () => import('@/views/experiment/ExperimentRecord.vue') },
      
      // 教师端功能
      { path: 'reserve-manage', name: 'ReserveManage', component: () => import('@/views/reserve/ReserveManage.vue'), meta: { roles: ['SYS_ADMIN', 'LAB_ADMIN', 'TEACHER'] } },
      { path: 'teacher-student-manage', name: 'TeacherStudentManage', component: () => import('@/views/teacher/TeacherStudentManage.vue'), meta: { roles: ['SYS_ADMIN', 'TEACHER'] } },
      { path: 'task-manage', name: 'TaskManage', component: () => import('@/views/task/TaskManage.vue'), meta: { roles: ['SYS_ADMIN', 'TEACHER'] } },
      { path: 'check-manage', name: 'CheckManage', component: () => import('@/views/check/CheckAll.vue'), meta: { roles: ['SYS_ADMIN', 'LAB_ADMIN', 'TEACHER'] } },
      { path: 'attendance-manage', name: 'AttendanceManage', component: () => import('@/views/attendance/AttendanceManage.vue'), meta: { roles: ['SYS_ADMIN', 'TEACHER'] } },
      
      // 公共公告详情页
      { path: 'announcement-detail', name: 'AnnouncementDetail', component: () => import('@/views/announcement/AnnouncementDetail.vue') },
      
      // 实验室管理员功能
      { path: 'lab-manage', name: 'LabManage', component: () => import('@/views/lab/LabManage.vue'), meta: { roles: ['SYS_ADMIN', 'LAB_ADMIN'] } },
      { path: 'device', name: 'Device', component: () => import('@/views/device/DeviceList.vue'), meta: { roles: ['SYS_ADMIN', 'LAB_ADMIN'] } },
      { path: 'reserve-all', name: 'ReserveAll', component: () => import('@/views/reserve/ReserveAll.vue'), meta: { roles: ['SYS_ADMIN', 'LAB_ADMIN'] } },
      { path: 'check-all', name: 'CheckAll', component: () => import('@/views/check/CheckAll.vue'), meta: { roles: ['SYS_ADMIN', 'LAB_ADMIN'] } },
      { path: 'attendance-all', name: 'AttendanceAll', component: () => import('@/views/attendance/AttendanceAll.vue'), meta: { roles: ['SYS_ADMIN', 'LAB_ADMIN'] } },
      { path: 'announcement', name: 'Announcement', component: () => import('@/views/announcement/AnnouncementList.vue'), meta: { roles: ['SYS_ADMIN', 'LAB_ADMIN'] } },
      { path: 'statistics', name: 'Statistics', component: () => import('@/views/statistics/Statistics.vue'), meta: { roles: ['SYS_ADMIN', 'LAB_ADMIN'] } },
      { path: 'user', name: 'User', component: () => import('@/views/user/UserList.vue'), meta: { roles: ['SYS_ADMIN', 'LAB_ADMIN'] } },
      
      // 系统管理员功能
      { path: 'system-config', name: 'SystemConfig', component: () => import('@/views/system/SystemConfig.vue'), meta: { roles: ['SYS_ADMIN'] } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  // 1. 鉴权拦截
  if (to.meta.requiresAuth && !token) return next('/login')
  if (to.meta.guest && token) return next('/')
  
  // 2. 【修复点】安全地解析 user 对象
  let user = null;
  try {
    const userStr = localStorage.getItem('user');
    // 只有当字符串存在且不是空字符串时才解析
    if (userStr) {
      user = JSON.parse(userStr);
    }
  } catch (e) {
    // 如果解析失败（说明数据脏了），清除本地存储，强制重新登录
    console.error('User data corrupted, clearing storage.', e);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    return next('/login'); 
  }

  // 3. 角色权限拦截
  // 系统管理员可以访问所有模块
  if (user && user.role === 'SYS_ADMIN') {
    next();
    return;
  }
  
  // 如果路由需要特定角色，而用户没有
  if (to.meta.roles && user && !to.meta.roles.includes(user.role)) {
    // 这里建议跳转到一个“无权限”页面，或者直接回首页，避免死循环
    return next('/dashboard'); 
  }
  
  next();
});

export default router
