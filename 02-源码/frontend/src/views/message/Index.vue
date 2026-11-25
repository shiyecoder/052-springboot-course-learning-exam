<template>
  <div>
    <div class="page-head">
      <h2>消息中心</h2>
      <span class="sub">考试提醒、成绩发布、预警通知与讨论回复推送</span>
      <div class="grow" />
      <el-radio-group v-model="readFlag" size="small" @change="load(1)">
        <el-radio-button :value="''">全部</el-radio-button>
        <el-radio-button :value="0">未读</el-radio-button>
        <el-radio-button :value="1">已读</el-radio-button>
      </el-radio-group>
      <el-button size="small" @click="readAll">全部已读</el-button>
    </div>

    <div class="card">
      <div v-for="m in rows" :key="m.id" class="msg" :class="{ unread: !m.readFlag }" @click="open(m)">
        <div style="flex: 1">
          <div class="tname">{{ m.title }}<span class="tag info" style="margin-left: 6px">{{ m.msgType }}</span></div>
          <div class="tsub" style="line-height: 1.7">{{ m.content }}</div>
        </div>
        <div class="tsub">{{ fmtFromNow(m.createTime) }}</div>
      </div>
      <div v-if="!rows.length" class="empty">暂无消息</div>
      <div class="pager">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="load()" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../../http'
import { fmtFromNow } from '../../utils/format'

const rows = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const readFlag = ref('')

async function load(p) {
  if (p) page.value = p
  const params = { page: page.value, size: size.value }
  if (readFlag.value !== '') params.readFlag = readFlag.value
  const d = await http.get('/system/message/page', { params })
  rows.value = d.records || []
  total.value = d.total || 0
}

async function open(m) {
  if (!m.readFlag) {
    await http.post(`/system/message/${m.id}/read`)
    m.readFlag = 1
  }
}

async function readAll() {
  await http.post('/system/message/read-all')
  load()
}

onMounted(() => load(1))
</script>

<style scoped>
.msg { display: flex; gap: 12px; padding: 12px 4px; border-bottom: 1px solid #F0F3F5; cursor: pointer; }
.msg:hover { background: #FAFBFC; }
.msg.unread .tname::before { content: '●'; color: #F39C12; font-size: 9px; margin-right: 5px; vertical-align: 2px; }
</style>
