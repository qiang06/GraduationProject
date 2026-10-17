<script setup>
import {computed} from 'vue'

/**
 * 分类颜色圆点。
 * 颜色来自分类表；缺失或格式非法时回落到主题主色，
 * 避免出现空心点或黑点（数据库里的 color 是人为维护的，不能假定一定合法）。
 */
const props = defineProps({
    color: {type: String, default: ''},
    size: {type: Number, default: 10}
})

const dotColor = computed(() => {
    const value = (props.color || '').trim()
    return /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/.test(value) ? value : 'var(--el-color-primary)'
})
</script>

<template>
    <span
        :style="{width: size + 'px', height: size + 'px', background: dotColor}"
        aria-hidden="true"
        class="color-dot"/>
</template>

<style scoped>
.color-dot {
    display: inline-block;
    flex: none;
    border-radius: 50%;
    vertical-align: middle;
    /* 浅色圆点在白底上边界不清，加一圈极淡内描边 */
    box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.08);
}
</style>
