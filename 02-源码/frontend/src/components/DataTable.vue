<template>
  <div>
    <!-- 筛选条：搜索项由各页面配置，下拉宽度统一 200px -->
    <div v-if="filters.length || $slots.filters" class="filters">
      <template v-for="f in filters" :key="f.key">
        <el-select v-if="f.type === 'select'" v-model="query[f.key]" :placeholder="f.placeholder || f.label" clearable>
          <el-option v-for="o in f.options" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-date-picker
          v-else-if="f.type === 'date'"
          v-model="query[f.key]"
          type="date"
          value-format="YYYY-MM-DD"
          :placeholder="f.placeholder || f.label"
        />
        <el-input
          v-else
          v-model="query[f.key]"
          :placeholder="f.placeholder || f.label"
          clearable
          @keyup.enter="reload(1)"
        />
      </template>
      <el-button type="primary" :icon="Search" @click="reload(1)">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <div class="grow" />
      <slot name="filters" />
    </div>

    <el-table :data="rows" v-loading="loading" stripe style="width: 100%" :row-key="rowKey">
      <el-table-column
        v-for="c in columns"
        :key="c.prop"
        :prop="c.prop"
        :label="c.label"
        :width="c.width"
        :min-width="c.minWidth || 90"
        :align="c.align || 'left'"
        :fixed="c.fixed"
        :show-overflow-tooltip="c.tooltip !== false"
      >
        <template #default="{ row, $index }">
          <slot :name="`col-${c.prop}`" :row="row" :val="row[c.prop]" :index="$index">
            {{ c.formatter ? c.formatter(row[c.prop], row) : (row[c.prop] ?? '—') }}
          </slot>
        </template>
      </el-table-column>

      <el-table-column v-if="actions.length" label="操作" :width="actionWidth" fixed="right" align="right">
        <template #default="{ row }">
          <span class="ops">
            <a
              v-for="(a, i) in actions.filter((x) => !x.show || x.show(row))"
              :key="i"
              :class="['op', a.type || '']"
              @click="a.click(row)"
              >{{ typeof a.label === 'function' ? a.label(row) : a.label }}</a
            >
          </span>
        </template>
      </el-table-column>

      <template #empty>
        <div class="empty">暂无数据</div>
      </template>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @current-change="load"
      @size-change="reload(1)"
    />
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'

/**
 * 通用分页表格：后端分页（保留页码与每页条数），表头宽度按内容自适应
 * api(params) 需返回 { total, records }
 */
const props = defineProps({
  api: { type: Function, required: true },
  columns: { type: Array, required: true },
  filters: { type: Array, default: () => [] },
  actions: { type: Array, default: () => [] },
  params: { type: Object, default: () => ({}) },
  rowKey: { type: String, default: 'id' },
  actionWidth: { type: Number, default: 170 },
  auto: { type: Boolean, default: true }
})

const emit = defineEmits(['loaded'])

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, ...buildInitial() })

function buildInitial() {
  const o = {}
  props.filters.forEach((f) => (o[f.key] = f.default ?? ''))
  return o
}

async function load() {
  loading.value = true
  try {
    const payload = { ...props.params }
    Object.entries(query).forEach(([k, v]) => {
      if (v !== '' && v !== null && v !== undefined) payload[k] = v
    })
    const data = await props.api(payload)
    rows.value = data?.records || []
    total.value = Number(data?.total || 0)
    emit('loaded', data)
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function reload(page = query.page) {
  query.page = page
  load()
}

function reset() {
  Object.assign(query, { page: 1, size: query.size }, buildInitial())
  load()
}

watch(() => props.params, () => reload(1), { deep: true })

onMounted(() => {
  if (props.auto) load()
})

defineExpose({ reload, rows, total })
</script>

<style scoped>
.ops { display: inline-flex; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.op { font-size: 12.5px; cursor: pointer; color: var(--accent); white-space: nowrap; }
.op:hover { text-decoration: underline; }
.op.dan { color: var(--danger); }
.op.ok { color: var(--ok); }
</style>
