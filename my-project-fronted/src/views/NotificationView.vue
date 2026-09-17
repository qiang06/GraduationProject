<script setup>
import {onMounted, ref} from 'vue'
import {Bell, Check, Finished} from '@element-plus/icons-vue'
import {useRouter} from 'vue-router'
import {get, post} from '@/net'
import {ElMessage} from 'element-plus'
import LightCard from '@/components/LightCard.vue'

const router = useRouter()
const items = ref([])
const loading = ref(false)
const unread = ref(0)

function load() {
    loading.value = true
    get('/api/notification/list?page=1&size=50', data => {
        items.value = data.items || []
        loading.value = false
    }, message => {
        ElMessage.warning(message)
        loading.value = false
    })
    get('/api/notification/unread', data => unread.value = data.count || 0)
}

function markRead(item) {
    if (!item.read) {
        post('/api/notification/read', {id: item.id}, () => {
            item.read = 1
            unread.value = Math.max(0, unread.value - 1)
        })
    }
    if (item.url) router.push(item.url)
}

function markAllRead() {
    post('/api/notification/read-all', {}, () => {
        items.value.forEach(item => item.read = 1)
        unread.value = 0
        ElMessage.success('已全部标记为已读')
    })
}

onMounted(load)
</script>

<template>
    <div class="notification-page">
        <div class="page-heading">
            <div><h1>消息列表</h1><p>{{ unread ? '有 ' + unread + ' 条未读消息' : '暂无未读消息' }}</p></div>
            <el-button type="primary" plain :icon="Finished" @click="markAllRead" :disabled="!unread">全部已读</el-button>
        </div>
        <div v-loading="loading" class="notification-list">
            <LightCard v-for="item in items" :key="item.id" class="notification-item" :class="{unread: !item.read}" @click="markRead(item)">
                <div class="notification-icon"><el-icon><Bell/></el-icon></div>
                <div class="notification-main">
                    <div class="notification-title"><strong>{{ item.title }}</strong><el-tag v-if="!item.read" size="small" type="danger">未读</el-tag></div>
                    <div class="notification-content">{{ item.content }}</div>
                    <time>{{ new Date(item.time).toLocaleString() }}</time>
                </div>
                <el-icon v-if="!item.read" class="mark-icon"><Check/></el-icon>
            </LightCard>
            <div v-if="!items.length && !loading" class="empty-state">还没有新的消息</div>
        </div>
    </div>
</template>

<style scoped>
.notification-page { max-width:900px; margin:20px auto; padding:0 16px; }
.page-heading { display:flex; justify-content:space-between; align-items:center; gap:16px; margin-bottom:16px; }
h1 { margin:0; font-size:24px; color:var(--el-text-color-primary); }
p { margin:6px 0 0; color:var(--el-text-color-secondary); }
.notification-list { display:flex; flex-direction:column; gap:10px; min-height:120px; }
.notification-item { display:flex; align-items:flex-start; gap:12px; cursor:pointer; transition:background-color .2s ease; }
.notification-item:hover { background:var(--el-fill-color-light); }
.notification-item.unread { border-left:3px solid var(--el-color-primary); }
.notification-icon { width:32px; height:32px; display:grid; place-items:center; color:var(--el-color-primary); }
.notification-main { flex:1; min-width:0; }
.notification-title { display:flex; align-items:center; gap:8px; margin-bottom:6px; }
.notification-content { color:var(--el-text-color-regular); line-height:1.5; overflow-wrap:anywhere; }
time { display:block; margin-top:8px; color:var(--el-text-color-secondary); font-size:12px; }
.mark-icon { color:var(--el-color-success); margin-top:4px; }
.empty-state { padding:48px 0; text-align:center; color:var(--el-text-color-secondary); }
@media (max-width:640px) { .page-heading { align-items:flex-start; flex-direction:column; } }
</style>
