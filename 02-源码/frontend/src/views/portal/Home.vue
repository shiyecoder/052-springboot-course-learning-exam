<template>
  <div class="home">
    <section class="hero">
      <div class="hero-txt">
        <h1>你好，{{ store.user.realName }}</h1>
        <p>本学期共选修 {{ stat.courseCount }} 门课程，累计学习 {{ stat.minutes }} 分钟，已完成 {{ stat.done }} 门。</p>
        <div class="hero-acts">
          <el-button type="primary" @click="$router.push('/portal/enroll')">去选课</el-button>
          <el-button @click="$router.push('/portal/my-course')">继续学习</el-button>
        </div>
      </div>
      <div class="hero-kpi">
        <div><b>{{ stat.courseCount }}</b><span>在修课程</span></div>
        <div><b>{{ stat.avgProgress }}%</b><span>平均进度</span></div>
        <div><b>{{ stat.examCount }}</b><span>待考考试</span></div>
        <div><b :class="stat.riskCount ? 'dan' : ''">{{ stat.riskCount }}</b><span>风险提醒</span></div>
      </div>
    </section>

    <div class="row">
      <div class="card" style="flex: 1.6">
        <h3>
          我的课程
          <div class="grow" />
          <a class="more" @click="$router.push('/portal/my-course')">全部 →</a>
        </h3>
        <div class="course-grid" v-if="courses.length">
          <div v-for="c in courses" :key="c.enrollmentId" class="course-card" @click="go(c)">
            <div class="cover"><img :src="imgUrl(c.coverUrl)" alt="" /></div>
            <div class="body">
              <div class="name">{{ c.courseName }}</div>
              <div class="meta"><span>{{ c.teacherName }}</span><span>{{ c.credit }} 学分</span></div>
              <div class="bar"><i :style="{ width: Math.min(100, Number(c.progressRate || 0)) + '%' }" /></div>
              <div class="meta"><span>进度 {{ Number(c.progressRate || 0).toFixed(0) }}%</span><span>{{ c.studyMinutes || 0 }} 分钟</span></div>
            </div>
          </div>
        </div>
        <div v-else class="empty">还没有选课，先去 <a class="lnk" @click="$router.push('/portal/enroll')">选课中心</a> 看看吧</div>
      </div>

      <div style="flex: 1; display: flex; flex-direction: column; gap: 12px">
        <div class="card">
          <h3>待考考试 <a class="more" @click="$router.push('/portal/score')">成绩 →</a></h3>
          <div v-if="exams.length" class="list">
            <div v-for="e in exams" :key="e.id" class="item" @click="goExam(e)">
              <div>
                <div class="tname">{{ e.examName }}</div>
                <div class="tsub">{{ e.courseName }} · {{ fmtDateTime(e.startTime) }}</div>
              </div>
              <span class="tag" :class="e.status === '进行中' ? 'dan' : 'info'">{{ e.status }}</span>
            </div>
          </div>
          <div v-else class="empty">暂无待考考试</div>
        </div>

        <div class="card">
          <h3>通知公告</h3>
          <div v-if="notices.length" class="list">
            <div v-for="n in notices" :key="n.id" class="item">
              <div>
                <div class="tname">{{ n.title }}</div>
                <div class="tsub">{{ fmtDate(n.publishTime || n.createTime) }}</div>
              </div>
            </div>
          </div>
          <div v-else class="empty">暂无公告</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../http'
import { store } from '../../store'
import { fmtDate, fmtDateTime, imgUrl } from '../../utils/format'

const router = useRouter()
const courses = ref([])
const exams = ref([])
const notices = ref([])
const stat = reactive({ courseCount: 0, minutes: 0, done: 0, avgProgress: 0, examCount: 0, riskCount: 0 })

onMounted(async () => {
  // 用 allSettled：公告或考试接口异常时，课程卡片仍要正常展示
  const [r1, r2, r3, r4] = await Promise.allSettled([
    http.get('/enrollment/my'),
    http.get('/exam/page', { params: { page: 1, size: 20 } }),
    http.get('/system/notice/list'),
    http.get('/risk/snapshot/page', { params: { page: 1, size: 1 } })
  ])
  const my = r1.status === 'fulfilled' ? r1.value : { total: 0 }
  const ex = r2.status === 'fulfilled' ? r2.value : { records: [] }
  const nt = r3.status === 'fulfilled' ? r3.value : []
  const risk = r4.status === 'fulfilled' ? r4.value : { total: 0 }
  const studying = my['学习中'] || []
  const done = my['已完成'] || []
  courses.value = [...studying, ...done].slice(0, 4)
  stat.courseCount = my.total || 0
  stat.done = done.length
  stat.minutes = [...studying, ...done].reduce((s, x) => s + (x.studyMinutes || 0), 0)
  const rates = [...studying, ...done].map((x) => Number(x.progressRate || 0))
  stat.avgProgress = rates.length ? Math.round(rates.reduce((a, b) => a + b, 0) / rates.length) : 0
  // 只展示未结束的考试，已归档的不占版面
  exams.value = (ex.records || []).filter((e) => ['待开始', '进行中'].includes(e.status)).slice(0, 4)
  stat.examCount = exams.value.length
  notices.value = (nt || []).slice(0, 4)
  stat.riskCount = risk.total || 0
})

function go(c) {
  router.push(`/portal/course/${c.enrollmentId}`)
}
function goExam(e) {
  if (e.status === '待开始') return
  router.push(`/portal/exam/${e.id}`)
}
</script>

<style scoped>
.hero {
  background: linear-gradient(120deg, #1b4f72 0%, #2e86ab 100%);
  color: #fff; border-radius: 10px; padding: 22px 24px; display: flex; align-items: center; gap: 20px;
}
.hero h1 { font-size: 20px; margin: 0 0 6px; }
.hero p { margin: 0 0 14px; color: rgba(255, 255, 255, 0.85); font-size: 13px; }
.hero-acts { display: flex; gap: 8px; }
.hero-kpi { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-left: auto; text-align: center; }
.hero-kpi b { display: block; font-size: 22px; }
.hero-kpi b.dan { color: #ffd2a8; }
.hero-kpi span { font-size: 11.5px; color: rgba(255, 255, 255, 0.78); }
.row { display: flex; gap: 12px; margin-top: 12px; align-items: flex-start; }
.card h3 { display: flex; align-items: center; gap: 8px; }
.card h3 .grow { flex: 1; }
.more, .lnk { color: var(--primary-2); font-size: 12px; cursor: pointer; font-weight: 400; }
.course-grid { grid-template-columns: repeat(2, 1fr); }
.course-card { cursor: pointer; }
.list .item { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 8px 0; border-bottom: 1px dashed var(--line); }
.list .item:last-child { border-bottom: 0; }
.list .item:hover .tname { color: var(--primary-2); }
</style>
