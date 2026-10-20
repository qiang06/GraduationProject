<script setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import LightCard from '@/components/LightCard.vue'
import {ElMessage} from 'element-plus'
import {apiCampusNotices} from '@/net/api'

const router = useRouter()
const categories = ['全部', '教务', '考试', '选课', '其他']
const activeCategory = ref('全部')
const keyword = ref('')
const notices = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const pageSize = 10

function load(target = 1) {
    loading.value = true
    page.value = target
    const params = {page: String(target), size: String(pageSize)}
    if (activeCategory.value !== '全部') params.category = activeCategory.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    apiCampusNotices(params, data => {
        notices.value = data.items || []
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

function formatTime(time) {
    if (!time) return ''
    const d = new Date(time)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => load(1))
</script>

<template>
    <div class="campus-page">
        <div class="campus-head">
            <div>
                <div class="campus-title">教务通知</div>
                <div class="campus-hint">教务处、学生处等发布的校园通知，由管理员统一维护</div>
            </div>
            <el-input v-model="keyword" placeholder="搜索通知标题..." clearable style="width: 260px"
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

        <div v-loading="loading" class="notice-list">
            <div v-if="!loading && notices.length === 0" class="empty">
                暂时没有通知
            </div>
            <light-card v-for="notice in notices" :key="notice.id" class="notice-card"
                        @click="router.push('/index/notices/' + notice.id)">
                <div class="notice-top">
                    <el-tag v-if="notice.top === 1" type="danger" size="small">置顶</el-tag>
                    <el-tag size="small" effect="plain">{{ notice.category }}</el-tag>
                    <span class="notice-title">{{ notice.title }}</span>
                </div>
                <div class="notice-summary">{{ notice.summary }}</div>
                <div class="notice-meta">
                    <span>{{ notice.publisher }}</span>
                    <span>{{ formatTime(notice.publishTime) }}</span>
                </div>
            </light-card>
        </div>

        <div v-if="total > pageSize" class="pager">
            <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total"
                           layout="prev, pager, next" background @current-change="load"/>
        </div>
    </div>
</template>

<style scoped>
.campus-page {
    max-width: 980px;
    margin: 20px auto;
    padding: 0 16px;
}

.campus-head {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 14px;
}

.campus-title {
    font-size: 22px;
    font-weight: 700;
    color: var(--el-text-color-primary);
}

.campus-hint {
    margin-top: 4px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
}

.category-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
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

.notice-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    min-height: 120px;
}

.notice-card {
    cursor: pointer;
    transition: transform .15s ease;
}

.notice-card:hover {
    transform: translateY(-2px);
}

.notice-top {
    display: flex;
    align-items: center;
    gap: 8px;
}

.notice-title {
    flex: 1;
    min-width: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.notice-summary {
    margin-top: 6px;
    font-size: 13px;
    line-height: 1.6;
    color: var(--el-text-color-secondary);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.notice-meta {
    display: flex;
    gap: 16px;
    margin-top: 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.empty {
    padding: 40px 0;
    text-align: center;
    color: grey;
}

.pager {
    display: flex;
    justify-content: center;
    margin-top: 18px;
}

@media (max-width: 700px) {
    .campus-head {
        flex-direction: column;
        align-items: stretch;
    }
}
</style>
