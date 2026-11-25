<template>
  <ListPage title="教学班" sub="课程下的授课班级，决定课表与统计口径" api="/meta/class/page"
    :filters="[{ key: 'keyword', label: '班级名称 / 编号' }]"
    :columns="columns" :actions="[{ key: 'edit', label: '编辑' }]" @action="onAction">
  </ListPage>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../http'
import ListPage from '../../components/ListPage.vue'

const columns = [
  { prop: 'className', label: '班级名称', type: 'name', subProp: 'classNo', minWidth: 220 },
  { prop: 'teacherName', label: '任课教师', width: 110 },
  { prop: 'major', label: '专业', width: 160 },
  { prop: 'studentCount', label: '人数', width: 80 },
  { prop: 'capacity', label: '容量', width: 80 },
  { prop: 'schedule', label: '上课安排', minWidth: 220 },
  { prop: 'status', label: '状态', type: 'tag', width: 90 }
]

async function onAction({ key, row }) {
  const { value } = await ElMessageBox.prompt('调整容量（1-500）', `编辑 ${row.className}`, {
    inputValue: String(row.capacity),
    inputPattern: /^([1-9]\d{0,2}|500)$/,
    inputErrorMessage: '请输入 1-500 的整数'
  })
  await http.put(`/meta/class/${row.id}`, { ...row, capacity: Number(value) })
  ElMessage.success('已保存')
  location.reload()
}
</script>
