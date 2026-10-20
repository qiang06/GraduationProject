<script setup>
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ArrowLeft} from '@element-plus/icons-vue'
import LightCard from '@/components/LightCard.vue'
import {ElMessage} from 'element-plus'
import {apiCampusNoticeDetail} from '@/net/api'

const route = useRoute()
const router = useRouter()
const notice = ref(null)
const loading = ref(true)

function load() {
    loading.value = true
    apiCampusNoticeDetail(route.params.id, data => {
        notice.value = data
        loading.value = false
    }, message => {
        loading.value = false
        ElMessage.warning(message)
        router.push('/index/notices')
    })
}

function formatTime(time) {
    if (!time) return ''
    const d = new Date(time)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(load)
</script>

<template>
    <div class="notice-detail-page" v-loading="loading">
        <div class="back-bar" @click="router.push('/index/notices')">
            <el-icon><ArrowLeft/></el-icon>
            返回通知列表
        </div>
        <light-card v-if="notice">
            <div class="notice-head">
                <el-tag v-if="notice.top === 1" type="danger" size="small">置顶</el-tag>
                <el-tag size="small" effect="plain">{{ notice.category }}</el-tag>
                <h1>{{ notice.title }}</h1>
            </div>
            <div class="notice-meta">
                <span>{{ notice.publisher }}</span>
                <el-divider direction="vertical"/>
                <span>{{ formatTime(notice.publishTime) }}</span>
            </div>
            <el-divider/>
            <div class="notice-content" v-html="notice.content"></div>
        </light-card>
        <div v-else-if="!loading" class="empty">通知不存在或已被删除</div>
    </div>
</template>

<style scoped>
.notice-detail-page {
    max-width: 860px;
    margin: 20px auto;
    padding: 0 16px;
}

.back-bar {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 0 12px;
    color: grey;
    font-size: 14px;
    cursor: pointer;
}

.back-bar:hover {
    color: var(--el-color-primary);
}

.notice-head {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

.notice-head h1 {
    width: 100%;
    margin: 8px 0 0;
    font-size: 24px;
    line-height: 1.45;
    color: var(--el-text-color-primary);
}

.notice-meta {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 10px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
}

.notice-content {
    font-size: 15px;
    line-height: 1.85;
    color: var(--el-text-color-regular);
    word-break: break-word;
}

.notice-content :deep(p) {
    margin: 8px 0;
}

.notice-content :deep(img) {
    max-width: 100%;
    height: auto;
    border-radius: 4px;
}

.notice-content :deep(blockquote) {
    margin: 10px 0;
    padding: 6px 12px;
    border-left: 3px solid var(--el-border-color);
    color: var(--el-text-color-secondary);
}

.notice-content :deep(pre) {
    padding: 10px;
    overflow-x: auto;
    border-radius: 4px;
    background: var(--el-fill-color-light);
}

.notice-content :deep(ul),
.notice-content :deep(ol) {
    padding-left: 24px;
}

.empty {
    padding: 60px 0;
    text-align: center;
    color: grey;
}
</style>
