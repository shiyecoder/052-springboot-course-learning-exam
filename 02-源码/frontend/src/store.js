import { reactive } from 'vue'
import http from './http'

// 全局登录态：token 存 localStorage（键 ce_*），菜单由后端按角色下发
const saved = JSON.parse(localStorage.getItem('ce_user') || 'null')
const savedMenus = JSON.parse(localStorage.getItem('ce_menus') || '[]')

export const store = reactive({
  token: localStorage.getItem('ce_token') || '',
  user: saved || {},
  menus: savedMenus,
  unread: 0
})

export function isLogin() {
  // 以 localStorage 为准：刷新页面、外部注入 token 的场景都能正确判定
  if (!store.token) store.token = localStorage.getItem('ce_token') || ''
  return !!store.token
}

export async function login(username, password) {
  const d = await http.post('/auth/login', { username, password })
  store.token = d.token
  store.user = {
    userId: d.userId,
    username: d.username,
    realName: d.realName,
    avatar: d.avatar,
    roleCode: d.roleCode,
    roleName: d.roleName,
    deptName: d.deptName
  }
  store.menus = d.menus || []
  store.unread = d.unread || 0
  localStorage.setItem('ce_token', d.token)
  localStorage.setItem('ce_user', JSON.stringify(store.user))
  localStorage.setItem('ce_menus', JSON.stringify(store.menus))
  return d
}

export async function refreshMe() {
  if (!store.token) return
  const d = await http.get('/auth/me')
  if (d.user) {
    store.user = { ...store.user, ...d.user, roleCode: d.roleCode }
    localStorage.setItem('ce_user', JSON.stringify(store.user))
  }
  store.unread = d.unread || 0
}

export async function loadUnread() {
  if (!store.token) return
  try {
    const d = await http.get('/auth/me')
    store.unread = d.unread || 0
  } catch (e) {
    store.unread = 0
  }
}

export function logout() {
  http.post('/auth/logout').catch(() => {})
  store.token = ''
  store.user = {}
  store.menus = []
  localStorage.removeItem('ce_token')
  localStorage.removeItem('ce_user')
  localStorage.removeItem('ce_menus')
}

export const roleCode = () => store.user?.roleCode || ''
export const isAdmin = () => roleCode() === 'ADMIN'
export const isJw = () => roleCode() === 'JWADMIN'
export const isTeacher = () => roleCode() === 'TEACHER'
export const isAssistant = () => roleCode() === 'ASSISTANT'
export const isStudent = () => roleCode() === 'STUDENT'
// 教学人员可进阅卷台，学生只走门户
export const isTeaching = () => isTeacher() || isAssistant()
