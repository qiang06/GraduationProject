<script setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import axios from 'axios'
import {ElMessage} from 'element-plus'
import {apiCampusLibrary} from '@/net/api'

const router = useRouter()
const categories = ['全部', '专业', '文学', '考试', '其他']
const activeCategory = ref('全部')
const keyword = ref('')
const resources = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const pageSize = 12

function load(target = 1) {
    loading.value = true
    page.value = target
    const params = {page: String(target), size: String(pageSize)}
    if (activeCategory.value !== '全部') params.category = activeCategory.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    apiCampusLibrary(params, data => {
        resources.value = data.items || []
        total.value = Number(data.total || 0)
        loading.value = false
    }, message => {
        loading.value = false
        ElMessage.warning(message)
    })
}

function selectCategory(category) {
    activeCategory.value = category
    load(1)
}

function search() {
    load(1)
}

function coverUrl(path) {
    if (!path) return ''
    if (/^https?:\/\//i.test(path)) return path
    const base = axios.defaults.baseURL || ''
    return path.startsWith('/images/') ? base + path : base + '/images' + path
}

onMounted(() => load(1))
</script>

<template>
    <div class="library-page">
        <div class="library-head">
            <div>
                <div class="library-title">在线图书馆</div>
                <div class="library-hint">管理员维护的书目与学习资源，点开可查看简介和阅读链接</div>
            </div>
            <el-input v-model="keyword" placeholder="搜索书名或作者..." clearable style="width: 260px"
                      @keyup.enter="search" @clear="search">
                <template #append>
                    <el-button @click="search">搜索</el-button>
                </template>
            </el-input>
        </div>

        <div class="category-row">
            <div v-for="category in categories" :key="category" class="category-chip"
                 :class="{active: activeCategory === category}" @click="selectCategory(category)">
                {{ category }}
            </div>
        </div>

        <div v-loading="loading" class="resource-grid">
            <div v-if="!loading && resources.length === 0" class="empty">暂时没有资源</div>
            <div v-for="resource in resources" :key="resource.id" class="resource-card"
                 @click="router.push('/index/library/' + resource.id)">
                <div class="cover">
                    <img v-if="resource.cover" :src="coverUrl(resource.cover)" alt=""/>
                    <div v-else class="cover-fallback">{{ resource.title?.charAt(0) }}</div>
                </div>
                <div class="resource-body">
                    <div class="resource-title">{{ resource.title }}</div>
                    <div class="resource-author">{{ resource.author || '佚名' }}</div>
                    <el-tag size="small" effect="plain">{{ resource.category }}</el-tag>
                </div>
            </div>
        </div>

        <div v-if="total > pageSize" class="pager">
            <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total"
                           layout="prev, pager, next" background @current-change="load"/>
        </div>
    </div>
</template>

<style scoped>
.library-page {
    max-width: 1100px;
    margin: 20px auto;
    padding: 0 16px;
}

.library-head {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 14px;
}

.library-title {
    font-size: 22px;
    font-weight: 700;
    color: var(--el-text-color-primary);
}

.library-hint {
    margin-top: 4px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
}

.category-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 14px;
}

.category-chip {
    padding: 2px 10px;
    border: 1px solid transparent;
    border-radius: 3px;
    background: #f5f5f5;
    font-size: 14px;
    color: var(--el-text-color-regular);
    cursor: pointer;
    transition: background-color .2s;
}

.category-chip:hover {
    background: #dadada;
}

.category-chip.active {
    border-color: #ead4c4;
    font-weight: 600;
}

:global(.dark) .category-chip {
    background: #282828;
}

:global(.dark) .category-chip:hover {
    background: #5e5e5e;
}

:global(.dark) .category-chip.active {
    border-color: #64594b;
}

.resource-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 14px;
    min-height: 160px;
}

.resource-card {
    overflow: hidden;
    border-radius: 8px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    cursor: pointer;
    transition: transform .15s, box-shadow .15s;
}

.resource-card:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, .08);
}

.cover {
    height: 170px;
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
    font-size: 44px;
    font-weight: 700;
    color: #fff;
    background: linear-gradient(135deg, #409eff, #9b59b6);
}

.resource-body {
    padding: 10px 12px 12px;
}

.resource-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.resource-author {
    margin: 4px 0 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.empty {
    grid-column: 1 / -1;
    padding: 50px 0;
    text-align: center;
    color: grey;
}

.pager {
    display: flex;
    justify-content: center;
    margin-top: 18px;
}

@media (max-width: 700px) {
    .library-head {
        flex-direction: column;
        align-items: stretch;
    }
}
</style>
