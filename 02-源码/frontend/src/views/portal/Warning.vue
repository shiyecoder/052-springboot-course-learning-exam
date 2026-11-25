<template>
  <div>
    <div class="page-head">
      <h2>学习预警</h2>
      <span class="sub">依据观看、作业、测验、登录、讨论五因子加权计算，规则版本会随快照留存</span>
    </div>

    <div class="kpis" style="grid-template-columns: repeat(4, 1fr); margin-bottom: 12px">
      <div class="kpi"><div class="lb">预警记录</div><div class="vl">{{ total }}</div></div>
      <div class="kpi"><div class="lb">高风险课程</div><div class="vl dan">{{ countOf('高风险') }}</div></div>
      <div class="kpi"><div class="lb">中风险课程</div><div class="vl">{{ countOf('中风险') }}</div></div>
      <div class="kpi"><div class="lb">平均综合得分</div><div class="vl">{{ avgScore }}</div></div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" stripe empty-text="暂无预警记录">
        <el-table-column prop="courseName" label="课程" min-width="200">
          <template #default="{ row }">
            <div class="tname">{{ row.courseName }}</div>
            <div class="tsub">{{ row.ruleVersion }} · {{ fmtDateTime(row.calcTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="综合得分" width="150">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 6px">
              <div class="progress" :class="barClass(row.riskLevel)" style="flex: 1">
                <i :style="{ width: Math.min(100, Number(row.riskScore || 0)) + '%' }" />
              </div>
              <b>{{ Number(row.riskScore || 0).toFixed(1) }}</b>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="等级" width="100">
          <template #default="{ row }"><span class="tag" :class="tagType(row.riskLevel)">{{ row.riskLevel }}</span></template>
        </el-table-column>
        <el-table-column prop="gapText" label="短板" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="tag" :class="row.adoptFlag ? 'ok' : ''">{{ row.adoptFlag ? '已采纳建议' : '待跟进' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="right">
          <template #default="{ row }"><a class="op" @click="open(row)">看因子</a></template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />
      </div>
    </div>

    <el-drawer v-model="drawer" title="预警详情" size="520px">
      <template v-if="cur">
        <div class="sum">
          <div><span>课程</span><b>{{ cur.courseName }}</b></div>
          <div><span>综合得分</span><b>{{ Number(cur.riskScore || 0).toFixed(1) }}</b></div>
          <div><span>等级</span><b>{{ cur.riskLevel }}</b></div>
          <div><span>规则版本</span><b>{{ cur.ruleVersion }}</b></div>
        </div>

        <h3 class="h">五项因子</h3>
        <div v-for="f in factors" :key="f.key" class="factor">
          <div class="f-head"><span>{{ f.label }}</span><b>{{ Number(cur[f.key] || 0).toFixed(1) }}</b></div>
          <div class="progress"><i :style="{ width: Math.min(100, Number(cur[f.key] || 0)) + '%' }" /></div>
        </div>

        <h3 class="h">短板说明</h3>
        <div class="box">{{ cur.gapText || '—' }}</div>
        <h3 class="h">改进建议</h3>
        <div class="box">{{ cur.suggestion || '—' }}</div>
        <div class="tsub" style="margin-top: 10px">
          {{ cur.corrected ? '该快照含教师人工修正记录' : '系统自动计算，未人工修正' }} · 计算时间 {{ fmtDateTime(cur.calcTime) }}
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../../http'
import { fmtDateTime, tagType } from '../../utils/format'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const drawer = ref(false)
const cur = ref(null)

const factors = [
  { key: 'scoreWatch', label: '观看完成度（权重 30）' },
  { key: 'scoreSubmit', label: '作业提交（权重 20）' },
  { key: 'scoreQuiz', label: '测验成绩（权重 25）' },
  { key: 'scoreLogin', label: '登录活跃（权重 15）' },
  { key: 'scoreDiscuss', label: '讨论参与（权重 10）' }
]

const avgScore = computed(() => {
  if (!rows.value.length) return 0
  return (rows.value.reduce((a, b) => a + Number(b.riskScore || 0), 0) / rows.value.length).toFixed(1)
})
const countOf = (lv) => rows.value.filter((r) => r.riskLevel === lv).length
const barClass = (lv) => (lv === '高风险' ? 'dan' : lv === '中风险' ? '' : 'ok')

async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const d = await http.get('/risk/snapshot/page', { params: { page: page.value, size: 10 } })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally {
    loading.value = false
  }
}

function open(row) {
  cur.value = row
  drawer.value = true
}

onMounted(() => load(1))
</script>

<style scoped>
.op { color: var(--primary-2); cursor: pointer; }
.dan { color: var(--dan); }
.sum { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; background: #f7f9fa; border-radius: 6px; padding: 12px; }
.sum span { display: block; color: var(--text-2); font-size: 11.5px; }
.sum b { font-size: 14px; color: var(--primary); }
.h { font-size: 13px; margin: 16px 0 8px; }
.factor { margin-bottom: 8px; }
.f-head { display: flex; justify-content: space-between; font-size: 12.5px; margin-bottom: 3px; }
.box { background: #f7f9fa; border-radius: 6px; padding: 10px; font-size: 12.5px; line-height: 1.75; color: var(--text); }
</style>
