import { createRouter, createWebHashHistory } from 'vue-router'
import { isLogin, store } from './store'

/**
 * 路由表：后台（管理员 / 教务 / 教师 / 助教）走 AdminLayout，学生走 PortalLayout。
 * 菜单由后端按角色下发，前端只做「路径 → 组件」映射，避免前端写死可见范围。
 */
const adminPages = {
  '/dashboard': () => import('./views/dashboard/Index.vue'),
  '/course/list': () => import('./views/course/List.vue'),
  '/course/audit': () => import('./views/course/Audit.vue'),
  '/course/chapter': () => import('./views/course/Chapter.vue'),
  '/course/class': () => import('./views/course/Class.vue'),
  '/question': () => import('./views/question/Index.vue'),
  '/exam/list': () => import('./views/exam/List.vue'),
  '/exam/grading': () => import('./views/exam/Grading.vue'),
  '/exam/score': () => import('./views/exam/Score.vue'),
  '/risk/warning': () => import('./views/risk/Warning.vue'),
  '/risk/rule': () => import('./views/risk/Rule.vue'),
  '/statistics': () => import('./views/statistics/Index.vue'),
  '/discussion': () => import('./views/discussion/Index.vue'),
  '/message': () => import('./views/message/Index.vue'),
  '/system/user': () => import('./views/system/User.vue'),
  '/system/role': () => import('./views/system/Role.vue'),
  '/system/menu': () => import('./views/system/Menu.vue'),
  '/system/dict': () => import('./views/system/Dict.vue'),
  '/system/log': () => import('./views/system/Log.vue'),
  '/profile': () => import('./views/profile/Index.vue')
}

const adminRoutes = Object.entries(adminPages).map(([path, comp]) => ({
  path,
  component: () => import('./layouts/AdminLayout.vue'),
  children: [{ path: '', component: comp }]
}))

const routes = [
  { path: '/login', component: () => import('./views/Login.vue') },
  ...adminRoutes,
  {
    path: '/portal',
    component: () => import('./layouts/PortalLayout.vue'),
    children: [
      { path: '', component: () => import('./views/portal/Home.vue') },
      { path: 'enroll', component: () => import('./views/portal/Enroll.vue') },
      { path: 'my-course', component: () => import('./views/portal/MyCourse.vue') },
      { path: 'course/:id', component: () => import('./views/portal/CourseLearn.vue') },
      { path: 'exam/:id', component: () => import('./views/portal/ExamTaking.vue') },
      { path: 'score', component: () => import('./views/portal/Score.vue') },
      { path: 'warning', component: () => import('./views/portal/Warning.vue') },
      { path: 'message', component: () => import('./views/message/Index.vue') },
      { path: 'profile', component: () => import('./views/profile/Index.vue') }
    ]
  },
  { path: '/', redirect: () => (store.user?.roleCode === 'STUDENT' ? '/portal' : '/dashboard') },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHashHistory(), routes })

// 路由守卫：未登录一律回登录页；学生访问后台路径时转到门户
router.beforeEach((to) => {
  if (to.path === '/login') return true
  if (!isLogin()) return { path: '/login', query: { redirect: to.fullPath } }
  const student = store.user?.roleCode === 'STUDENT'
  if (student && !to.path.startsWith('/portal')) return { path: '/portal' }
  if (!student && to.path.startsWith('/portal')) return { path: '/dashboard' }
  return true
})

export default router
