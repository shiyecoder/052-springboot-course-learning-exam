<template>
  <ListPage title="成绩管理" sub="成绩发布后只读；复核由非原阅卷人处理" api="/score/page"
    :filters="[{ key: 'courseId', type: 'select', label: '课程', options: [] }]"
    :columns="columns">
    <template #cell-totalScore="{ row }">
      <b :style="{ color: row.passFlag ? 'var(--ok)' : 'var(--dan)' }">{{ row.totalScore }}</b>
      <span class="tsub"> / {{ row.fullScore }}</span>
    </template>
    <template #cell-passFlag="{ row }">
      <span class="tag" :class="row.passFlag ? 'ok' : 'dan'">{{ row.passFlag ? '通过' : '不及格' }}</span>
    </template>
  </ListPage>
</template>

<script setup>
import http from '../../http'
import ListPage from '../../components/ListPage.vue'

const columns = [
  { prop: 'studentName', label: '学生', type: 'name', subProp: 'studentNo', width: 140 },
  { prop: 'courseName', label: '课程', minWidth: 160 },
  { prop: 'examName', label: '考试', minWidth: 180 },
  { prop: 'examType', label: '类型', width: 100 },
  { prop: 'objectiveScore', label: '客观题', width: 90 },
  { prop: 'subjectiveScore', label: '主观题', width: 90 },
  { prop: 'totalScore', label: '总评', width: 100 },
  { prop: 'passFlag', label: '结论', width: 90 },
  { prop: 'submitTime', label: '交卷时间', type: 'datetime', width: 150 }
]
</script>
