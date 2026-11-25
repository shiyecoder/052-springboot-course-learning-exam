<template>
  <div>
    <div class="page-head">
      <h2>课程统计</h2>
      <span class="sub">章节完成率、成绩分布、通过率与讨论活跃度，支持按课程对比</span>
      <div class="grow" />
      <el-select v-model="courseId" style="width: 240px" placeholder="全部课程" clearable @change="load">
        <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
      </el-select>
    </div>

    <div class="kpis" style="grid-template-columns: repeat(4, 1fr); margin-bottom: 12px">
      <div class="kpi"><div class="lb">在修人数</div><div class="vl">{{ o.onCourse ? o.onCourse : 0 }}<small> 门课</small></div></div>
      <div class="kpi"><div class="lb">平均学习进度</div><div class="vl">{{ o.avgProgress || 0 }}<small>%</small></div></div>
      <div class="kpi"><div class="lb">考试通过率</div><div class="vl">{{ o.passRate || 0 }}<small>%</small></div></div>
      <div class="kpi"><div class="lb">章节完成率</div><div class="vl">{{ o.chapterRate || 0 }}<small>%</small></div></div>
    </div>

    <div class="charts c2">
      <div class="card">
        <h3>学习进度分布</h3>
        <Chart :option="distOpt" :height="220" />
      </div>
      <div class="card">
        <h3>考试成绩分段</h3>
        <Chart :option="segOpt" :height="220" />
      </div>
      <div class="card">
        <h3>近 30 天学习时长</h3>
        <Chart :option="trendOpt" :height="200" />
      </div>
      <div class="card">
        <h3>各课程风险等级堆叠</h3>
        <Chart :option="riskOpt" :height="200" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../../http'
import Chart from '../../components/Chart.vue'

const courses = ref([])
const courseId = ref(null)
const o = ref({})
const c = ref({})

const AX = { axisLine: { lineStyle: { color: '#E3E7EA' } }, axisLabel: { color: '#5B6770', fontSize: 10 } }
const G = { left: 8, right: 12, top: 18, bottom: 4, containLabel: true }
const Y = { type: 'value', ...AX, splitLine: { lineStyle: { color: '#F0F3F5' } } }

const distOpt = computed(() => ({ grid: G, tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: c.value.progressDist?.labels || [], ...AX }, yAxis: Y,
  series: [{ type: 'bar', data: c.value.progressDist?.values || [], barWidth: 26, itemStyle: { color: '#2E86AB', borderRadius: [3, 3, 0, 0] } }] }))
const segOpt = computed(() => ({ grid: G, tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: c.value.scoreSeg?.labels || [], ...AX }, yAxis: Y,
  series: [{ type: 'bar', data: c.value.scoreSeg?.values || [], barWidth: 26, itemStyle: { color: '#1B4F72', borderRadius: [3, 3, 0, 0] } }] }))
const trendOpt = computed(() => ({ grid: G, tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: (c.value.studyTrend?.labels || []).map((x) => x.slice(5)), ...AX, axisLabel: { ...AX.axisLabel, interval: 6 } }, yAxis: Y,
  series: [{ type: 'line', smooth: true, symbol: 'none', data: c.value.studyTrend?.values || [], lineStyle: { color: '#2E86AB', width: 2 }, areaStyle: { color: 'rgba(46,134,171,.16)' } }] }))
const riskOpt = computed(() => {
  const s = c.value.riskByDept || { labels: [], series: [] }
  const colors = { 低风险: '#8FA9B8', 关注: '#2E86AB', 中风险: '#F39C12', 高风险: '#B02A37' }
  return { grid: { ...G, bottom: 18 }, tooltip: { trigger: 'axis' },
    legend: { bottom: -2, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 10 } },
    xAxis: { type: 'category', data: s.labels, ...AX, axisLabel: { ...AX.axisLabel, interval: 0, rotate: 18 } }, yAxis: Y,
    series: (s.series || []).map((x) => ({ name: x.name, type: 'bar', stack: 'a', data: x.values, barWidth: 22, itemStyle: { color: colors[x.name] } })) }
})

async function load() {
  const [a, b] = await Promise.all([http.get('/dashboard/overview'), http.get('/dashboard/charts')])
  o.value = a
  c.value = b
}

onMounted(async () => {
  const d = await http.get('/course/page', { params: { page: 1, size: 100 } })
  courses.value = d.records || []
  load()
})
</script>
