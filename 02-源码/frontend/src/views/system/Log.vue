<template>
  <ListPage title="操作日志" sub="记录审核、评分、预警修正等关键动作，便于追溯" api="/system/log/page"
    :filters="filters" :columns="columns">
    <template #cell-content="{ row }">
      <span class="txt">{{ row.content || '—' }}</span>
    </template>
  </ListPage>
</template>

<script setup>
import ListPage from '../../components/ListPage.vue'

const filters = [{ key: 'keyword', label: '操作人 / 模块 / 内容' }]
const columns = [
  { prop: 'operName', label: '操作人', width: 130 },
  { prop: 'module', label: '模块', width: 130 },
  { prop: 'operType', label: '类型', width: 100 },
  { prop: 'bizNo', label: '业务单号', width: 170 },
  { prop: 'content', label: '操作内容', minWidth: 260 },
  { prop: 'ip', label: 'IP', width: 130 },
  { prop: 'operTime', label: '时间', type: 'datetime', width: 160 }
]
</script>

<style scoped>
.txt { color: var(--text); }
</style>
