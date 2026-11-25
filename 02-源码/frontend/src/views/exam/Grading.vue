<template>
  <div>
    <div class="page-head">
      <h2>阅卷台</h2>
      <span class="sub">客观题交卷即判分；主观题评分不得超过分值且评语必填</span>
      <div class="grow" />
      <span class="tag info">待批阅 {{ total }} 份</span>
    </div>

    <div style="display: grid; grid-template-columns: 300px 1fr; gap: 12px; align-items: start">
      <!-- 左列：待批阅答卷 -->
      <div class="card" style="padding: 10px">
        <el-input v-model="keyword" placeholder="搜索学生 / 考试" size="small" clearable style="margin-bottom: 8px" />
        <div v-for="r in list" :key="r.id" class="q-item" :class="{ on: current?.record?.id === r.id }" @click="open(r)">
          <div class="tname">{{ r.studentName }}</div>
          <div class="tsub">{{ r.examName }}</div>
          <div class="tsub">交卷 {{ fmtShort(r.submitTime) }} · 待批 {{ r.pendingCount }} 题</div>
        </div>
        <div v-if="!list.length" class="empty">暂无待批阅答卷</div>
      </div>

      <!-- 中间+右侧：作答与评分 -->
      <div class="card" v-if="current">
        <h3>{{ current.exam?.examName }}<span class="tip">{{ current.student?.realName }} · 客观题 {{ current.record.objectiveScore }} 分</span></h3>
        <el-table :data="current.details" size="small" border>
          <el-table-column prop="sortNo" label="#" width="50" />
          <el-table-column prop="questionType" label="题型" width="90" />
          <el-table-column prop="answerContent" label="学生作答" min-width="220" show-overflow-tooltip />
          <el-table-column prop="correctFlag" label="判定" width="70">
            <template #default="{ row }"><span class="tag" :class="row.correctFlag ? 'ok' : ''">{{ row.correctFlag ? '正确' : '待评' }}</span></template>
          </el-table-column>
          <el-table-column prop="score" label="得分" width="80" />
          <el-table-column prop="fullScore" label="满分" width="70" />
          <el-table-column label="评分" width="260">
            <template #default="{ row }">
              <template v-if="row.questionType === '简答题'">
                <el-input-number v-model="row._score" :min="0" :max="Number(row.fullScore)" size="small" style="width: 100px" />
                <el-input v-model="row._comment" size="small" placeholder="评语（必填）" style="width: 140px; margin-left: 6px" />
              </template>
              <span v-else class="tsub">自动判分</span>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px">
          <el-button @click="current = null">收起</el-button>
          <el-button type="primary" @click="submitGrade">提交并下一份</el-button>
        </div>
      </div>
      <div class="card" v-else><div class="empty">从左侧选择一份答卷开始批阅</div></div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../http'
import { fmtShort } from '../../utils/format'

const list = ref([])
const total = ref(0)
const keyword = ref('')
const current = ref(null)

async function load() {
  const d = await http.get('/exam-record/grading', { params: { page: 1, size: 50 } })
  list.value = (d.records || []).filter((r) => !keyword.value || r.studentName.includes(keyword.value) || r.examName.includes(keyword.value))
  total.value = d.total || 0
  // 默认打开第一份答卷，避免右侧长时间空白（批阅效率也更高）
  if (!current.value && list.value.length) open(list.value[0])
}

async function open(row) {
  const d = await http.get(`/exam-record/${row.id}`)
  d.details = (d.details || []).map((x) => ({ ...x, _score: Number(x.score) || 0, _comment: '' }))
  current.value = d
}

async function submitGrade() {
  const items = current.value.details
    .filter((d) => d.questionType === '简答题')
    .map((d) => ({ id: d.id, score: d._score, comment: d._comment }))
  if (items.some((i) => !i.comment)) return ElMessage.warning('主观题评语必填')
  const r = await http.post(`/exam-record/${current.value.record.id}/grade`, { items })
  ElMessage.success(r.remain ? `已保存，剩余 ${r.remain} 题未批` : `批阅完成，总分 ${r.totalScore}`)
  current.value = null
  load()
}

onMounted(load)
</script>

<style scoped>
.q-item { padding: 8px 10px; border-radius: 6px; cursor: pointer; border: 1px solid transparent; }
.q-item:hover { background: #F7F9FA; }
.q-item.on { background: #EAF2F8; border-color: #CFE1EE; }
</style>
