<template>
  <div>
    <div class="page-head">
      <h2>菜单管理</h2>
      <span class="sub">菜单与按钮权限点由后端下发，改动需在角色权限中重新分配</span>
      <div class="grow" />
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="card">
      <el-table :data="rows" row-key="id" default-expand-all v-loading="loading" :tree-props="{ children: 'children' }">
        <el-table-column prop="label" label="菜单名称" min-width="220">
          <template #default="{ row }">
            <span class="nm">{{ row.label }}</span>
            <span v-if="row.path" class="tsub" style="margin-left: 8px">{{ row.path }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="menuType" label="类型" width="100">
          <template #default="{ row }">
            <span class="tag" :class="row.menuType === '目录' ? 'info' : row.menuType === '按钮' ? 'warn' : ''">{{ row.menuType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="perms" label="权限标识" min-width="180">
          <template #default="{ row }">
            <code v-if="row.perms" class="perm">{{ row.perms }}</code>
            <span v-else class="tsub">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="80" />
        <el-table-column label="层级" width="90">
          <template #default="{ row }">{{ row.children && row.children.length ? '目录（' + row.children.length + '）' : '页面' }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import http from '../../http'

const rows = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = await http.get('/system/menu/tree')
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.nm { font-weight: 500; }
.perm { background: #f5f8fa; border: 1px solid var(--line); border-radius: 3px; padding: 0 5px; font-size: 11.5px; color: var(--primary-2); }
</style>
