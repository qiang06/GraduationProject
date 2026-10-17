<script setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {ArrowLeft, Delete, Star} from '@element-plus/icons-vue'
import LightCard from '@/components/LightCard.vue'
import TopicTag from '@/components/TopicTag.vue'
import {apiForumCollect, apiForumCollects} from '@/net/api'

const router = useRouter()
const list = ref([])
const loading = ref(false)

function load() {
    loading.value = true
    apiForumCollects(data => {
        list.value = data || []
        loading.value = false
    }, message => {
        ElMessage.warning(message)
        loading.value = false
    })
}

function remove(item) {
    apiForumCollect(item.id, () => {
        ElMessage.success('已取消收藏')
        load()
    }, message => ElMessage.warning(message))
}

function formatTime(time) {
    return time ? new Date(time).toLocaleString() : ''
}

onMounted(load)
</script>

<template>
    <div class="collect-page">
        <div class="page-heading">
            <div>
                <h1><el-icon><Star/></el-icon> 我的收藏</h1>
                <p>按收藏时间倒序，只包含还没有被作者删除的帖子</p>
            </div>
            <el-button plain :icon="ArrowLeft" @click="router.push('/index')">返回帖子广场</el-button>
        </div>
        <div v-loading="loading" class="collect-list">
            <light-card v-for="item in list" :key="item.id" class="collect-card"
                        @click="router.push(`/index/topic/${item.id}`)">
                <div class="collect-main">
                    <div class="collect-title">
                        <topic-tag :type="item.type"/>
                        <span>{{ item.title }}</span>
                    </div>
                    <div v-if="item.preview" class="collect-preview">{{ item.preview }}</div>
                    <div class="collect-meta">
                        <span>{{ item.username }}</span>
                        <span>{{ formatTime(item.time) }}</span>
                        <span>赞 {{ item.likeCount || 0 }} · 藏 {{ item.collectCount || 0 }}</span>
                    </div>
                </div>
                <el-button link type="danger" :icon="Delete" @click.stop="remove(item)">取消收藏</el-button>
            </light-card>
            <div v-if="!list.length && !loading" class="empty">还没有收藏任何帖子</div>
        </div>
    </div>
</template>

<style scoped>
.collect-page {
    max-width: 900px;
    margin: 20px auto;
    padding: 0 16px;
}

.page-heading {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
}

.page-heading h1 {
    margin: 0;
    font-size: 22px;
    display: flex;
    align-items: center;
    gap: 6px;
    color: var(--el-text-color-primary);
}

.page-heading p {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
}

.collect-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    min-height: 120px;
}

.collect-card {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    cursor: pointer;
    transition: background-color .2s;
}

.collect-card:hover {
    background: var(--el-fill-color-light);
}

.collect-main {
    flex: 1;
    min-width: 0;
}

.collect-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.collect-title span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.collect-preview {
    margin-top: 6px;
    font-size: 13px;
    line-height: 1.6;
    color: var(--el-text-color-regular);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.collect-meta {
    display: flex;
    gap: 14px;
    margin-top: 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.empty {
    padding: 48px 0;
    text-align: center;
    color: var(--el-text-color-secondary);
}
</style>
