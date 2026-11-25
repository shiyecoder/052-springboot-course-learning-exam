<template>
  <div class="portal">
    <header class="portal-top">
      <div class="inner">
        <div class="logo"><span class="dot" />在线课程学习</div>
        <nav>
          <a v-for="m in navs" :key="m.id" :href="'#' + m.path" :class="{ on: isOn(m.path) }">
            <el-icon v-if="menuIcon(m.icon)"><component :is="menuIcon(m.icon)" /></el-icon>{{ m.name }}
          </a>
        </nav>
        <div class="icon-btn" style="position: relative; cursor: pointer" @click="$router.push('/portal/message')">
          <el-icon><Bell /></el-icon>
          <span v-if="store.unread" class="badge">{{ store.unread }}</span>
        </div>
        <el-dropdown trigger="click" @command="onCommand">
          <div class="user">
            <img :src="store.user.avatar || '/uploads/seed/avatar/05.jpg'" alt="头像" />
            <div class="nm">
              <b>{{ store.user.realName }}</b>
              <span>{{ store.user.deptName || '学生' }}</span>
            </div>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="score">我的成绩</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <div class="portal-body">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { store, logout, refreshMe, loadUnread } from '../store'
import { menuIcon } from '../utils/icons'

const route = useRoute()
const router = useRouter()

// 门户只展示后端下发的学生菜单（id >= 31）
const navs = computed(() => (store.menus || []).filter((m) => m.menuType !== '按钮'))
const isOn = (path) => route.path === path || (path !== '/portal' && route.path.startsWith(path))

function onCommand(cmd) {
  if (cmd === 'logout') {
    logout()
    router.replace('/login')
  } else if (cmd === 'profile') {
    router.push('/portal/profile')
  } else if (cmd === 'score') {
    router.push('/portal/score')
  }
}

onMounted(() => {
  refreshMe()
  loadUnread()
})
</script>
