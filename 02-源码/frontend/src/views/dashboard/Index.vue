<template>
  <div>
    <div class="page-head">
      <h2>数据看板</h2>
      <span class="sub">最近 30 天口径 · 与列表总数可对账</span>
      <div class="grow" />
      <el-button :icon="Refresh" size="small" @click="load">刷新</el-button>
    </div>

    <!-- 8 个核心指标：与设计稿一致排成一行，1440×900 仍在一屏内 -->
    <div class="kpis k8" style="gap: 10px">
      <div class="kpi" v-for="k in kpiList" :key="k.label">
        <div class="lb">{{ k.label }}</div>
        <div class="vl">{{ k.value }}<small>{{ k.unit }}</small></div>
      </div>
    </div>

    <div class="charts c3" style="margin-top: 10px">
      <div class="card" v-for="ch in charts" :key="ch.title">
        <h3>{{ ch.title }}<span class="tip">{{ ch.tip }}</span></h3>
        <Chart :option="ch.option" :height="150" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import http from '../../http'
import Chart from '../../components/Chart.vue'

const o = ref({})
const c = ref({})

const kpiList = computed(() => [
  { label: '在修课程数', value: o.value.onCourse ?? 0, unit: ' 门' },
  { label: '本周新增选课', value: o.value.newEnroll ?? 0, unit: ' 人次' },
  { label: '平均学习进度', value: o.value.avgProgress ?? 0, unit: '%' },
  { label: '待批阅答卷', value: o.value.pendingGrade ?? 0, unit: ' 份' },
  { label: '考试通过率', value: o.value.passRate ?? 0, unit: '%' },
  { label: '高风险预警', value: o.value.highRisk ?? 0, unit: ' 人' },
  { label: '章节平均完成率', value: o.value.chapterRate ?? 0, unit: '%' },
  { label: '今日学习人数', value: o.value.activeToday ?? 0, unit: ' 人' }
])

const C1 = '#1B4F72', C2 = '#2E86AB', C3 = '#F39C12', C4 = '#8FA9B8'
const AX = { axisLine: { lineStyle: { color: '#E3E7EA' } }, axisLabel: { color: '#5B6770', fontSize: 10 } }
const G = { left: 8, right: 12, top: 16, bottom: 2, containLabel: true }
const Y = { type: 'value', ...AX, splitLine: { lineStyle: { color: '#F0F3F5' } } }

const charts = computed(() => {
  const d = c.value
  const riskColors = { 低风险: C4, 关注: C2, 中风险: C3, 高风险: '#B02A37' }
  return [
    {
      title: '各课程学习进度分布', tip: '在修学生',
      option: { grid: G, tooltip: { trigger: 'axis' }, xAxis: { type: 'category', data: d.progressDist?.labels || [], ...AX }, yAxis: Y,
        series: [{ type: 'bar', data: d.progressDist?.values || [], barWidth: 22, itemStyle: { color: C2, borderRadius: [3, 3, 0, 0] } }] }
    },
    {
      title: '近 30 天日学习时长', tip: '分钟',
      option: { grid: G, tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: (d.studyTrend?.labels || []).map((x) => x.slice(5)), ...AX, axisLabel: { ...AX.axisLabel, interval: 6 } },
        yAxis: Y,
        series: [{ type: 'line', smooth: true, symbol: 'none', data: d.studyTrend?.values || [], lineStyle: { color: C1, width: 2 }, areaStyle: { color: 'rgba(46,134,171,.14)' } }] }
    },
    {
      title: '按院系的风险等级', tip: '人次',
      option: { grid: { ...G, bottom: 16 }, tooltip: { trigger: 'axis' },
        legend: { bottom: -2, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 10, color: '#5B6770' } },
        xAxis: { type: 'category', data: d.riskByDept?.labels || [], ...AX, axisLabel: { ...AX.axisLabel, interval: 0, rotate: 18 } },
        yAxis: Y,
        series: (d.riskByDept?.series || []).map((s) => ({ name: s.name, type: 'bar', stack: 'a', data: s.values, barWidth: 20, itemStyle: { color: riskColors[s.name] } })) }
    },
    {
      title: '考试成绩分段', tip: '得分率',
      option: { grid: G, tooltip: { trigger: 'axis' }, xAxis: { type: 'category', data: d.scoreSeg?.labels || [], ...AX }, yAxis: Y,
        series: [{ type: 'bar', data: d.scoreSeg?.values || [], barWidth: 22, itemStyle: { color: C1, borderRadius: [3, 3, 0, 0] } }] }
    },
    {
      title: '章节观看完成率漏斗', tip: '节点数',
      option: { tooltip: { trigger: 'item' },
        legend: { bottom: 0, itemWidth: 8, itemHeight: 8, itemGap: 8, textStyle: { fontSize: 10, color: '#5B6770' } },
        series: [{ type: 'funnel', sort: 'none', left: 8, right: 8, top: 6, bottom: 26, minSize: '32%',
          itemStyle: { borderWidth: 0 },
          label: { position: 'inside', fontSize: 10, color: '#fff', formatter: '{c}' },
          data: (d.funnel || []).map((x, i) => ({ ...x, itemStyle: { color: [C1, C2, '#6FA3BE', C3][i] } })) }] }
    },
    {
      title: '防作弊事件分布', tip: '横轴参考人数 / 纵轴事件数',
      option: { grid: { ...G, bottom: 10 }, tooltip: { trigger: 'item' },
        xAxis: { type: 'value', name: '人数', nameTextStyle: { fontSize: 10, color: '#5B6770' }, ...AX, splitLine: { lineStyle: { color: '#F0F3F5' } } },
        yAxis: { type: 'value', name: '事件', nameTextStyle: { fontSize: 10, color: '#5B6770' }, ...AX, splitLine: { lineStyle: { color: '#F0F3F5' } } },
        series: [{ type: 'scatter', symbolSize: 13, itemStyle: { color: 'rgba(176,42,55,.62)' },
          data: (d.cheatScatter || []).map((x) => [x.x, x.y]) }] }
    }
  ]
})

async function load() {
  const [a, b] = await Promise.all([http.get('/dashboard/overview'), http.get('/dashboard/charts')])
  o.value = a
  c.value = b
}

onMounted(load)
</script>
<style scoped>
/* 8 个指标与设计稿一致排成一行；窄屏回落两行 */
.kpis.k8 { grid-template-columns: repeat(8, 1fr); }
.kpis.k8 .kpi { padding: 10px 12px; }
.kpis.k8 .kpi .vl { font-size: 19px; }
@media (max-width: 1400px) { .kpis.k8 { grid-template-columns: repeat(4, 1fr); } }
</style>
<style scoped>
/* 8 个指标与设计稿一致排成一行；窄屏回落两行 */
.kpis.k8 { grid-template-columns: repeat(8, 1fr); }
.kpis.k8 .kpi { padding: 10px 12px; }
.kpis.k8 .kpi .vl { font-size: 19px; }
@media (max-width: 1400px) { .kpis.k8 { grid-template-columns: repeat(4, 1fr); } }
</style>
