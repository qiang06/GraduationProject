<script setup>
import {useDark, useToggle} from '@vueuse/core'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

useDark({
    selector: 'html',
    attribute: 'class',
    valueDark: 'dark',
    valueLight: 'light'
})

useDark({
    onChanged(dark) {
        useToggle(dark)
    }
})
</script>

<template>
    <el-config-provider :locale="zhCn">
        <div>
            <router-view/>
        </div>
    </el-config-provider>
</template>

<style scoped>

</style>
