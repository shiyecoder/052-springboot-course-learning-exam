<template>
  <span :class="['tag', tagType(code)]">{{ code || '—' }}</span>
</template>

<script setup>
import { tagType } from '../utils/format'

// 状态标签：颜色只作辅助，文字始终显示中文状态值（设计规范：状态不能只靠颜色表达）
defineProps({ code: { type: String, default: '' } })
</script>
