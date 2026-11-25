<template>
  <div v-loading="loading">
    <div v-if="!recordId" class="empty">考试加载失败或当前状态不可作答</div>

    <template v-else>
      <div class="exam-head">
        <div>
          <h2>{{ exam.examName }}</h2>
          <div class="tsub">
            {{ exam.examType }} · {{ exam.totalScore }} 分 · {{ exam.durationMin }} 分钟 · 防作弊等级
            {{ exam.cheatLevel }}
          </div>
        </div>
        <div class="grow" />
        <div class="timer" :class="remain < 300 ? 'dan' : ''">
          <span class="tsub">剩余时间</span>
          <b>{{ left }}</b>
        </div>
        <el-button type="primary" :loading="submitting" @click="submit(false)">交卷</el-button>
      </div>

      <div class="exam-body">
        <!-- 答题卡 -->
        <div class="card sheet">
          <h3>答题卡</h3>
          <div class="grid">
            <div
              v-for="(q, i) in questions"
              :key="q.questionId"
              :class="['cell', answers[q.questionId] ? 'done' : '']"
              @click="scrollTo(i)"
            >
              {{ i + 1 }}
            </div>
          </div>
          <div class="tsub" style="margin-top: 8px">已答 {{ answeredCount }} / {{ questions.length }}</div>
          <div class="cheat" v-if="cheatCount">
            <span class="tag dan">切屏 {{ cheatCount }} 次</span>
            <div class="tsub">达到上限将强制交卷</div>
          </div>
        </div>

        <!-- 题目 -->
        <div class="card paper">
          <div v-for="(q, i) in questions" :key="q.questionId" :ref="(el) => (itemRefs[i] = el)" class="q">
            <div class="q-head">
              <span class="no">{{ i + 1 }}</span>
              <span class="tag info">{{ q.questionType }}</span>
              <span class="tsub">{{ q.score }} 分</span>
            </div>
            <div class="stem">{{ q.stem }}</div>

            <el-radio-group
              v-if="q.questionType === '单选题' || q.questionType === '判断题'"
              v-model="answers[q.questionId]"
              class="opts"
            >
              <el-radio v-for="o in parseOptions(q)" :key="o.key" :value="o.key" class="opt">
                <b v-if="o.key !== o.text">{{ o.key }}.</b> {{ o.text }}
              </el-radio>
            </el-radio-group>

            <el-checkbox-group v-else-if="q.questionType === '多选题'" v-model="multi[q.questionId]" class="opts">
              <el-checkbox v-for="o in parseOptions(q)" :key="o.key" :value="o.key" class="opt">
                <b v-if="o.key !== o.text">{{ o.key }}.</b> {{ o.text }}
              </el-checkbox>
            </el-checkbox-group>

            <el-input
              v-else
              v-model="answers[q.questionId]"
              :type="q.questionType === '简答题' ? 'textarea' : 'text'"
              :rows="4"
              :placeholder="q.questionType === '填空题' ? '请填写答案' : '请作答，阅卷时按要点给分'"
            />
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../http'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const recordId = ref(null)
const exam = ref({})
const questions = ref([])
const answers = reactive({})
const multi = reactive({})
const remain = ref(0)
const cheatCount = ref(0)
const itemRefs = ref([])
let timer = null

const answeredCount = computed(() => questions.value.filter((q) => answers[q.questionId]).length)
const left = computed(() => {
  const m = Math.floor(remain.value / 60)
  const s = remain.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

onMounted(async () => {
  loading.value = true
  try {
    const d = await http.post(`/exam/${route.params.id}/start`)
    recordId.value = d.recordId
    exam.value = d.exam || {}
    questions.value = d.questions || []
    remain.value = d.remainSeconds || 0
    // 回填已保存的答案，刷新页面不丢作答
    ;(d.details || []).forEach((x) => {
      if (x.answerContent) answers[x.questionId] = x.answerContent
    })
    questions.value.forEach((q) => {
      if (q.questionType === '多选题') {
        multi[q.questionId] = answers[q.questionId] ? String(answers[q.questionId]).split('') : []
      }
    })
    startTimer()
    window.addEventListener('visibilitychange', onHide)
  } finally {
    loading.value = false
  }
})

// 多选题：勾选结果同步回 answers，提交时统一序列化
function syncMulti() {
  Object.entries(multi).forEach(([qid, arr]) => {
    answers[qid] = arr.length ? [...arr].sort().join('') : ''
  })
}

function startTimer() {
  timer = setInterval(() => {
    syncMulti()
    if (remain.value > 0) remain.value -= 1
    else {
      clearInterval(timer)
      ElMessage.warning('考试时间已到，系统自动交卷')
      submit(true)
    }
  }, 1000)
}

function onHide() {
  if (document.hidden && recordId.value) reportCheat('切屏')
}

async function reportCheat(type) {
  const r = await http.post(`/exam-record/${recordId.value}/cheat-event`, { eventType: type, device: 'Chrome/Web' })
  cheatCount.value = r.totalCount
  if (r.forceSubmit) {
    ElMessage.error('防作弊次数达到上限，已强制交卷')
    clearInterval(timer)
    setTimeout(() => router.push('/portal/score'), 1200)
    return
  }
  ElMessage.warning(`已记录${type}行为（第 ${r.totalCount} 次，上限 ${r.limit} 次）`)
}

function parseOptions(q) {
  let arr = []
  try {
    arr = JSON.parse(q.optionsJson || '[]')
  } catch (e) {
    arr = []
  }
  if (!Array.isArray(arr)) return []
  // 题库里选项可能是 "A. 文本"，也可能是纯文本（判断题「正确/错误」本身即答案值）
  return arr.map((x, i) => {
    if (typeof x !== 'string') return x
    const m = x.match(/^([A-Za-z])[.、．)]\s*(.+)$/)
    if (m) return { key: m[1].toUpperCase(), text: m[2] }
    return { key: x, text: x }
  })
}

function scrollTo(i) {
  itemRefs.value[i]?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

async function submit(auto) {
  syncMulti()
  if (!auto) {
    const un = questions.value.length - answeredCount.value
    await ElMessageBox.confirm(
      un > 0 ? `还有 ${un} 题未作答，确认交卷？` : '确认交卷？交卷后不可修改。',
      '交卷确认',
      { type: 'warning' }
    )
  }
  submitting.value = true
  try {
    const r = await http.post(`/exam-record/${recordId.value}/submit`, { answers: { ...answers } })
    clearInterval(timer)
    const msg = r.duplicated ? '该答卷已提交过，无需重复提交' : `交卷成功，客观题得分 ${r.objectiveScore ?? r.totalScore ?? 0} 分`
    ElMessage.success(msg)
    setTimeout(() => router.push('/portal/score'), 900)
  } finally {
    submitting.value = false
  }
}

onBeforeUnmount(() => {
  clearInterval(timer)
  window.removeEventListener('visibilitychange', onHide)
})
</script>

<style scoped>
.exam-head { display: flex; align-items: center; gap: 14px; margin-bottom: 12px; }
.exam-head h2 { font-size: 17px; margin: 0 0 4px; }
.exam-head .grow { flex: 1; }
.timer { text-align: right; }
.timer b { display: block; font-size: 20px; color: var(--primary); letter-spacing: 1px; }
.timer.dan b { color: var(--dan); }
.exam-body { display: flex; gap: 12px; align-items: flex-start; }
.sheet { width: 220px; flex: 0 0 220px; position: sticky; top: 0; }
.sheet .grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px; }
.sheet .cell {
  height: 30px; border: 1px solid var(--line); border-radius: 4px; display: grid; place-items: center;
  font-size: 12px; cursor: pointer; color: var(--text-2);
}
.sheet .cell:hover { border-color: var(--primary-2); color: var(--primary); }
.sheet .cell.done { background: #eaf2f8; border-color: #cfe1ee; color: var(--primary); font-weight: 600; }
.sheet .cheat { margin-top: 10px; display: flex; flex-direction: column; gap: 4px; }
.paper { flex: 1; min-width: 0; }
.q { padding: 12px 0; border-bottom: 1px dashed var(--line); }
.q:first-child { padding-top: 0; }
.q:last-child { border-bottom: 0; }
.q-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.q-head .no {
  width: 22px; height: 22px; border-radius: 50%; background: var(--primary); color: #fff;
  font-size: 12px; display: grid; place-items: center;
}
.stem { font-size: 13.5px; line-height: 1.75; margin-bottom: 8px; }
.opts { display: flex; flex-direction: column; gap: 6px; align-items: flex-start; }
.opt { height: auto; margin-right: 0; white-space: normal; line-height: 1.6; }
</style>
