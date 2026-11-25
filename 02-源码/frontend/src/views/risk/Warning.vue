<template>
  <div>
    <div class="page-head">
      <h2>学习预警台</h2>
      <span class="sub">五项因子加权：观看完成率 30% / 提交及时率 20% / 测验得分率 25% / 登录频次 15% / 讨论参与度 10%</span>
      <div class="grow" />
      <el-select v-model="level" style="width: 150px" placeholder="风险等级" clearable @change="load(1)">
        <el-option v-for="x in ['低风险', '关注', '中风险', '高风险']" :key="x" :label="x" :value="x" />
      </el-select>
    </div>

    <div class="kpis" style="grid-template-columns: repeat(4, 1fr); margin-bottom: 12px">
      <div class="kpi"><div class="lb">低风险</div><div class="vl">{{ stat.byLevel?.['低风险'] || 0 }}<small> 人</small></div></div>
      <div class="kpi"><div class="lb">关注</div><div class="vl">{{ stat.byLevel?.['关注'] || 0 }}<small> 人</small></div></div>
      <div class="kpi"><div class="lb">中风险</div><div class="vl">{{ stat.byLevel?.['中风险'] || 0 }}<small> 人</small></div></div>
      <div class="kpi"><div class="lb">高风险</div><div class="vl" style="color: var(--dan)">{{ stat.byLevel?.['高风险'] || 0 }}<small> 人</small></div></div>
    </div>

    <ListPage ref="lp" title="" sub="" api="/risk/snapshot/page"
      :params="level ? { riskLevel: level } : {}"
      :columns="columns" :actions="[{ key: 'detail', label: '详情' }, { key: 'correct', label: '人工修正' }]"
      @action="onAction">
      <template #cell-studentName="{ row }">
        <div class="tname">{{ row.studentName }}</div>
        <div class="tsub">{{ row.deptName }}</div>
      </template>
      <template #cell-riskScore="{ row }">
        <b :style="{ color: row.riskScore >= 70 ? 'var(--ok)' : row.riskScore >= 55 ? '#9A6412' : 'var(--dan)' }">{{ row.riskScore }}</b>
      </template>
      <template #cell-riskLevel="{ row }"><span class="tag" :class="tagType(row.riskLevel)">{{ row.riskLevel }}</span></template>
      <template #cell-factors="{ row }">
        <div style="display: flex; gap: 3px" :title="factorText(row)">
          <i v-for="(v, i) in factorsOf(row)" :key="i" :style="barStyle(v)" />
        </div>
      </template>
    </ListPage>

    <el-drawer v-model="drawer" title="预警详情" size="560px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="学生">{{ detail.studentName }}</el-descriptions-item>
          <el-descriptions-item label="课程">{{ detail.courseName }}</el-descriptions-item>
          <el-descriptions-item label="综合得分">{{ detail.snapshot.riskScore }}（{{ detail.snapshot.riskLevel }}）</el-descriptions-item>
          <el-descriptions-item label="规则版本">{{ detail.snapshot.ruleVersion }}</el-descriptions-item>
          <el-descriptions-item label="主要短板">{{ detail.snapshot.gapText }}</el-descriptions-item>
          <el-descriptions-item label="干预建议">{{ detail.snapshot.suggestion }}</el-descriptions-item>
          <el-descriptions-item label="缺失数据">{{ detail.snapshot.missingFlag || '—' }}</el-descriptions-item>
          <el-descriptions-item label="计算时间">{{ fmtDateTime(detail.snapshot.calcTime) }}</el-descriptions-item>
        </el-descriptions>
        <h3 style="font-size: 13px; margin: 16px 0 8px">五项因子</h3>
        <el-table :data="factorRows" size="small" border>
          <el-table-column prop="name" label="因子" min-width="150" />
          <el-table-column prop="score" label="得分" width="80" />
          <el-table-column prop="weight" label="权重" width="70" />
        </el-table>
        <h3 style="font-size: 13px; margin: 16px 0 8px">人工修正记录</h3>
        <el-table :data="detail.corrections" size="small" border empty-text="暂无修正">
          <el-table-column prop="originLevel" label="原等级" width="90" />
          <el-table-column prop="newLevel" label="新等级" width="90" />
          <el-table-column prop="reason" label="原因" min-width="180" />
          <el-table-column prop="correctTime" label="时间" width="150">
            <template #default="{ row }">{{ fmtDateTime(row.correctTime) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../http'
import ListPage from '../../components/ListPage.vue'
import { fmtDateTime, tagType } from '../../utils/format'

const lp = ref(null)
const level = ref('')
const stat = ref({})
const drawer = ref(false)
const detail = ref(null)

const columns = [
  { prop: 'studentName', label: '学生', width: 140 },
  { prop: 'courseName', label: '课程', minWidth: 160 },
  { prop: 'factors', label: '五项因子', width: 120 },
  { prop: 'riskScore', label: '综合得分', width: 90 },
  { prop: 'riskLevel', label: '等级', width: 100 },
  { prop: 'gapText', label: '主要短板', minWidth: 200 },
  { prop: 'adoptFlag', label: '推荐采纳', width: 100 },
  { prop: 'calcTime', label: '计算时间', type: 'datetime', width: 150 }
]

// 五项因子得分条：宽度按得分百分比，颜色跟随等级
const factorsOf = (row) => [row.scoreWatch, row.scoreSubmit, row.scoreQuiz, row.scoreLogin, row.scoreDiscuss].map((x) => Number(x || 0))
const barStyle = (v) => ({ width: '18px', height: '6px', borderRadius: '3px', background: v >= 70 ? '#2E86AB' : v >= 45 ? '#F39C12' : '#B02A37' })
const factorText = (row) => `观看 ${row.scoreWatch} / 提交 ${row.scoreSubmit} / 测验 ${row.scoreQuiz} / 登录 ${row.scoreLogin} / 讨论 ${row.scoreDiscuss}`

const factorRows = computed(() => {
  const s = detail.value?.snapshot
  if (!s) return []
  return [
    { name: '视频观看完成率', score: s.scoreWatch, weight: '30%' },
    { name: '作业与测验提交及时率', score: s.scoreSubmit, weight: '20%' },
    { name: '阶段测验得分率', score: s.scoreQuiz, weight: '25%' },
    { name: '近 14 天登录学习频次', score: s.scoreLogin, weight: '15%' },
    { name: '讨论区与答疑参与度', score: s.scoreDiscuss, weight: '10%' }
  ]
})

async function load() {
  stat.value = await http.get('/risk/stats')
}

async function onAction({ key, row }) {
  if (key === 'detail') {
    detail.value = await http.get(`/risk/snapshot/${row.id}`)
    drawer.value = true
    return
  }
  // 人工修正：原因不少于 5 字，原始结果保留
  const { value } = await ElMessageBox.prompt('修正原因（不少于 5 字）', `人工修正：${row.studentName} · ${row.riskLevel}`, {
    inputType: 'textarea', inputPlaceholder: '例如：已提交补学计划，经任课教师核实下调一级'
  })
  const newLevel = row.riskLevel === '高风险' ? '中风险' : row.riskLevel === '中风险' ? '关注' : '低风险'
  await http.post(`/risk/snapshot/${row.id}/correct`, { newLevel, reason: value })
  ElMessage.success(`已修正为 ${newLevel}，原始结果保留`)
  lp.value?.load()
  load()
}

onMounted(load)
</script>
