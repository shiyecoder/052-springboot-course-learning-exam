<template>
  <ListPage title="课程列表" sub="按学期与状态维护；已发布课程进入选课中心" api="/course/page"
    :filters="filters" :columns="columns" :actions="actions" @action="onAction">
    <template #cell-coverUrl="{ val }"><img v-if="val" :src="val" class="thumb" /><div v-else class="thumb" /></template>
    <template #head><el-button type="primary" :icon="Plus" @click="dialog = true">新建课程</el-button></template>
    <template #footer>
      <el-dialog v-model="dialog" title="新建课程" width="560px">
        <el-form :model="form" label-width="96px">
          <el-form-item label="课程名称" required><el-input v-model="form.courseName" placeholder="2 至 60 字" /></el-form-item>
          <el-form-item label="学期" required>
            <el-select v-model="form.termId" style="width: 100%">
              <el-option v-for="t in terms" :key="t.id" :label="t.termName" :value="t.id" :disabled="t.status === '已结束'" />
            </el-select>
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="form.courseType" style="width: 100%">
              <el-option v-for="x in ['必修', '选修', '公选', '重修']" :key="x" :label="x" :value="x" />
            </el-select>
          </el-form-item>
          <el-form-item label="学分"><el-input-number v-model="form.credit" :min="0.5" :max="10" :step="0.5" /></el-form-item>
          <el-form-item label="容量"><el-input-number v-model="form.capacity" :min="1" :max="500" /></el-form-item>
          <el-form-item label="封面">
            <el-upload :show-file-list="false" :http-request="uploadCover" accept="image/*">
              <img v-if="form.coverUrl" :src="form.coverUrl" style="width: 120px; height: 76px; object-fit: cover; border-radius: 4px" />
              <el-button v-else>上传封面</el-button>
            </el-upload>
          </el-form-item>
          <el-form-item label="课程简介" required><el-input v-model="form.intro" type="textarea" :rows="4" placeholder="不少于 30 字" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialog = false">取消</el-button>
          <el-button type="primary" @click="save">保存为草稿</el-button>
        </template>
      </el-dialog>
    </template>
  </ListPage>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http from '../../http'
import ListPage from '../../components/ListPage.vue'

const dialog = ref(false)
const terms = ref([])
const form = reactive({ courseName: '', termId: null, courseType: '必修', credit: 3, capacity: 60, intro: '', coverUrl: '' })

const filters = [
  { key: 'keyword', label: '课程名称 / 编号' },
  { key: 'status', type: 'select', label: '状态', options: ['草稿', '待审核', '已发布', '已结课', '已下架'].map((x) => ({ label: x, value: x })) },
  { key: 'courseType', type: 'select', label: '类型', options: ['必修', '选修', '公选', '重修'].map((x) => ({ label: x, value: x })) }
]
const columns = [
  { prop: 'coverUrl', label: '封面', width: 84 },
  { prop: 'courseName', label: '课程名称', type: 'name', subProp: 'courseNo', minWidth: 200 },
  { prop: 'teacherName', label: '任课教师', width: 100 },
  { prop: 'termName', label: '学期', width: 170 },
  { prop: 'credit', label: '学分', width: 70 },
  { prop: 'capacity', label: '已选/容量', type: 'cap', width: 110 },
  { prop: 'status', label: '状态', type: 'tag', width: 96 },
  { prop: 'createTime', label: '创建时间', type: 'datetime', width: 150 }
]
const actions = [
  { key: 'submit', label: '提交审核' },
  { key: 'audit', label: '审核' },
  { key: 'offline', label: '下架', danger: true }
]

async function onAction({ key, row }) {
  if (key === 'submit') {
    await http.post(`/course/${row.id}/submit`)
    ElMessage.success('已提交审核，等待教务核验课时与师资')
  } else if (key === 'audit') {
    const pass = await ElMessageBox.confirm(`确认通过《${row.courseName}》的开课审核？`, '开课审核', {
      confirmButtonText: '通过', cancelButtonText: '驳回', distinguishCancelAndClose: true, type: 'info'
    }).then(() => true).catch((a) => (a === 'cancel' ? false : Promise.reject(a)))
    let opinion = ''
    if (!pass) opinion = (await ElMessageBox.prompt('驳回意见（不少于 10 字）', '驳回开课申请', { inputType: 'textarea' })).value
    await http.post(`/course/${row.id}/audit`, { pass, opinion })
    ElMessage.success(pass ? '已通过，课程进入选课中心' : '已驳回并通知任课教师')
  } else {
    await ElMessageBox.confirm('下架后学生不可再选课，已选记录保留。确认下架？', '提示', { type: 'warning' })
    await http.delete(`/course/${row.id}`)
    ElMessage.success('已下架')
  }
  location.reload()
}

async function uploadCover(opt) {
  const fd = new FormData()
  fd.append('file', opt.file)
  fd.append('bizType', 'course')
  form.coverUrl = (await http.post('/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })).url
}

async function save() {
  await http.post('/course', form)
  ElMessage.success('已保存为草稿，补充章节后可提交审核')
  dialog.value = false
  location.reload()
}

onMounted(async () => {
  terms.value = await http.get('/meta/term/list')
  form.termId = terms.value.find((t) => t.status === '进行中')?.id || null
})
</script>
