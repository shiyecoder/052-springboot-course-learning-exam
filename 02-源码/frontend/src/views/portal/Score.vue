<template>
  <div>
    <div class="page-head">
      <h2>我的成绩</h2>
      <span class="sub">成绩发布后可见；主客观分项与是否通过一目了然</span>
    </div>

    <div class="kpis" style="grid-template-columns: repeat(4, 1fr); margin-bottom: 12px">
      <div class="kpi"><div class="lb">已出分考试</div><div class="vl">{{ rows.length }}</div></div>
      <div class="kpi"><div class="lb">平均分</div><div class="vl">{{ avg }}</div></div>
      <div class="kpi"><div class="lb">最高分</div><div class="vl">{{ max }}</div></div>
      <div class="kpi"><div class="lb">通过率</div><div class="vl">{{ passRate }}<small>%</small></div></div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" stripe empty-text="暂无已发布的成绩">
        <el-table-column prop="examName" label="考试" min-width="200">
          <template #default="{ row }">
            <div class="tname">{{ row.examName }}</div>
            <div class="tsub">{{ row.courseName }} · {{ row.examType }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="objectiveScore" label="客观题" width="100" align="center" />
        <el-table-column prop="subjectiveScore" label="主观题" width="100" align="center" />
        <el-table-column label="总分" width="120" align="center">
          <template #default="{ row }">
            <b class="score" :class="row.passFlag ? 'ok' : 'dan'">{{ row.totalScore }}</b>
            <span class="tsub"> / {{ row.fullScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="100" align="center">
          <template #default="{ row }">
            <span class="tag" :class="row.passFlag ? 'ok' : 'dan'">{{ row.passFlag ? '通过' : '不及格' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="交卷时间" type="datetime" width="160" />
        <el-table-column label="操作" width="100" align="right">
          <template #default="{ row }"><a class="op" @click="openDetail(row)">查看答卷</a></template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="drawer" title="答卷详情" size="620px">
      <template v-if="detail">
        <div class="sum">
          <div><span>总分</span><b>{{ detail.record.totalScore }}</b></div>
          <div><span>客观题</span><b>{{ detail.record.objectiveScore }}</b></div>
          <div><span>主观题</span><b>{{ detail.record.subjectiveScore }}</b></div>
          <div><span>状态</span><b>{{ detail.record.status }}</b></div>
        </div>
        <div v-for="(d, i) in detail.details" :key="d.id" class="q">
          <div class="q-head">
            <span class="no">{{ i + 1 }}</span>
            <span class="tag info">{{ d.questionType }}</span>
            <span class="tsub">得分 {{ d.score }} / {{ d.fullScore }}</span>
            <span class="tag" :class="d.correctFlag ? 'ok' : 'dan'">{{ d.correctFlag ? '正确' : '错误' }}</span>
          </div>
          <div class="line"><span>我的答案</span><b>{{ d.answerContent || '未作答' }}</b></div>
          <div class="line" v-if="d.comment"><span>评语</span><b>{{ d.comment }}</b></div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../../http'

const rows = ref([])
const loading = ref(false)
const drawer = ref(false)
const detail = ref(null)

const avg = computed(() => {
  if (!rows.value.length) return 0
  const s = rows.value.reduce((a, b) => a + Number(b.totalScore || 0), 0)
  return (s / rows.value.length).toFixed(1)
})
const max = computed(() => (rows.value.length ? Math.max(...rows.value.map((r) => Number(r.totalScore || 0))) : 0))
const passRate = computed(() => {
  if (!rows.value.length) return 0
  return ((rows.value.filter((r) => r.passFlag).length / rows.value.length) * 100).toFixed(0)
})

async function load() {
  loading.value = true
  try {
    const d = await http.get('/score/page', { params: { page: 1, size: 50 } })
    rows.value = d.records || []
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  detail.value = await http.get(`/exam-record/${row.id}`)
  drawer.value = true
}

onMounted(load)
</script>

<style scoped>
.score { font-size: 15px; }
.score.ok { color: var(--ok); }
.score.dan { color: var(--dan); }
.op { color: var(--primary-2); cursor: pointer; }
.sum { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; background: #f7f9fa; border-radius: 6px; padding: 12px; margin-bottom: 12px; }
.sum div { text-align: center; }
.sum span { display: block; color: var(--text-2); font-size: 11.5px; }
.sum b { font-size: 16px; color: var(--primary); }
.q { padding: 10px 0; border-bottom: 1px dashed var(--line); }
.q-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.q-head .no { width: 20px; height: 20px; border-radius: 50%; background: var(--primary); color: #fff; font-size: 11.5px; display: grid; place-items: center; }
.line { display: flex; gap: 10px; font-size: 12.5px; padding: 2px 0; }
.line span { color: var(--text-2); flex: 0 0 56px; }
</style>
