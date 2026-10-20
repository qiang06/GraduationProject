<script setup>
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import axios from 'axios'
import {ArrowLeft} from '@element-plus/icons-vue'
import LightCard from '@/components/LightCard.vue'
import {ElMessage} from 'element-plus'
import {apiCampusLibraryDetail} from '@/net/api'

const route = useRoute()
const router = useRouter()
const resource = ref(null)
const loading = ref(true)

function load() {
    loading.value = true
    apiCampusLibraryDetail(route.params.id, data => {
        resource.value = data
        loading.value = false
    }, message => {
        loading.value = false
        ElMessage.warning(message)
        router.push('/index/library')
    })
}

function coverUrl(path) {
    if (!path) return ''
    if (/^https?:\/\//i.test(path)) return path
    const base = axios.defaults.baseURL || ''
    return path.startsWith('/images/') ? base + path : base + '/images' + path
}

function openResource() {
    if (!resource.value?.url) {
        ElMessage.warning('该资源暂时没有阅读链接')
        return
    }
    window.open(resource.value.url, '_blank', 'noopener,noreferrer')
}

onMounted(load)
</script>

<template>
    <div class="library-detail-page" v-loading="loading">
        <div class="back-bar" @click="router.push('/index/library')">
            <el-icon><ArrowLeft/></el-icon>
            返回图书馆
        </div>
        <light-card v-if="resource">
            <div class="detail-body">
                <div class="cover">
                    <img v-if="resource.cover" :src="coverUrl(resource.cover)" alt=""/>
                    <div v-else class="cover-fallback">{{ resource.title?.charAt(0) }}</div>
                </div>
                <div class="info">
                    <h1>{{ resource.title }}</h1>
                    <div class="meta">
                        <span>{{ resource.author || '佚名' }}</span>
                        <el-tag size="small" effect="plain">{{ resource.category }}</el-tag>
                    </div>
                    <div class="description">{{ resource.description || '暂无简介' }}</div>
                    <el-button type="primary" :disabled="!resource.url" @click="openResource">
                        去阅读
                    </el-button>
                </div>
            </div>
        </light-card>
        <div v-else-if="!loading" class="empty">资源不存在或已被删除</div>
    </div>
</template>

<style scoped>
.library-detail-page {
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

.detail-body {
    display: flex;
    gap: 24px;
}

.cover {
    width: 210px;
    height: 280px;
    flex: none;
    overflow: hidden;
    border-radius: 8px;
    background: var(--el-fill-color-lighter);
}

.cover img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
}

.cover-fallback {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 64px;
    font-weight: 700;
    color: #fff;
    background: linear-gradient(135deg, #409eff, #9b59b6);
}

.info {
    flex: 1;
    min-width: 0;
}

.info h1 {
    margin: 0;
    font-size: 24px;
    line-height: 1.4;
    color: var(--el-text-color-primary);
}

.meta {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: 10px;
    font-size: 14px;
    color: var(--el-text-color-secondary);
}

.description {
    margin: 18px 0 22px;
    font-size: 14px;
    line-height: 1.8;
    color: var(--el-text-color-regular);
    white-space: pre-wrap;
}

.empty {
    padding: 60px 0;
    text-align: center;
    color: grey;
}

@media (max-width: 700px) {
    .detail-body {
        flex-direction: column;
    }

    .cover {
        width: 100%;
        height: 220px;
    }
}
</style>
