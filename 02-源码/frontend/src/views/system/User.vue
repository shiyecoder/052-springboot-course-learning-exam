<template>
  <ListPage
    ref="lp"
    title="用户管理"
    sub="账号由管理员统一维护，停用后立即无法登录"
    api="/system/user/page"
    :filters="filters"
    :columns="columns"
    :actions="actions"
    @action="onAction"
  >
    <template #head>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增用户</el-button>
    </template>
    <template #cell-realName="{ row }">
      <div style="display: flex; align-items: center; gap: 8px">
        <img :src="imgUrl(row.avatar)" class="av" alt="" />
        <div>
          <div class="tname">{{ row.realName }}</div>
          <div class="tsub">{{ row.username }}<span v-if="row.userNo"> · {{ row.userNo }}</span></div>
        </div>
      </div>
    </template>
  </ListPage>

  <el-dialog v-model="dlg" :title="form.id ? '编辑用户' : '新增用户'" width="560px">
    <el-form :model="form" label-width="90px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="登录账号" required>
            <el-input v-model="form.username" :disabled="!!form.id" placeholder="如 student09" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="姓名" required><el-input v-model="form.realName" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="学号/工号"><el-input v-model="form.userNo" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="角色" required>
            <el-select v-model="form.roleId" placeholder="请选择" style="width: 100%">
              <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属院系"><el-input v-model="form.deptName" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="职称"><el-input v-model="form.title" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">正常</el-radio>
              <el-radio :value="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="初始密码">
            <el-input v-model="form.password" :placeholder="form.id ? '留空则不修改' : '默认 123456'" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dlg = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http from '../../http'
import ListPage from '../../components/ListPage.vue'
import { imgUrl } from '../../utils/format'

const lp = ref()
const roles = ref([])
const dlg = ref(false)
const saving = ref(false)
const form = reactive({})

const filters = [
  { key: 'keyword', label: '姓名/账号/学号' },
  { key: 'roleId', type: 'select', label: '角色', options: [] }
]
const columns = [
  { prop: 'realName', label: '用户', minWidth: 190 },
  { prop: 'roleName', label: '角色', width: 120 },
  { prop: 'deptName', label: '院系', minWidth: 150 },
  { prop: 'title', label: '职称', width: 100 },
  { prop: 'phone', label: '手机号', width: 130 },
  { prop: 'status', label: '状态', width: 90 },
  { prop: 'lastLoginTime', label: '最近登录', type: 'datetime', width: 150 }
]
const actions = [
  { key: 'edit', label: '编辑' },
  { key: 'reset', label: '重置密码' }
]

// 角色下拉：筛选条与表单共用一份数据
onMounted(async () => {
  roles.value = await http.get('/system/role/list')
  filters[1].options = roles.value.map((r) => ({ label: r.roleName, value: r.id }))
})

function openDialog(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row } : { status: 1, password: '' })
  dlg.value = true
}

async function save() {
  if (!form.username || !form.realName || !form.roleId) {
    ElMessage.warning('账号、姓名、角色为必填项')
    return
  }
  saving.value = true
  try {
    if (form.id) await http.put(`/system/user/${form.id}`, { ...form })
    else await http.post('/system/user', { ...form })
    ElMessage.success('保存成功')
    dlg.value = false
    lp.value.load()
  } finally {
    saving.value = false
  }
}

async function onAction({ key, row }) {
  if (key === 'edit') return openDialog(row)
  await http.put(`/system/user/${row.id}`, { ...row, password: '123456' })
  ElMessage.success(`已将 ${row.realName} 的密码重置为 123456`)
}
</script>

<style scoped>
.av { width: 30px; height: 30px; border-radius: 50%; object-fit: cover; background: #eef2f5; }
</style>
