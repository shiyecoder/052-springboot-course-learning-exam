<template>
  <ListPage title="考试列表" sub="状态机：待审核 → 待开始 → 进行中 → 待批阅 → 已出分 → 已归档" api="/exam/page"
    :filters="filters" :columns="columns" :actions="actions" @action="onAction">
    <template #cell-progress="{ row }">
      <div style="display: flex; gap: 6px; flex-wrap: wrap">
        <span class="tag info">{{ row.questionCount }} 题</span>
        <span class="tag">{{ row.recordCount }} 人作答</span>
      </div>
    </template>
  </ListPage>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../http'
import ListPage from '../../components/ListPage.vue'

const filters = [
  { key: 'keyword', label: '考试名称' },
  { key: 'status', type: 'select', label: '状态', options: ['待审核', '待开始', '进行中', '待批阅', '已出分', '已归档'].map((x) => ({ label: x, value: x })) }
]
const columns = [
  { prop: 'examName', label: '考试名称', type: 'name', subProp: 'examNo', minWidth: 220 },
  { prop: 'courseName', label: '课程', minWidth: 150 },
  { prop: 'examType', label: '类型', width: 100 },
  { prop: 'startTime', label: '开始时间', type: 'datetime', width: 150 },
  { prop: 'durationMin', label: '时长(分)', width: 90 },
  { prop: 'totalScore', label: '总分', width: 70 },
  { prop: 'cheatLevel', label: '防作弊', width: 90 },
  { prop: 'progress', label: '题量/作答', width: 160 },
  { prop: 'status', label: '状态', type: 'tag', width: 96 }
]
const actions = [
  { key: 'audit', label: '审核' },
  { key: 'publish', label: '发布成绩' },
  { key: 'grading', label: '去阅卷' }
]

async function onAction({ key, row }) {
  if (key === 'audit') {
    await http.post(`/exam/${row.id}/audit`, { pass: true, opinion: '' })
    ElMessage.success('已通过审核，考试进入待开始')
  } else if (key === 'publish') {
    await ElMessageBox.confirm('存在未批阅主观题时将无法发布，确认发布？', '发布成绩', { type: 'warning' })
    const r = await http.post(`/exam/${row.id}/publish-score`)
    ElMessage.success(`已发布 ${r.published} 份成绩，补考名单 ${r.failCount} 人`)
  } else {
    location.hash = '#/exam/grading'
    return
  }
  location.reload()
}
</script>
