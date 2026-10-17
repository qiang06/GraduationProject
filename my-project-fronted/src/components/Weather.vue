<script setup>
import {Warning} from '@element-plus/icons-vue'

defineProps({
    data: Object,
    failed: Boolean
})

const emit = defineEmits(['retry'])
</script>

<template>
    <div style="height: 160px" v-loading="!data.success && !failed"
         element-loading-text="正在加载天气信息...">
        <div v-if="failed" class="weather-failed">
            <el-icon>
                <Warning/>
            </el-icon>
            <div class="weather-failed-text">{{ data.error || '天气信息加载失败' }}</div>
            <el-button size="small" @click="emit('retry')">重试</el-button>
        </div>
        <template v-else-if="data.success">
            <div style="display: flex;justify-content: space-between;margin: 10px 20px">
                <div style="font-size: 45px;color: grey">
                    <i :class="`qi-${data.now.icon}-fill`"></i>
                </div>
                <div style="font-weight: bold;text-align: center">
                    <div style="font-size: 25px">{{data.now.temp}}℃</div>
                    <div style="font-size: 15px">{{data.now.text}}</div>
                </div>
                <div style="margin-top: 13px">
                    <div style="font-size: 15px">{{`${data.location.country} ${data.location.adm1}`}}</div>
                    <div style="font-size: 14px;color: grey">{{`${data.location.adm2} ${data.location.name}区`}}</div>
                </div>
            </div>
            <el-divider style="margin: 10px 0"/>
            <div style="display: grid;grid-template-columns: repeat(5, 1fr);text-align: center">
                <div v-for="item in data.hourly">
                    <div style="font-size: 13px">{{new Date(item.fxTime).getHours()}} 时</div>
                    <div style="font-size: 23px">
                        <i :class="`qi-${item.icon}-fill`"></i>
                    </div>
                    <div style="font-size: 12px">{{item.temp}}℃</div>
                </div>
            </div>
        </template>
    </div>
</template>

<style scoped>
.weather-failed {
    height: 140px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: var(--el-text-color-secondary);
}

.weather-failed .el-icon {
    font-size: 24px;
    color: var(--el-color-warning);
}

.weather-failed-text {
    font-size: 13px;
}
</style>
