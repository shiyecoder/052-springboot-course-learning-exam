<template>
  <div ref="el" :style="{ height: height + 'px' }" class="chart-box"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

// ECharts 容器：父组件传入 option，数据变化自动重绘，窗口/容器尺寸变化自动 resize
const props = defineProps({
  option: { type: Object, required: true },
  height: { type: Number, default: 180 }
})

const el = ref(null)
let ins = null
let ro = null

function draw() {
  if (!el.value) return
  if (!ins) ins = echarts.init(el.value, null, { renderer: 'canvas' })
  ins.setOption(props.option, true)
}

onMounted(async () => {
  await nextTick()
  draw()
  ro = new ResizeObserver(() => ins && ins.resize())
  ro.observe(el.value)
})

watch(() => props.option, () => draw(), { deep: true })

onBeforeUnmount(() => {
  if (ro) ro.disconnect()
  if (ins) { ins.dispose(); ins = null }
})
</script>

<style scoped>
.chart-box { width: 100%; }
</style>
