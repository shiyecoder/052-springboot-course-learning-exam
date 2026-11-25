<template>
  <div class="app">
    <!-- 220px 侧栏：菜单由后端按角色下发，父子两级 -->
    <aside class="sidebar">
      <div class="brand"><span class="dot" />在线课程学习与考试</div>
      <div class="menu">
        <template v-for="m in menus" :key="m.id">
          <a v-if="!m.children || !m.children.length" :href="'#' + m.path" :class="{ on: isOn(m.path) }">
            <span class="ico"><el-icon v-if="menuIcon(m.icon)"><component :is="menuIcon(m.icon)" /></el-icon><i v-else>{{ m.name.slice(0, 1) }}</i></span>{{ m.name }}
          </a>
          <template v-else>
            <div class="group">{{ m.name }}</div>
            <a v-for="c in m.children" :key="c.id" :href="'#' + c.path" :class="{ on: isOn(c.path) }">
              <span class="ico"><el-icon v-if="menuIcon(c.icon)"><component :is="menuIcon(c.icon)" /></el-icon><i v-else>{{ c.name.slice(0, 1) }}</i></span>{{ c.name }}
            </a>
          </template>
        </template>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="crumb">
          <b>{{ currentName }}</b>
          <span v-if="currentGroup"> / {{ currentGroup }}</span>
        </div>
        <div class="grow" />
        <div class="icon-btn" title="消息中心" @click="$router.push('/message')">
          <el-icon><Bell /></el-icon>
          <span v-if="store.unread" class="badge">{{ store.unread > 99 ? '99+' : store.unread }}</span>
        </div>
        <!-- 退出登录：trigger=click，避免 hover 触发器在自动化/触屏下打不开 -->
        <el-dropdown trigger="click" @command="onCommand">
          <div class="user">
            <img :src="store.user.avatar || '/uploads/seed/avatar/01.jpg'" alt="头像" />
            <div class="nm">
              <b>{{ store.user.realName }}</b>
              <span>{{ store.user.roleName || roleText }}</span>
            </div>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="message">消息中心</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <section class="content">
        <div class="content-inner">
          <router-view />
        </div>
      </section>
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

const menus = computed(() => (store.menus || []).filter((m) => m.menuType !== '按钮'))
const roleText = computed(() => ({ ADMIN: '系统管理员', JWADMIN: '教务管理员', TEACHER: '任课教师', ASSISTANT: '助教' }[store.user.roleCode] || ''))

const isOn = (path) => route.path === path || (path !== '/dashboard' && route.path.startsWith(path + '/'))

const flat = computed(() => {
  const arr = []
  for (const m of menus.value) {
    arr.push(m)
    for (const c of m.children || []) arr.push(c)
  }
  return arr
})
const currentName = computed(() => [...flat.value].sort((a, b) => (b.path || '').length - (a.path || '').length)
  .find((m) => isOn(m.path))?.name || '工作台')
const currentGroup = computed(() => {
  const hit = flat.value.find((m) => isOn(m.path))
  const parent = menus.value.find((m) => (m.children || []).some((c) => c.id === hit?.id))
  return parent && parent.id !== hit?.id ? parent.name : ''
})

// 菜单图标：后端存 Element Plus 图标名，白名单映射，缺失时退化为首字

function onCommand(cmd) {
  if (cmd === 'logout') {
    logout()
    router.replace('/login')
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'message') {
    router.push('/message')
  }
}

onMounted(() => {
  refreshMe()
  loadUnread()
})
</script>
