<template>
  <div>
    <div class="page-head">
      <h2>{{ title }}</h2>
      <span class="sub">{{ sub }}</span>
      <div class="grow" />
      <slot name="head" />
    </div>

    <div class="card">
      <!-- 筛选条：下拉宽度统一 200px（设计规范 10.3） -->
      <div class="filters">
        <template v-for="f in filters" :key="f.key">
          <el-select v-if="f.type === 'select'" v-model="query[f.key]" :placeholder="f.label" clearable>
            <el-option v-for="o in f.options" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-date-picker
            v-else-if="f.type === 'date'"
            v-model="query[f.key]"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="f.label"
          />
          <el-input v-else v-model="query[f.key]" :placeholder="f.label" clearable @keyup.enter="load(1)" />
        </template>
        <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
        <div class="grow" />
        <slot name="filters" :reload="load" />
      </div>

      <el-table :data="rows" v-loading="loading" stripe style="width: 100%" :empty-text="emptyText">
        <el-table-column
          v-for="c in columns"
          :key="c.prop"
          :prop="c.prop"
          :label="c.label"
          :width="c.width"
          :min-width="c.minWidth || 90"
          :align="c.align || 'left'"
          :fixed="c.fixed"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <slot :name="'cell-' + c.prop" :row="row" :val="row[c.prop]">
              <!-- 图片列：统一走本地 uploads，空值给占位块（不出现破图） -->
              <img v-if="c.type === 'image'" :src="row[c.prop]" class="thumb" alt="" />
              <template v-else-if="c.type === 'name'">
                <div class="tname">{{ row[c.prop] }}</div>
                <div class="tsub" v-if="c.subProp">{{ row[c.subProp] }}</div>
              </template>
              <span v-else-if="c.type === 'tag'" class="tag" :class="tagType(row[c.prop])">{{ row[c.prop] || '—' }}</span>
              <template v-else-if="c.type === 'cap'">
                <span>{{ row.enrollCount ?? 0 }} / {{ row.capacity ?? 0 }}</span>
              </template>
              <template v-else-if="c.type === 'progress'">
                <div style="display: flex; align-items: center; gap: 6px">
                  <div class="progress" style="flex: 1"><i :style="{ width: Math.min(100, Number(row[c.prop] || 0)) + '%' }" /></div>
                  <span class="tsub">{{ Number(row[c.prop] || 0).toFixed(0) }}%</span>
                </div>
              </template>
              <span v-else-if="c.type === 'datetime'">{{ fmtDateTime(row[c.prop]) }}</span>
              <span v-else-if="c.type === 'date'">{{ fmtDate(row[c.prop]) }}</span>
              <span v-else-if="c.type === 'percent'">{{ Number(row[c.prop] || 0).toFixed(1) }}%</span>
              <span v-else>{{ row[c.prop] ?? '—' }}</span>
            </slot>
          </template>
        </el-table-column>

        <el-table-column v-if="visibleActions.length" label="操作" :width="opWidth" fixed="right" align="right">
          <template #default="{ row }">
            <span class="ops">
              <a
                v-for="(a, i) in visibleActions"
                :key="i"
                :class="['op', a.danger ? 'dan' : '']"
                @click="emitAction(a, row)"
                >{{ a.label }}</a
              >
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="load(1)"
          @current-change="load()"
        />
      </div>
    </div>

    <slot name="footer" />
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import http from '../http'
import { fmtDate, fmtDateTime, tagType } from '../utils/format'

/**
 * 通用列表页：筛选 + 后端分页表格 + 行内操作。
 * 各页面只传配置与插槽，避免每个列表重复实现分页、筛选、状态标签。
 * 注意：后端分页（page/size/total 由后端返回），前端不一次性拉全量。
 */
const props = defineProps({
  title: { type: String, default: '' },
  sub: { type: String, default: '' },
  api: { type: String, required: true },
  filters: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  actions: { type: Array, default: () => [] },
  params: { type: Object, default: () => ({}) },
  emptyText: { type: String, default: '暂无数据' },
  size0: { type: Number, default: 10 }
})

const emit = defineEmits(['action', 'loaded'])

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(props.size0)
const loading = ref(false)
const query = reactive({})

props.filters.forEach((f) => {
  query[f.key] = f.default ?? ''
})

const visibleActions = computed(() => props.actions.filter(() => true))
const opWidth = computed(() => Math.max(90, visibleActions.value.length * 62))

async function load(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const params = { page: page.value, size: size.value, ...props.params }
    Object.entries(query).forEach(([k, v]) => {
      if (v !== '' && v !== null && v !== undefined) params[k] = v
    })
    const d = await http.get(props.api, { params })
    rows.value = d.records || []
    total.value = d.total || 0
    emit('loaded', d)
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.keys(query).forEach((k) => (query[k] = ''))
  load(1)
}

function emitAction(a, row) {
  if (a.type === 'emit') emit('action', { key: a.key, row })
  else if (typeof a.click === 'function') a.click(row)
}

defineExpose({ load, rows, total, query })
onMounted(() => load(1))
</script>

<style scoped>
/* 缩略图尺寸样式在 style.css 全局定义（插槽内容不走 scoped） */
</style>
