<template>
  <div>
    <div class="page-head">
      <h2>数据字典</h2>
      <span class="sub">课程状态、考试类型等枚举集中维护，业务页面直接引用</span>
    </div>

    <div class="split">
      <div class="card types">
        <h3>字典类型 <span class="tip">共 {{ types.length }} 类</span></h3>
        <div
          v-for="t in types"
          :key="t.dictType"
          :class="['type', cur && cur.dictType === t.dictType ? 'on' : '']"
          @click="pick(t)"
        >
          <div class="tname">{{ t.dictName }}</div>
          <div class="tsub">{{ t.dictType }} · {{ t.count }} 项</div>
        </div>
      </div>

      <div class="card" style="flex: 1">
        <h3>
          <template v-if="cur">{{ cur.dictName }}<span class="tip">{{ cur.dictType }}</span></template>
          <template v-else>字典项</template>
          <div class="grow" />
          <el-button size="small" type="primary" :icon="Plus" :disabled="!cur" @click="openDialog()">新增字典项</el-button>
        </h3>
        <el-table :data="items" v-loading="loading" stripe :empty-text="cur ? '该类型暂无数据' : '请先选择左侧字典类型'">
          <el-table-column prop="dictLabel" label="标签" min-width="160" />
          <el-table-column prop="dictValue" label="值" min-width="140" />
          <el-table-column prop="tagType" label="标签样式" width="120">
            <template #default="{ row }">
              <span v-if="row.tagType" class="tag" :class="row.tagType">{{ row.tagType }}</span>
              <span v-else class="tsub">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="sortNo" label="排序" width="80" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <span class="tag" :class="row.status === 1 ? 'ok' : 'dan'">{{ row.status === 1 ? '启用' : '停用' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="right">
            <template #default="{ row }"><a class="op" @click="openDialog(row)">编辑</a></template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑字典项' : '新增字典项'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="所属类型"><el-input :model-value="form.dictType" disabled /></el-form-item>
        <el-form-item label="标签" required><el-input v-model="form.dictLabel" /></el-form-item>
        <el-form-item label="值" required><el-input v-model="form.dictValue" /></el-form-item>
        <el-form-item label="标签样式">
          <el-select v-model="form.tagType" clearable placeholder="默认灰色">
            <el-option label="成功 ok" value="ok" />
            <el-option label="警告 warn" value="warn" />
            <el-option label="危险 dan" value="dan" />
            <el-option label="信息 info" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="1" :max="99" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http from '../../http'

const types = ref([])
const items = ref([])
const cur = ref(null)
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const form = reactive({})

onMounted(async () => {
  types.value = await http.get('/system/dict/types')
  if (types.value.length) pick(types.value[0])
})

async function pick(t) {
  cur.value = t
  loading.value = true
  try {
    items.value = await http.get('/system/dict/data', { params: { type: t.dictType } })
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row } : { dictType: cur.value.dictType, status: 1, sortNo: 10 })
  dlg.value = true
}

async function save() {
  if (!form.dictLabel || !form.dictValue) {
    ElMessage.warning('标签与值不能为空')
    return
  }
  saving.value = true
  try {
    await http.post('/system/dict/data', { ...form })
    ElMessage.success('保存成功')
    dlg.value = false
    // 条目数会变化，刷新左栏统计
    types.value = await http.get('/system/dict/types')
    cur.value = types.value.find((x) => x.dictType === form.dictType) || cur.value
    pick(cur.value)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.split { display: flex; gap: 12px; align-items: flex-start; }
.types { width: 260px; flex: 0 0 260px; padding: 14px 10px; }
.types h3 { padding: 0 6px; }
.type { padding: 9px 10px; border-radius: 6px; cursor: pointer; border: 1px solid transparent; }
.type:hover { background: #f5f8fa; }
.type.on { background: #eaf2f8; border-color: #cfe1ee; }
.card h3 { display: flex; align-items: center; gap: 8px; }
.card h3 .grow { flex: 1; }
.op { color: var(--primary-2); cursor: pointer; }
</style>
