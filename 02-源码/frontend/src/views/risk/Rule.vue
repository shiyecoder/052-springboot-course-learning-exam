<template>
  <div>
    <div class="page-head">
      <h2>预警规则</h2>
      <span class="sub">五项权重合计必须等于 100%；修改后生成新版本，历史计算结果不受影响</span>
      <div class="grow" />
      <el-tag type="success" effect="plain">当前版本 {{ current?.ruleVersion }}</el-tag>
    </div>

    <div class="card">
      <h3>权重与分级阈值<span class="tip">保存后立即用于新计算</span></h3>
      <el-form :model="form" label-width="150px" style="max-width: 720px">
        <el-row :gutter="12">
          <el-col :span="12" v-for="w in weights" :key="w.key">
            <el-form-item :label="w.label">
              <el-input-number v-model="form[w.key]" :min="0" :max="100" :step="5" />
              <span class="tsub" style="margin-left: 6px">%</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert :type="sum === 100 ? 'success' : 'error'" :closable="false" show-icon
          :title="`当前权重合计 ${sum}%${sum === 100 ? '' : '，必须等于 100% 才能保存'}`" style="margin-bottom: 12px" />
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="低风险线"><el-input-number v-model="form.lowLine" :min="0" :max="100" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="关注线"><el-input-number v-model="form.focusLine" :min="0" :max="100" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="中风险线"><el-input-number v-model="form.midLine" :min="0" :max="100" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="规则名称"><el-input v-model="form.ruleName" style="max-width: 420px" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.remark" type="textarea" :rows="2" style="max-width: 520px" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :disabled="sum !== 100" @click="save">保存为新版本</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <h3>版本历史</h3>
      <el-table :data="rules" size="small" border>
        <el-table-column prop="ruleVersion" label="版本" width="120" />
        <el-table-column prop="ruleName" label="名称" min-width="200" />
        <el-table-column label="权重（观看/提交/测验/登录/讨论）" min-width="220">
          <template #default="{ row }">{{ row.weightWatch }}/{{ row.weightSubmit }}/{{ row.weightQuiz }}/{{ row.weightLogin }}/{{ row.weightDiscuss }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><span class="tag" :class="row.status === '启用' ? 'ok' : ''">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column prop="effectTime" label="生效时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.effectTime) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../http'
import { fmtDateTime } from '../../utils/format'

const rules = ref([])
const current = ref(null)
const weights = [
  { key: 'weightWatch', label: '视频观看完成率' },
  { key: 'weightSubmit', label: '作业与测验提交及时率' },
  { key: 'weightQuiz', label: '阶段测验得分率' },
  { key: 'weightLogin', label: '近 14 天登录频次' },
  { key: 'weightDiscuss', label: '讨论区与答疑参与度' }
]
const form = reactive({ ruleName: '', weightWatch: 30, weightSubmit: 20, weightQuiz: 25, weightLogin: 15, weightDiscuss: 10, lowLine: 85, focusLine: 70, midLine: 55, remark: '' })

const sum = computed(() => weights.reduce((s, w) => s + Number(form[w.key] || 0), 0))

async function load() {
  rules.value = await http.get('/risk/rule/list')
  current.value = rules.value.find((r) => r.status === '启用') || rules.value[0]
  if (current.value) Object.assign(form, { ...current.value, id: undefined, ruleName: current.value.ruleName })
}

async function save() {
  await http.post('/risk/rule', { ...form })
  ElMessage.success('已保存新版本，历史计算结果保留原版本')
  load()
}

onMounted(load)
</script>
