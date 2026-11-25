<template>
  <div>
    <div class="page-head">
      <h2>我的课程</h2>
      <span class="sub">按学习状态分组，点击卡片进入学习页</span>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane v-for="g in groups" :key="g" :label="`${g}（${(data[g] || []).length}）`" :name="g" />
    </el-tabs>

    <div class="course-grid" v-loading="loading">
      <div v-for="c in data[tab] || []" :key="c.enrollmentId" class="course-card">
        <div class="cover"><img :src="imgUrl(c.coverUrl)" alt="" /></div>
        <div class="body">
          <div class="name">{{ c.courseName }}</div>
          <div class="meta"><span>{{ c.teacherName }}</span><span>{{ c.credit }} 学分</span></div>
          <div class="bar"><i :style="{ width: Math.min(100, Number(c.progressRate || 0)) + '%' }" /></div>
          <div class="meta">
            <span>进度 {{ Number(c.progressRate || 0).toFixed(0) }}%</span>
            <span>学习 {{ c.studyMinutes || 0 }} 分钟</span>
          </div>
          <div class="meta"><span>最近学习：{{ fmtFromNow(c.lastStudyTime) }}</span></div>
          <div class="acts">
            <el-button v-if="tab !== '已退课'" size="small" type="primary" @click="goLearn(c)">继续学习</el-button>
            <el-button v-else size="small" @click="goEnroll">重新选课</el-button>
            <span class="tag" :class="tab === '已完成' ? 'ok' : tab === '已退课' ? 'dan' : 'info'">{{ c.status }}</span>
          </div>
        </div>
      </div>
      <div v-if="!loading && !(data[tab] || []).length" class="empty" style="grid-column: 1/-1">
        该分组暂无课程
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../http'
import { fmtFromNow, imgUrl } from '../../utils/format'

const router = useRouter()
const groups = ['学习中', '已完成', '已退课']
const data = ref({})
const tab = ref('学习中')
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    data.value = await http.get('/enrollment/my')
  } finally {
    loading.value = false
  }
})

const goLearn = (c) => router.push(`/portal/course/${c.enrollmentId}`)
const goEnroll = () => router.push('/portal/enroll')
</script>

<style scoped>
.course-card .acts { margin-top: auto; display: flex; align-items: center; gap: 8px; }
:deep(.el-tabs__header) { margin-bottom: 12px; }
</style>
