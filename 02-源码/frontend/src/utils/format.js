/**
 * 全局格式化函数（全系统统一调用，禁止各页面自己拼时间/金额）
 */
const pad = (n) => String(n).padStart(2, '0')

function toDate(v) {
  if (!v) return null
  if (v instanceof Date) return v
  if (typeof v === 'number') return new Date(v)
  const s = String(v).replace('T', ' ')
  const d = new Date(s.replace(/-/g, '/'))
  return isNaN(d.getTime()) ? null : d
}

/** 日期：2026-11-01 */
export function fmtDate(v) {
  const d = toDate(v)
  if (!d) return '—'
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 日期时间：2026-11-01 13:00 */
export function fmtDateTime(v) {
  const d = toDate(v)
  if (!d) return '—'
  return `${fmtDate(d)} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 时分：13:00 */
export function fmtTime(v) {
  const d = toDate(v)
  if (!d) return '—'
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 月日时分：11-01 13:00 */
export function fmtShort(v) {
  const d = toDate(v)
  if (!d) return '—'
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 相对时间 */
export function fmtFromNow(v) {
  const d = toDate(v)
  if (!d) return '—'
  const diff = Date.now() - d.getTime()
  if (diff < 0) return fmtDateTime(d)
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`
  if (diff < 30 * 86_400_000) return `${Math.floor(diff / 86_400_000)} 天前`
  return fmtDate(d)
}

/** 秒 → 00:00 */
export function fmtDuration(sec) {
  const s = Math.max(0, Math.floor(Number(sec) || 0))
  return `${pad(Math.floor(s / 60))}:${pad(s % 60)}`
}

/** 学时：2 小时 30 分 */
export function fmtMinutes(min) {
  const m = Number(min) || 0
  if (m < 60) return `${m} 分钟`
  return `${Math.floor(m / 60)} 小时 ${m % 60} 分`
}

export function fmtNum(v, digits = 0) {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

export function fmtPercent(v, digits = 1) {
  return `${Number(v || 0).toFixed(digits)}%`
}

/* ===== 字典（与 sys_dict_data 一致，全部中文值）===== */
export const DICT = {
  course_status: { 草稿: '草稿', 待审核: '待审核', 已发布: '已发布', 已结课: '已结课', 已下架: '已下架' },
  course_type: { 必修: '必修', 选修: '选修', 公选: '公选', 重修: '重修' },
  node_type: { 视频: '视频', 文档: '文档', 测验: '测验', 讨论: '讨论' },
  exam_type: { 章节测验: '章节测验', 期中考试: '期中考试', 期末考试: '期末考试', 补考: '补考' },
  exam_status: { 待审核: '待审核', 待开始: '待开始', 进行中: '进行中', 待批阅: '待批阅', 已出分: '已出分', 已归档: '已归档' },
  question_type: { 单选题: '单选题', 多选题: '多选题', 判断题: '判断题', 填空题: '填空题', 简答题: '简答题' },
  risk_level: { 低风险: '低风险', 关注: '关注', 中风险: '中风险', 高风险: '高风险' },
  enroll_status: { 学习中: '学习中', 已完成: '已完成', 已退课: '已退课' },
  record_status: { 作答中: '作答中', 待批阅: '待批阅', 已出分: '已出分' },
  cheat_type: { 切屏: '切屏', 退出全屏: '退出全屏', 多端登录: '多端登录', 超时未操作: '超时未操作' },
  term_status: { 未开始: '未开始', 进行中: '进行中', 已结束: '已结束' }
}

export function dictLabel(type, code) {
  if (!code) return '—'
  return DICT[type]?.[code] || code
}

/** 状态语义 → 标签样式：正常/通过 ok，待处理 warn，异常/驳回 dan，进行中 info */
const OK = ['已发布', '已结课', '已出分', '已完成', '已归档', '有效', '正常', '低风险', '已通过', '已解决']
const WARN = ['待审核', '待开始', '待批阅', '关注', '中风险', '草稿']
const DAN = ['已下架', '已退课', '高风险', '驳回', '异常']
const INFO = ['进行中', '学习中', '作答中', '启用']
const RANK = { 低风险: 'ok', 关注: 'info', 中风险: 'warn', 高风险: 'dan' }

export function tagType(code) {
  if (RANK[code]) return RANK[code]
  if (OK.includes(code)) return 'ok'
  if (WARN.includes(code)) return 'warn'
  if (DAN.includes(code)) return 'dan'
  if (INFO.includes(code)) return 'info'
  return ''
}

/** 统一图片地址：后端存相对路径，直接可用；空值返回占位色块 */
export function imgUrl(v) {
  if (!v) return ''
  if (String(v).startsWith('http')) return v
  return String(v)
}
