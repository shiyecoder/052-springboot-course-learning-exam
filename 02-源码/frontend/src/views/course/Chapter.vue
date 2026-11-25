<template>
  <div>
    <div class="page-head">
      <h2>章节课时</h2>
      <span class="sub">两级结构：章 → 节；视频节点必须上传资源才能启用</span>
      <div class="grow" />
      <el-select v-model="courseId" style="width: 260px" placeholder="选择课程" @change="load">
        <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
      </el-select>
      <el-button type="primary" :icon="Plus" :disabled="!courseId" @click="openAdd(null)">新增章节</el-button>
    </div>

    <div class="card" v-loading="loading">
      <el-table :data="tree" row-key="id" default-expand-all :tree-props="{ children: 'children' }" stripe>
        <el-table-column prop="chapterName" label="章节名称" min-width="240" />
        <el-table-column prop="nodeType" label="类型" width="90">
          <template #default="{ row }"><span class="tag" :class="row.nodeType === '视频' ? 'info' : ''">{{ row.nodeType }}</span></template>
        </el-table-column>
        <el-table-column prop="durationMin" label="时长(分)" width="90">
          <template #default="{ row }">{{ row.durationMin || '—' }}</template>
        </el-table-column>
        <el-table-column prop="minFinishRate" label="最低完成率" width="100">
          <template #default="{ row }">{{ row.parentId ? row.minFinishRate + '%' : '—' }}</template>
        </el-table-column>
        <el-table-column prop="isRequired" label="必修" width="70">
          <template #default="{ row }"><span class="tag" :class="row.isRequired ? 'ok' : ''">{{ row.isRequired ? '必修' : '选修' }}</span></template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="70" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><span class="tag" :class="row.status ? 'ok' : 'dan'">{{ row.status ? '启用' : '隐藏' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="right" fixed="right">
          <template #default="{ row }">
            <span class="ops">
              <a class="op" @click="move(row, 'up')">上移</a>
              <a class="op" @click="move(row, 'down')">下移</a>
              <a class="op" v-if="!row.parentId" @click="openAdd(row)">加节</a>
              <a class="op" @click="openEdit(row)">编辑</a>
              <a class="op dan" @click="remove(row)">删除</a>
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="editing ? '编辑章节' : '新增章节'" width="520px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="章节名称"><el-input v-model="form.chapterName" /></el-form-item>
        <el-form-item label="节点类型">
          <el-select v-model="form.nodeType" style="width: 100%">
            <el-option v-for="x in ['视频', '文档', '测验', '讨论']" :key="x" :label="x" :value="x" />
          </el-select>
        </el-form-item>
        <el-form-item label="时长（分）"><el-input-number v-model="form.durationMin" :min="0" :max="300" /></el-form-item>
        <el-form-item label="最低完成率"><el-input-number v-model="form.minFinishRate" :min="0" :max="100" /></el-form-item>
        <el-form-item label="必修"><el-switch v-model="form.isRequired" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="资源">
          <el-upload :show-file-list="false" :http-request="uploadRes" accept="video/*,.pdf,.doc,.docx">
            <el-button>{{ form.resourceUrl ? '已上传，重新上传' : '上传视频 / 文档' }}</el-button>
          </el-upload>
          <div class="tsub" v-if="form.resourceUrl">{{ form.resourceUrl }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http from '../../http'
import { fmtMinutes } from '../../utils/format'

const courses = ref([])
const courseId = ref(null)
const tree = ref([])
const loading = ref(false)
const dialog = ref(false)
const editing = ref(false)
const form = reactive({ id: null, courseId: null, parentId: 0, chapterName: '', nodeType: '视频', durationMin: 30, minFinishRate: 90, isRequired: 1, resourceUrl: '' })

async function load() {
  if (!courseId.value) return
  loading.value = true
  try {
    tree.value = await http.get('/course-chapter/page', { params: { courseId: courseId.value } })
  } finally {
    loading.value = false
  }
}

function openAdd(parent) {
  editing.value = false
  Object.assign(form, { id: null, courseId: courseId.value, parentId: parent ? parent.id : 0, chapterName: '', nodeType: parent ? '视频' : '视频', durationMin: parent ? 30 : 0, minFinishRate: 90, isRequired: 1, resourceUrl: '' })
  dialog.value = true
}

function openEdit(row) {
  editing.value = true
  Object.assign(form, row)
  dialog.value = true
}

async function save() {
  if (editing.value) await http.put(`/course-chapter/${form.id}`, form)
  else await http.post('/course-chapter', form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function move(row, direction) {
  await http.post(`/course-chapter/${row.id}/move?direction=${direction}`)
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('已有学习记录的章节将改为隐藏而不是删除，确认继续？', '删除章节', { type: 'warning' })
  const r = await http.delete(`/course-chapter/${row.id}`)
  ElMessage.success(r.hidden ? `已有 ${r.progressCount} 条学习记录，已改为隐藏` : '已删除')
  load()
}

async function uploadRes(opt) {
  const fd = new FormData()
  fd.append('file', opt.file)
  fd.append('bizType', 'lesson')
  const d = await http.post('/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  form.resourceUrl = d.url
  ElMessage.success('资源已上传')
}

onMounted(async () => {
  const d = await http.get('/course/page', { params: { page: 1, size: 100 } })
  courses.value = d.records || []
  courseId.value = courses.value[0]?.id || null
  load()
})
</script>
