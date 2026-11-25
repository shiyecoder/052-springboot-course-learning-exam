<template>
  <div v-loading="loading">
    <div class="learn-head">
      <a class="back" @click="$router.push('/portal/my-course')">← 返回我的课程</a>
      <h2>{{ course.courseName }}</h2>
      <span class="tsub">{{ course.teacherName }} · {{ course.credit }} 学分 · {{ course.totalHours }} 学时</span>
      <div class="grow" />
      <div class="prog">
        <span class="tsub">总进度 {{ progressRate }}%（{{ doneCount }}/{{ requiredCount }} 节）</span>
        <div class="progress"><i :style="{ width: Math.min(100, progressRate) + '%' }" /></div>
      </div>
    </div>

    <div class="learn">
      <!-- 左：章节 -->
      <div class="card side">
        <h3>课程目录</h3>
        <div class="tree">
          <template v-for="p in parents" :key="p.id">
            <div class="parent">{{ p.chapterName }}</div>
            <div
              v-for="c in childrenOf(p.id)"
              :key="c.id"
              :class="['leaf', active && active.id === c.id ? 'on' : '']"
              @click="pick(c)"
            >
              <span class="dot" :class="c.finishFlag ? 'ok' : ''" />
              <span class="nm">{{ c.chapterName }}</span>
              <span class="tsub">{{ c.nodeType }}</span>
            </div>
          </template>
          <div v-if="!parents.length" class="empty">该课程暂无章节</div>
        </div>
      </div>

      <!-- 中：播放 -->
      <div class="card main">
        <template v-if="active">
          <div class="ch-head">
            <h3 style="margin: 0">{{ active.chapterName }}</h3>
            <span class="tag" :class="active.isRequired ? 'info' : ''">{{ active.isRequired ? '必修' : '选修' }}</span>
            <span class="tag" :class="active.finishFlag ? 'ok' : 'warn'">
              {{ active.finishFlag ? '已完成' : '未完成' }}
            </span>
            <div class="grow" />
            <span class="tsub">观看 {{ Number(active.watchRate || 0).toFixed(0) }}% · 需 {{ active.minFinishRate || 90 }}% 达标</span>
          </div>

          <div class="player">
            <video
              v-if="active.nodeType === '视频' && active.resourceUrl"
              ref="video"
              :src="active.resourceUrl"
              controls
              @play="startHeart"
              @pause="stopHeart"
              @ended="markEnd"
            />
            <div v-else class="doc">
              <div class="doc-ico">📄</div>
              <div class="tname">{{ active.chapterName }}</div>
              <div class="tsub">本节为{{ active.nodeType }}资料，学习时长约 {{ active.durationMin }} 分钟</div>
              <el-button type="primary" style="margin-top: 12px" @click="readDoc">我已阅读本节资料</el-button>
            </div>
          </div>

          <div class="ch-foot">
            <span class="tsub">学习进度每 30 秒自动上报一次，异常加速会被标记复核</span>
            <div class="grow" />
            <el-button size="small" @click="step(-1)" :disabled="idx <= 0">上一节</el-button>
            <el-button size="small" type="primary" @click="step(1)" :disabled="idx >= flat.length - 1">下一节</el-button>
          </div>
        </template>
        <div v-else class="empty">请选择左侧章节开始学习</div>
      </div>

      <!-- 右：信息 -->
      <div class="card info">
        <h3>课程信息</h3>
        <img :src="imgUrl(course.coverUrl)" class="cover" alt="" />
        <div class="row"><span>课程编号</span><b>{{ course.courseNo }}</b></div>
        <div class="row"><span>课程类型</span><b>{{ course.courseType }}</b></div>
        <div class="row"><span>开课学期</span><b>{{ course.termName }}</b></div>
        <div class="row"><span>选课时间</span><b>{{ fmtDate(enrollment.enrollTime) }}</b></div>
        <div class="row"><span>累计学习</span><b>{{ enrollment.studyMinutes || 0 }} 分钟</b></div>
        <div class="tip-box">课程简介：{{ course.intro || '暂无简介' }}</div>
        <el-button style="width: 100%; margin-top: 10px" @click="$router.push('/portal/score')">查看我的成绩</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../../http'
import { fmtDate, imgUrl } from '../../utils/format'

const route = useRoute()
const enrollmentId = route.params.id
const loading = ref(false)
const course = ref({})
const enrollment = ref({})
const chapters = ref([])
const active = ref(null)
const progressRate = ref(0)
const doneCount = ref(0)
const requiredCount = ref(0)
const video = ref()
let timer = null

const parents = computed(() => chapters.value.filter((c) => !c.parentId || c.parentId === 0))
const flat = computed(() => chapters.value.filter((c) => c.parentId && c.parentId !== 0))
const childrenOf = (pid) => flat.value.filter((c) => c.parentId === pid)
const idx = computed(() => flat.value.findIndex((c) => active.value && c.id === active.value.id))

async function load(keepActive = true) {
  loading.value = true
  try {
    const d = await http.get(`/lesson/${enrollmentId}/chapters`)
    course.value = d.course || {}
    enrollment.value = d.enrollment || {}
    chapters.value = d.chapters || []
    requiredCount.value = d.requiredCount || 0
    doneCount.value = d.doneCount || 0
    progressRate.value = d.progressRate || 0
    if (keepActive && active.value) {
      active.value = chapters.value.find((c) => c.id === active.value.id) || active.value
    } else if (flat.value.length) {
      pick(flat.value.find((c) => !c.finishFlag) || flat.value[0])
    }
  } finally {
    loading.value = false
  }
}

function pick(c) {
  stopHeart()
  active.value = c
}

function step(d) {
  const n = flat.value[idx.value + d]
  if (n) pick(n)
}

// 每 30 秒上报一次心跳，后端按视频总长截断，异常加速会被标记
function startHeart() {
  stopHeart()
  timer = setInterval(() => report(30), 30000)
}
function stopHeart() {
  if (timer) clearInterval(timer)
  timer = null
}
async function report(sec) {
  if (!active.value) return
  const r = await http.post(`/lesson/${enrollmentId}/progress`, { chapterId: active.value.id, deltaSeconds: sec })
  if (r.abnormal) ElMessage.warning('检测到进度异常，已按视频总长校正并留痕')
  await load(true)
}
async function markEnd() {
  stopHeart()
  await report(Math.max(30, (active.value.durationMin || 1) * 60))
}
async function readDoc() {
  await report(Math.max(30, (active.value.durationMin || 1) * 60))
  ElMessage.success('本节已标记为完成')
}

onMounted(() => load(false))
onBeforeUnmount(stopHeart)
</script>

<style scoped>
.learn-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.learn-head h2 { font-size: 17px; margin: 0; }
.learn-head .grow { flex: 1; }
.back { color: var(--primary-2); cursor: pointer; font-size: 12.5px; }
.prog { width: 260px; }
.prog .progress { margin-top: 4px; }
.learn { display: flex; gap: 12px; align-items: flex-start; }
.side { width: 260px; flex: 0 0 260px; max-height: 620px; overflow-y: auto; }
.main { flex: 1; min-width: 0; }
.info { width: 260px; flex: 0 0 260px; }
.tree .parent { font-weight: 600; font-size: 13px; padding: 8px 4px 4px; color: var(--primary); }
.tree .leaf { display: flex; align-items: center; gap: 6px; padding: 6px 8px; border-radius: 5px; cursor: pointer; font-size: 12.5px; }
.tree .leaf:hover { background: #f5f8fa; }
.tree .leaf.on { background: #eaf2f8; color: var(--primary); font-weight: 500; }
.tree .leaf .nm { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree .dot { width: 7px; height: 7px; border-radius: 50%; background: #d6dee4; flex: 0 0 7px; }
.tree .dot.ok { background: var(--ok); }
.ch-head { display: flex; align-items: center; gap: 8px; }
.ch-head .grow { flex: 1; }
.player { margin-top: 10px; background: #0f2c40; border-radius: 8px; overflow: hidden; }
.player video { width: 100%; height: 380px; display: block; background: #000; }
.doc { height: 380px; display: grid; place-content: center; justify-items: center; color: #fff; gap: 6px; }
.doc .doc-ico { font-size: 34px; }
.doc .tsub { color: rgba(255, 255, 255, 0.7); }
.ch-foot { display: flex; align-items: center; gap: 8px; margin-top: 10px; }
.ch-foot .grow { flex: 1; }
.info .cover { width: 100%; height: 110px; object-fit: cover; border-radius: 6px; margin-bottom: 8px; background: #eef2f5; }
.info .row { display: flex; justify-content: space-between; font-size: 12.5px; padding: 4px 0; }
.info .row span { color: var(--text-2); }
.tip-box { margin-top: 8px; background: #f7f9fa; border-radius: 6px; padding: 8px 10px; font-size: 12px; color: var(--text-2); line-height: 1.7; }
</style>
