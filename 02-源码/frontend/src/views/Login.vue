<template>
  <div class="login-page">
    <!-- 左：品牌与价值说明；背景取本地课程实景图，断网也能显示 -->
    <div class="login-left">
      <h1>在线课程<br />学习与考试</h1>
      <p>从开课审核、章节学习、在线考试到成绩发布与学习预警，一套系统覆盖一门课的全过程。</p>
      <div class="feats">
        <div class="feat"><i>◆</i> 章节课时与学习进度自动同步，快进与挂机自动留痕</div>
        <div class="feat"><i>◆</i> 组卷、考试、防作弊与主观题批阅形成完整闭环</div>
        <div class="feat"><i>◆</i> 五项因子加权算出挂科风险，规则可配、结果可追溯</div>
      </div>
    </div>
    <div class="login-right">
      <div class="login-form">
        <h2>账号登录</h2>
        <div class="sub">使用学号 / 工号对应账号登录，教师与教务进入管理后台</div>
        <el-form :model="form" @keyup.enter="submit">
          <div class="row">
            <el-input v-model="form.username" placeholder="登录账号" size="large" :prefix-icon="User" />
          </div>
          <div class="row">
            <el-input v-model="form.password" type="password" placeholder="登录密码" size="large" show-password :prefix-icon="Lock" />
          </div>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">登 录</el-button>
        </el-form>
        <div class="acc">
          <span v-for="a in accounts" :key="a.u" @click="fill(a)">{{ a.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '../store'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

// 演示账号：答辩现场一键切换角色
const accounts = [
  { label: '系统管理员 admin', u: 'admin', p: 'admin123' },
  { label: '教务 jwadmin', u: 'jwadmin', p: '123456' },
  { label: '教师 teacher01', u: 'teacher01', p: '123456' },
  { label: '助教 assistant01', u: 'assistant01', p: '123456' },
  { label: '学生 student03', u: 'student03', p: '123456' }
]

const fill = (a) => Object.assign(form, { username: a.u, password: a.p })

async function submit() {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号与密码')
  loading.value = true
  try {
    const d = await login(form.username, form.password)
    ElMessage.success(`欢迎回来，${d.realName}`)
    router.replace(d.roleCode === 'STUDENT' ? '/portal' : '/dashboard')
  } catch (e) {
    /* 拦截器已统一提示 */
  } finally {
    loading.value = false
  }
}
</script>
