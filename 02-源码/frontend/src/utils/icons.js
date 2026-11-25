/**
 * 菜单图标：后端 sys_menu.icon 存的是 Element Plus 图标名，
 * 这里做一层白名单映射，避免图标名拼错或组件不存在时整块菜单空白。
 */
import {
  Odometer, Reading, List, Stamp, Document, School, Notebook, AlarmClock, EditPen, Medal,
  WarningFilled, SetUp, DataAnalysis, ChatDotRound, Bell, Setting, User, Avatar, Menu,
  Collection, Tickets, UserFilled, HomeFilled, Search, Files, TrendCharts
} from '@element-plus/icons-vue'

const MAP = {
  Odometer, Reading, List, Stamp, Document, School, Notebook, AlarmClock, EditPen, Medal,
  WarningFilled, SetUp, DataAnalysis, ChatDotRound, Bell, Setting, User, Avatar, Menu,
  Collection, Tickets, UserFilled, HomeFilled, Search, Files, TrendCharts
}

export function menuIcon(name) {
  return MAP[name] || null
}
