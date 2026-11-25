<template>
  <div>
    <div class="page-head">
      <h2>选课中心</h2>
      <span class="sub">仅展示已发布课程；开课两周内可退课，退课后名额释放</span>
      <div class="grow" />
      <el-select v-model="query.courseType" placeholder="课程类型" clearable style="width: 150px" @change="load(1)">
        <el-option label="必修" value="必修" />
        <el-option label="选修" value="选修" />
        <el-option label="公选" value="公选" />
        <el-option label="实践" value="实践" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="课程名称 / 编号" clearable style="width: 220px" @keyup.enter="load(1)" />
      <el-button type="primary" :icon="Search" @click="load(1)">搜索</el-button>
    </div>

    <div class="course-grid" v-loading="loading">
      <div v-for="c in rows" :key="c.id" class="course-card">
        <div class="cover"><img :src="imgUrl(c.coverUrl)" alt="" /></div>
        <div class="body">
          <div class="name">{{ c.courseName }}</div>
          <div class="meta">
            <span>{{ c.teacherName }} {{ c.teacherTitle }}</span>
            <span>{{ c.credit }} 学分 · {{ c.totalHours }} 学时</span>
          </div>
          <div class="meta">
            <span class="tag" :class="c.courseType === '必修' ? 'info' : ''">{{ c.courseType }}</span>
            <span>已选 {{ c.enrollCount }} / {{ c.capacity }}</span>
          </div>
          <div class="acts">
            <template v-if="enrolledIds.includes(c.id)">
              <span class="done">已选课</span>
              <el-button size="small" text type="danger" @click="withdraw(c)">退课</el-button>
            </template>
            <el-button v-else size="small" type="primary" :disabled="c.remain <= 0" @click="enroll(c)">
              {{ c.remain > 0 ? '选课' : '名额已满' }}
            </el-button>
          </div>
        </div>
      </div>
      <div v-if="!loading && !rows.length" class="empty" style="grid-column: 1/-1">没有匹配的课程</div>
    </div>

    <div class="pager">
      <el-pagination v-model:current-page="page" :page-size="12" :total="total" layout="total, prev, pager, next" @current-change="load()" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import http from '../../http'
import { imgUrl } from '../../utils/format'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const enrolledIds = ref([])
const myMap = ref({})
const query = reactive({ keyword: '', courseType: '' })

async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const params = { page: page.value, size: 12 }
    if (query.keyword) params.keyword = query.keyword
    if (query.courseType) params.courseType = query.courseType
    const d = await http.get('/course/page', { params })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally {
    loading.value = false
  }
}

async function loadMine() {
  const my = await http.get('/enrollment/my')
  const all = [...(my['学习中'] || []), ...(my['已完成'] || [])]
  enrolledIds.value = all.map((x) => x.courseId)
  myMap.value = Object.fromEntries(all.map((x) => [x.courseId, x.enrollmentId]))
}

async function enroll(c) {
  await http.post('/enrollment', { courseId: c.id })
  ElMessage.success(`已选修《${c.courseName}》，可在我的课程中开始学习`)
  await Promise.all([load(), loadMine()])
}

async function withdraw(c) {
  await ElMessageBox.confirm(`确认退选《${c.courseName}》？退课后学习记录将保留但不再计入进度。`, '退课确认', { type: 'warning' })
  await http.post(`/enrollment/${myMap.value[c.id]}/withdraw`, { reason: '学生自主退课' })
  ElMessage.success('已退课，名额已释放')
  await Promise.all([load(), loadMine()])
}

onMounted(async () => {
  await loadMine()
  load(1)
})
</script>

<style scoped>
.course-card .body { gap: 8px; }
.course-card .acts { margin-top: auto; display: flex; align-items: center; gap: 8px; }
.course-card .done { color: var(--ok); font-size: 12px; }
.pager { margin-top: 8px; }
</style>
