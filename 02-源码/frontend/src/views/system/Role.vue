<template>
  <div>
    <div class="page-head">
      <h2>角色权限</h2>
      <span class="sub">左侧选择角色，右侧勾选菜单并保存；菜单权限即时生效</span>
    </div>

    <div class="split">
      <div class="card roles">
        <h3>角色列表</h3>
        <div
          v-for="r in roles"
          :key="r.id"
          :class="['role', cur && cur.id === r.id ? 'on' : '']"
          @click="pick(r)"
        >
          <div class="tname">{{ r.roleName }}</div>
          <div class="tsub">{{ r.roleCode }}<span v-if="r.remark"> · {{ r.remark }}</span></div>
        </div>
      </div>

      <div class="card" style="flex: 1">
        <h3>
          菜单权限
          <span class="tip">{{ cur ? '当前角色：' + cur.roleName : '请先选择左侧角色' }}</span>
          <div class="grow" />
          <el-button size="small" @click="checkAll(true)">全选</el-button>
          <el-button size="small" @click="checkAll(false)">清空</el-button>
          <el-button size="small" type="primary" :loading="saving" :disabled="!cur" @click="save">保存</el-button>
        </h3>
        <el-tree
          ref="treeComp"
          :data="tree"
          node-key="id"
          show-checkbox
          default-expand-all
          :props="{ label: 'label', children: 'children' }"
          style="margin-top: 6px"
        >
          <template #default="{ data }">
            <span class="node">
              <b>{{ data.label }}</b>
              <span class="tag" :class="data.menuType === '目录' ? 'info' : ''">{{ data.menuType }}</span>
              <span class="tsub" v-if="data.perms">{{ data.perms }}</span>
            </span>
          </template>
        </el-tree>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../http'
import { store } from '../../store'

const roles = ref([])
const tree = ref([])
const cur = ref(null)
const checked = ref([])
const treeComp = ref()
const saving = ref(false)

const treeEl = () => treeComp.value

onMounted(async () => {
  const [rs, ts] = await Promise.all([http.get('/system/role/list'), http.get('/system/menu/tree')])
  roles.value = rs
  tree.value = ts
  if (rs.length) pick(rs[0])
})

async function pick(r) {
  cur.value = r
  checked.value = await http.get(`/system/role/${r.id}/menus`)
  await nextTick()
  treeEl()?.setCheckedKeys(checked.value, false)
}

function checkAll(on) {
  treeEl()?.setCheckedKeys(on ? allIds(tree.value) : [], false)
}

function allIds(nodes) {
  return nodes.flatMap((n) => [n.id, ...(n.children ? allIds(n.children) : [])])
}

async function save() {
  saving.value = true
  try {
    // 半选父节点也要提交，否则只勾子菜单会导致父目录不下发
    const ids = [...treeEl().getCheckedKeys(), ...treeEl().getHalfCheckedKeys()]
    await http.post(`/system/role/${cur.value.id}/menus`, { menuIds: ids })
    ElMessage.success('菜单权限已保存，重新登录后生效')
    // 改的是自己角色时刷新本地菜单，避免当场看不到变化
    if (cur.value.roleCode === store.user.roleCode) location.reload()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.split { display: flex; gap: 12px; align-items: flex-start; }
.roles { width: 260px; flex: 0 0 260px; padding: 14px 10px; }
.roles h3 { padding: 0 6px; }
.role { padding: 9px 10px; border-radius: 6px; cursor: pointer; border: 1px solid transparent; }
.role:hover { background: #f5f8fa; }
.role.on { background: #eaf2f8; border-color: #cfe1ee; }
.card h3 { display: flex; align-items: center; gap: 8px; }
.card h3 .grow { flex: 1; }
.node { display: flex; align-items: center; gap: 8px; }
.node b { font-weight: 500; }
</style>
