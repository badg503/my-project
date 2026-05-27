<template>
  <div class="announcement-detail">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <el-button link @click="goBack" style="padding: 0">
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
          <span class="title">公告详情</span>
          <span></span>
        </div>
      </template>
      
      <div v-if="announcement" class="content-wrapper">
        <h2 class="announce-title">{{ announcement.title }}</h2>
        <div class="meta-info">
          <span class="time">发布时间：{{ formatDate(announcement.createTime) }}</span>
          <el-tag :type="announcement.status === 1 ? 'success' : 'info'" size="small">
            {{ announcement.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </div>
        <el-divider />
        <div class="content">
          <pre>{{ announcement.content }}</pre>
        </div>
      </div>
      
      <el-empty v-else description="公告不存在或已被删除" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAnnouncementById } from '@/api/announcement'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const announcement = ref(null)
const loading = ref(false)

function formatDate(v) {
  if (!v) return ''
  return v.replace('T', ' ').slice(0, 16)
}

function goBack() {
  router.back()
}

async function loadAnnouncement() {
  const id = route.query.id
  if (!id) {
    ElMessage.error('公告ID不能为空')
    return
  }
  
  loading.value = true
  try {
    const res = await getAnnouncementById(id)
    announcement.value = res
  } catch (e) {
    ElMessage.error('加载公告失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadAnnouncement)
</script>

<style scoped>
.announcement-detail {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 16px;
  font-weight: 500;
}

.content-wrapper {
  padding: 20px 0;
}

.announce-title {
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 16px;
  color: #303133;
}

.meta-info {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #909399;
  font-size: 14px;
}

.content {
  margin-top: 20px;
  font-size: 15px;
  line-height: 1.8;
  color: #606266;
}

.content pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
  margin: 0;
}
</style>
