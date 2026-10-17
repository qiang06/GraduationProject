<script setup>
import {onMounted, reactive, ref, watch} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Delete, Hide, Lock, Search, Top} from '@element-plus/icons-vue'
import {useStore} from '@/store'
import {
    apiAdminTopicChangeType,
    apiAdminTopicDelete,
    apiAdminTopicInvisible,
    apiAdminTopicList,
    apiAdminTopicLocked,
    apiAdminTopicTop
} from '@/net/api'

const props = defineProps({
    types: Array
})

const store = useStore()
const table = reactive({list: [], page: 1, size: 10, total: 0})
const keyword = ref('')
const searchText = ref('')

function findType(id) {
    return (props.types || []).find(type => type.id === id)
}

function load() {
    apiAdminTopicList(table.page, table.size, keyword.value, data => {
        table.list = data.items || []
        table.total = data.total || 0
    }, message => ElMessage.warning(message))
}

function search() {
    keyword.value = searchText.value.trim()
    table.page = 1
    load()
}

function setTop(row) {
    apiAdminTopicTop(row.id, row.top !== 1, () => {
        ElMessage.success('置顶状态已更新')
        load()
    }, message => ElMessage.warning(message))
}

function setLocked(row) {
    apiAdminTopicLocked(row.id, row.locked !== 1, () => {
        ElMessage.success('锁定状态已更新')
        load()
    }, message => ElMessage.warning(message))
}

function setInvisible(row) {
    apiAdminTopicInvisible(row.id, row.invisible !== 1, () => {
        ElMessage.success('屏蔽状态已更新')
        load()
    }, message => ElMessage.warning(message))
}

function changeType(row, type) {
    apiAdminTopicChangeType(row.id, type, () => {
        row.type = type
        ElMessage.success('分类已更新')
    }, message => ElMessage.warning(message))
}

function remove(row) {
    ElMessageBox.confirm(`确定删除帖子「${row.title}」吗？删除后无法恢复。`, '删除帖子', {
        type: 'warning'
    }).then(() => {
        apiAdminTopicDelete(row.id, () => {
            ElMessage.success('帖子已删除')
            load()
        }, message => ElMessage.warning(message))
    }).catch(() => {})
}

function formatTime(time) {
    return time ? new Date(time).toLocaleString() : ''
}

watch(() => [table.page, table.size], load)
onMounted(load)
</script>

<template>
    <div>
        <div class="section-header">
            <div>
                <div class="title">帖子管理</div>
                <div class="desc">置顶、锁定、屏蔽、改分类、删除；被屏蔽的帖子只有管理员和作者可见</div>
            </div>
            <el-input v-model="searchText" :prefix-icon="Search" placeholder="搜索帖子标题..."
                      clearable style="width: 240px" @clear="search" @keyup.enter="search">
                <template #append>
                    <el-button @click="search">搜索</el-button>
                </template>
            </el-input>
        </div>

        <el-table :data="table.list" height="440" border>
            <el-table-column prop="id" label="ID" width="70" align="center"/>
            <el-table-column label="标题" min-width="260">
                <template #default="{ row }">
                    <div class="topic-title-cell">
                        <el-icon v-if="row.top === 1" color="var(--el-color-danger)"><Top/></el-icon>
                        <el-icon v-if="row.locked === 1" color="var(--el-color-warning)"><Lock/></el-icon>
                        <el-icon v-if="row.invisible === 1" color="var(--el-color-info)"><Hide/></el-icon>
                        <span>{{ row.title }}</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column label="分类" width="150">
                <template #default="{ row }">
                    <el-select v-model="row.type" size="small" style="width: 120px"
                               @change="value => changeType(row, value)">
                        <el-option v-for="type in types" :key="type.id" :label="type.name" :value="type.id"/>
                    </el-select>
                </template>
            </el-table-column>
            <el-table-column label="作者" width="150">
                <template #default="{ row }">
                    <div class="author-cell">
                        <el-avatar :size="22" :src="store.avatarUserUrl(row.avatar)"/>
                        <span>{{ row.username }}</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column label="发布时间" width="170">
                <template #default="{ row }">{{ formatTime(row.time) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="290" align="center" fixed="right">
                <template #default="{ row }">
                    <el-button size="small" :type="row.top === 1 ? 'danger' : 'success'"
                               plain @click="setTop(row)">{{ row.top === 1 ? '取消置顶' : '置顶' }}</el-button>
                    <el-button size="small" :type="row.locked === 1 ? 'warning' : 'info'"
                               plain @click="setLocked(row)">{{ row.locked === 1 ? '解锁' : '锁定' }}</el-button>
                    <el-button size="small" :type="row.invisible === 1 ? 'primary' : 'info'"
                               plain @click="setInvisible(row)">{{ row.invisible === 1 ? '取消屏蔽' : '屏蔽' }}</el-button>
                    <el-button size="small" type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="pagination">
            <el-pagination v-model:current-page="table.page" v-model:page-size="table.size"
                           :total="table.total" :page-sizes="[10, 20, 50]"
                           layout="total, sizes, prev, pager, next"/>
        </div>
    </div>
</template>

<style scoped>
.section-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    margin-bottom: 12px;
}

.title {
    font-weight: 700;
    font-size: 16px;
    color: var(--el-text-color-primary);
}

.desc {
    margin-top: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.topic-title-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    overflow: hidden;
}

.topic-title-cell span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.author-cell {
    display: flex;
    align-items: center;
    gap: 8px;
}

.pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 14px;
}
</style>
