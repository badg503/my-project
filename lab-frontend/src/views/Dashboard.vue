<template>
  <div class="dashboard">
    <h2 class="page-title">首页概览</h2>
    <el-row :gutter="20" v-if="stats && (stats.labCount !== undefined)">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-num">{{ stats.labCount }}</div>
          <div class="stat-label">实验室数量</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-num">{{ stats.deviceCount }}</div>
          <div class="stat-label">设备数量</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-num">{{ stats.reserveCount }}</div>
          <div class="stat-label">预约总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-num">{{ stats.repairCount }}</div>
          <div class="stat-label">报修记录</div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <template #header>系统公告</template>
          <el-empty v-if="!announcements.length" description="暂无公告" />
          <ul v-else class="announce-list">
            <li v-for="a in announcements" :key="a.id">
              <router-link :to="`/announcement-detail?id=${a.id}`" class="announce-title">{{ a.title }}</router-link>
              <span class="date">{{ formatDate(a.createTime) }}</span>
            </li>
          </ul>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>快捷入口</template>
          <!-- 学生端快捷入口 -->
          <div v-if="isStudent" class="quick-links">
            <el-button type="primary" @click="$router.push('/lab')">实验室查询</el-button>
            <el-button @click="$router.push('/lab-reserve')">我的预约</el-button>
            <el-button @click="$router.push('/check')">考勤签到</el-button>
            <el-button @click="$router.push('/ai-qa')">AI 问答</el-button>
          </div>
          <!-- 教师端快捷入口 -->
          <div v-else-if="isTeacher" class="quick-links">
            <el-button type="primary" @click="$router.push('/reserve-manage')">预约管理</el-button>
            <el-button @click="$router.push('/teacher-student-manage')">学员管理</el-button>
            <el-button @click="$router.push('/task-manage')">实验任务</el-button>
            <el-button @click="$router.push('/attendance-manage')">实验统计</el-button>
            <el-button @click="$router.push('/ai-qa')">AI 问答</el-button>
          </div>
          <!-- 实验室管理员快捷入口 -->
          <div v-else-if="isLabAdmin" class="quick-links">
            <el-button type="primary" @click="$router.push('/lab-manage')">实验室管理</el-button>
            <el-button @click="$router.push('/device')">设备管理</el-button>
            <el-button @click="$router.push('/reserve-all')">预约审核</el-button>
            <el-button @click="$router.push('/attendance-all')">考勤统计</el-button>
            <el-button @click="$router.push('/announcement')">公告管理</el-button>
          </div>
          <!-- 系统管理员快捷入口 -->
          <div v-else-if="isAdmin" class="quick-links">
            <el-button type="primary" @click="$router.push('/lab-manage')">实验室管理</el-button>
            <el-button @click="$router.push('/user')">用户管理</el-button>
            <el-button @click="$router.push('/device')">设备管理</el-button>
            <el-button @click="$router.push('/reserve-all')">预约审核</el-button>
            <el-button @click="$router.push('/announcement')">公告管理</el-button>
            <el-button @click="$router.push('/system-config')">系统配置</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getDashboard } from '@/api/statistics'
import { getAnnouncementList } from '@/api/announcement'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin())
const isTeacher = computed(() => userStore.user?.role === 'TEACHER')
const isLabAdmin = computed(() => userStore.user?.role === 'LAB_ADMIN')
const isStudent = computed(() => userStore.user?.role === 'STUDENT')
const stats = ref(null)
const announcements = ref([])

function formatDate(v) {
  if (!v) return ''
  return v.replace('T', ' ').slice(0, 16)
}

onMounted(async () => {
  if (userStore.isAdmin() || userStore.isDeviceAdmin()) {
    try { stats.value = await getDashboard() } catch (e) {}
  }
  try {
    announcements.value = await getAnnouncementList()
  } catch (e) {}
})
</script>

<style scoped>
.page-title { margin-bottom: 20px; font-size: 20px; }
.stat-card { text-align: center; }
.stat-num { font-size: 28px; font-weight: bold; color: #409eff; }
.stat-label { margin-top: 8px; color: #666; }
.announce-list { list-style: none; padding: 0; }
.announce-list li { padding: 8px 0; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; }
.announce-title { color: #409eff; text-decoration: none; cursor: pointer; }
.announce-title:hover { text-decoration: underline; }
.date { color: #999; font-size: 12px; }
.quick-links { display: flex; flex-wrap: wrap; gap: 12px; }
</style>
