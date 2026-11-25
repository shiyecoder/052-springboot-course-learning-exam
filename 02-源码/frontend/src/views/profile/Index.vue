<template>
  <div>
    <div class="page-head">
      <h2>个人中心</h2>
      <span class="sub">资料修改后立即生效，密码修改需重新登录</span>
    </div>

    <div class="two">
      <div class="card profile">
        <img :src="imgUrl(user.avatar)" class="big" alt="头像" />
        <div class="nm">{{ user.realName }}</div>
        <div class="tsub">{{ roleName }}</div>
        <div class="rows">
          <div><span>账号</span><b>{{ user.username }}</b></div>
          <div><span>学号/工号</span><b>{{ user.userNo || '—' }}</b></div>
          <div><span>院系</span><b>{{ user.deptName || '—' }}</b></div>
          <div><span>职称</span><b>{{ user.title || '—' }}</b></div>
          <div><span>信用分</span><b>{{ user.creditScore ?? 100 }}</b></div>
          <div><span>最近登录</span><b>{{ fmtDateTime(user.lastLoginTime) }}</b></div>
        </div>
      </div>

      <div class="card" style="flex: 1">
        <h3>修改资料</h3>
        <el-form :model="form" label-width="80px" style="max-width: 460px">
          <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio value="男">男</el-radio>
              <el-radio value="女">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="头像地址">
            <el-input v-model="form.avatar" placeholder="/uploads/seed/avatar/01.jpg" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>

        <h3 style="margin-top: 16px">修改密码</h3>
        <el-form :model="pwd" label-width="80px" style="max-width: 460px">
          <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password placeholder="至少 6 位" /></el-form-item>
          <el-form-item label="确认新密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
          <el-form-item>
            <el-button :loading="changing" @click="changePwd">修改密码</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../http'
import { store, refreshMe, logout } from '../../store'
import { fmtDateTime, imgUrl } from '../../utils/format'

const user = ref({})
const saving = ref(false)
const changing = ref(false)
const form = reactive({ realName: '', phone: '', email: '', gender: '男', avatar: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const roleName = computed(() => store.user.roleName || store.user.roleCode || '')

onMounted(async () => {
  const d = await http.get('/auth/me')
  user.value = d.user || {}
  Object.assign(form, {
    realName: user.value.realName,
    phone: user.value.phone,
    email: user.value.email,
    gender: user.value.gender || '男',
    avatar: user.value.avatar
  })
})

async function saveProfile() {
  if (!form.realName) return ElMessage.warning('姓名不能为空')
  saving.value = true
  try {
    await http.put('/auth/profile', { ...form })
    ElMessage.success('资料已更新')
    user.value = { ...user.value, ...form }
    refreshMe()
  } finally {
    saving.value = false
  }
}

async function changePwd() {
  if (!pwd.oldPassword || pwd.newPassword.length < 6) return ElMessage.warning('原密码必填，新密码至少 6 位')
  if (pwd.newPassword !== pwd.confirm) return ElMessage.warning('两次输入的新密码不一致')
  changing.value = true
  try {
    await http.post('/auth/password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    setTimeout(() => {
      logout()
      location.hash = '#/login'
    }, 800)
  } finally {
    changing.value = false
  }
}
</script>

<style scoped>
.two { display: flex; gap: 12px; align-items: flex-start; }
.profile { width: 300px; flex: 0 0 300px; text-align: center; padding: 20px 16px; }
.profile .big { width: 76px; height: 76px; border-radius: 50%; object-fit: cover; background: #eef2f5; }
.profile .nm { font-size: 15px; font-weight: 600; margin-top: 10px; }
.profile .rows { margin-top: 14px; text-align: left; border-top: 1px solid var(--line); padding-top: 10px; }
.profile .rows div { display: flex; justify-content: space-between; padding: 5px 0; font-size: 12.5px; }
.profile .rows span { color: var(--text-2); }
</style>
