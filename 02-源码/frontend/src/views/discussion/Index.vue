<template>
  <div>
    <div class="page-head">
      <h2>讨论答疑</h2>
      <span class="sub">按课程开设主题，教师与助教可标记精华与已解决</span>
      <div class="grow" />
      <el-input v-model="keyword" placeholder="搜索标题或内容" style="width: 220px" clearable @change="load(1)" />
    </div>

    <ListPage ref="lp" title="" sub="" api="/discussion/page"
      :filters="[{ key: 'courseId', type: 'select', label: '课程', options: courseOpts }]"
      :columns="columns"
      :actions="[{ key: 'reply', label: '查看/回复' }, { key: 'essence', label: '精华' }, { key: 'solved', label: '已解决' }]"
      @action="onAction">
      <template #cell-title="{ row }">
        <div class="tname">
          <span v-if="row.isTop" class="tag warn" style="margin-right: 4px">置顶</span>
          <span v-if="row.isEssence" class="tag ok" style="margin-right: 4px">精华</span>
          {{ row.title }}
        </div>
        <div class="tsub">{{ row.courseName }} · {{ row.authorName }}</div>
      </template>
      <template #cell-isSolved="{ row }">
        <span class="tag" :class="row.isSolved ? 'ok' : 'warn'">{{ row.isSolved ? '已解决' : '待解决' }}</span>
      </template>
    </ListPage>

    <el-drawer v-model="drawer" title="帖子详情" size="560px">
      <template v-if="post">
        <h3 style="margin: 0 0 6px">{{ post.post.title }}</h3>
        <div class="tsub" style="margin-bottom: 10px">{{ post.courseName }} · {{ post.authorName }} · {{ fmtDateTime(post.post.createTime) }}</div>
        <p style="line-height: 1.8">{{ post.post.content }}</p>
        <h3 style="font-size: 13px; margin: 16px 0 8px">回复（{{ post.replies.length }}）</h3>
        <div v-for="r in post.replies" :key="r.id" style="padding: 8px 0; border-top: 1px solid #EEF2F5">
          <div class="tname">{{ r.authorName }} <span class="tsub">{{ r.authorTitle }}</span>
            <span v-if="r.isAdopted" class="tag ok" style="margin-left: 6px">已采纳</span></div>
          <div style="line-height: 1.7">{{ r.content }}</div>
          <div class="tsub">{{ fmtDateTime(r.createTime) }}</div>
        </div>
        <el-input v-model="replyText" type="textarea" :rows="3" placeholder="写下你的回复…" style="margin-top: 12px" />
        <div style="display: flex; justify-content: flex-end; margin-top: 8px">
          <el-button type="primary" @click="sendReply">回复</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../http'
import ListPage from '../../components/ListPage.vue'
import { fmtDateTime } from '../../utils/format'

const lp = ref(null)
const keyword = ref('')
const drawer = ref(false)
const post = ref(null)
const replyText = ref('')
const courses = ref([])
const courseOpts = computed(() => courses.value.map((c) => ({ label: c.courseName, value: c.id })))

const columns = [
  { prop: 'title', label: '主题', minWidth: 320 },
  { prop: 'replyCount', label: '回复', width: 70 },
  { prop: 'isSolved', label: '状态', width: 100 },
  { prop: 'createTime', label: '发布时间', type: 'datetime', width: 150 }
]

async function onAction({ key, row }) {
  if (key === 'reply') {
    post.value = await http.get(`/discussion/${row.id}`)
    drawer.value = true
    return
  }
  await http.post(`/discussion/${row.id}/mark?type=${key}&value=${row[key === 'essence' ? 'isEssence' : 'isSolved'] ? 'false' : 'true'}`)
  ElMessage.success('已更新标记')
  lp.value?.load()
}

async function sendReply() {
  if (!replyText.value.trim()) return ElMessage.warning('回复内容不能为空')
  await http.post(`/discussion/${post.value.post.id}/reply`, { content: replyText.value })
  ElMessage.success('已回复')
  post.value = await http.get(`/discussion/${post.value.post.id}`)
  replyText.value = ''
  lp.value?.load()
}

async function load() {
  lp.value?.load(1)
}

onMounted(async () => {
  courses.value = (await http.get('/course/page', { params: { page: 1, size: 100 } })).records || []
})
</script>
