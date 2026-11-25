<template>
  <ListPage title="题库管理" sub="按课程与章节归类；单选题正确答案必须且只能有一个" api="/exam-question/page"
    :filters="filters" :columns="columns" :actions="[{ key: 'del', label: '删除', danger: true }]" @action="onAction">
    <template #head><el-button type="primary" :icon="Plus" @click="open()">录入题目</el-button></template>
    <template #cell-stem="{ row }">
      <div style="max-width: 460px">{{ row.stem }}</div>
      <div class="tsub">{{ row.questionNo }} · {{ row.courseName }}</div>
    </template>
    <template #footer>
      <el-dialog v-model="dialog" title="录入题目" width="600px">
        <el-form :model="form" label-width="96px">
          <el-form-item label="所属课程">
            <el-select v-model="form.courseId" style="width: 100%" @change="loadChapters">
              <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属章节">
            <el-select v-model="form.chapterId" style="width: 100%">
              <el-option v-for="ch in chapters" :key="ch.id" :label="ch.chapterName" :value="ch.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="题型">
            <el-select v-model="form.questionType" style="width: 100%">
              <el-option v-for="x in ['单选题', '多选题', '判断题', '填空题', '简答题']" :key="x" :label="x" :value="x" />
            </el-select>
          </el-form-item>
          <el-form-item label="题干"><el-input v-model="form.stem" type="textarea" :rows="3" /></el-form-item>
          <el-form-item label="选项（JSON）">
            <el-input v-model="form.optionsJson" type="textarea" :rows="2" placeholder='["A. ...","B. ..."]' />
          </el-form-item>
          <el-form-item label="标准答案"><el-input v-model="form.answer" placeholder="单选填 A，多选填 ABD" /></el-form-item>
          <el-form-item label="分值"><el-input-number v-model="form.score" :min="1" :max="50" /></el-form-item>
          <el-form-item label="难度"><el-rate v-model="form.difficulty" :max="3" /></el-form-item>
          <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" :rows="2" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialog = false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
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
const courses = ref([])
const chapters = ref([])
const form = reactive({ courseId: null, chapterId: null, questionType: '单选题', stem: '', optionsJson: '["A. ","B. ","C. ","D. "]', answer: '', score: 5, difficulty: 1, analysis: '' })

const filters = [
  { key: 'keyword', label: '题干关键字' },
  { key: 'questionType', type: 'select', label: '题型', options: ['单选题', '多选题', '判断题', '填空题', '简答题'].map((x) => ({ label: x, value: x })) }
]
const columns = [
  { prop: 'stem', label: '题干', minWidth: 420 },
  { prop: 'questionType', label: '题型', width: 90 },
  { prop: 'score', label: '分值', width: 70 },
  { prop: 'difficulty', label: '难度', width: 70 },
  { prop: 'useCount', label: '引用次数', width: 90 },
  { prop: 'status', label: '状态', type: 'tag', width: 80 },
  { prop: 'createTime', label: '创建时间', type: 'datetime', width: 150 }
]

async function loadChapters() {
  chapters.value = (await http.get('/course-chapter/page', { params: { courseId: form.courseId } }))
    .flatMap((c) => (c.children && c.children.length ? c.children : [c]))
}

function open() {
  dialog.value = true
}

async function save() {
  await http.post('/exam-question', form)
  ElMessage.success('已录入题库')
  dialog.value = false
  location.reload()
}

async function onAction({ key, row }) {
  if (key !== 'del') return
  await ElMessageBox.confirm('题目已被试卷引用时不可删除，确认尝试删除？', '删除题目', { type: 'warning' })
  await http.delete(`/exam-question/${row.id}`)
  ElMessage.success('已删除')
  location.reload()
}

onMounted(async () => {
  const d = await http.get('/course/page', { params: { page: 1, size: 100 } })
  courses.value = d.records || []
  form.courseId = courses.value[0]?.id || null
  await loadChapters()
  form.chapterId = chapters.value[0]?.id || null
})
</script>
