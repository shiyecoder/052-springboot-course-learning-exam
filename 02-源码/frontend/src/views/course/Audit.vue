<template>
  <ListPage title="开课审核" sub="按师资、学时、容量三项核对；驳回意见不少于 10 字" api="/course/page"
    :params="{ status: '待审核' }"
    :filters="[{ key: 'keyword', label: '课程名称 / 编号' }]"
    :columns="columns" :actions="[{ key: 'pass', label: '通过' }, { key: 'reject', label: '驳回', danger: true }]"
    @action="onAction">
    <template #cell-coverUrl="{ val }"><img v-if="val" :src="val" class="thumb" /></template>
    <template #cell-check="{ row }">
      <span class="tag" :class="row.totalHours > 0 ? 'ok' : 'warn'">课时 {{ row.totalHours }}h</span>
      <span class="tag" style="margin-left: 4px">{{ row.enrollCount }}/{{ row.capacity }}</span>
    </template>
  </ListPage>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../http'
import ListPage from '../../components/ListPage.vue'

// 审核要点：课时合计是否等于总学时、是否含视频节点、容量与师资是否合理
const columns = [
  { prop: 'coverUrl', label: '封面', width: 84 },
  { prop: 'courseName', label: '课程名称', type: 'name', subProp: 'courseNo', minWidth: 200 },
  { prop: 'teacherName', label: '任课教师', width: 100 },
  { prop: 'courseType', label: '类型', width: 80 },
  { prop: 'credit', label: '学分', width: 70 },
  { prop: 'check', label: '课时/容量', width: 150 },
  { prop: 'status', label: '状态', type: 'tag', width: 96 },
  { prop: 'createTime', label: '提交时间', type: 'datetime', width: 150 }
]

async function onAction({ key, row }) {
  if (key === 'pass') {
    await ElMessageBox.confirm(`通过后《${row.courseName}》将进入选课中心，确认？`, '审核通过', { type: 'info' })
    await http.post(`/course/${row.id}/audit`, { pass: true, opinion: '课时与师资符合开课要求' })
    ElMessage.success('已通过')
  } else {
    const { value } = await ElMessageBox.prompt('驳回意见（不少于 10 字）', `驳回《${row.courseName}》`, { inputType: 'textarea' })
    await http.post(`/course/${row.id}/audit`, { pass: false, opinion: value })
    ElMessage.success('已驳回并通知任课教师')
  }
  location.reload()
}
</script>
