<script setup>
import {onMounted, ref} from 'vue'
import {Delete} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {apiAdminCampusCourseDelete, apiAdminCampusCourses} from '@/net/api'

const weekdays = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
const list = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const keyword = ref('')

function load(target = 1) {
    loading.value = true
    page.value = target
    const params = {page: String(target), size: String(pageSize)}
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    apiAdminCampusCourses(params, data => {
        list.value = data.items || []
        total.value = Number(data.total || 0)
        loading.value = false
    }, message => {
        loading.value = false
        ElMessage.warning(message)
    })
}

function remove(id) {
    ElMessageBox.confirm('确定删除这门课程吗？', '删除课程', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        apiAdminCampusCourseDelete(id, () => {
            ElMessage.success('已删除')
            if (list.value.length === 1 && page.value > 1) page.value--
            load(page.value)
        }, message => ElMessage.warning(message))
    }).catch(() => {})
}

onMounted(() => load(1))
</script>

<template>
    <div>
        <div class="toolbar">
            <el-input v-model="keyword" placeholder="搜索课程/教师/地点..." clearable style="width: 260px"
                      @keyup.enter="load(1)" @clear="load(1)"/>
            <el-button @click="load(1)">查询</el-button>
            <span class="hint">管理员只查看和删除课程，不替用户修改课表</span>
        </div>

        <el-table :data="list" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="70"/>
            <el-table-column prop="username" label="用户" width="130"/>
            <el-table-column prop="name" label="课程" min-width="150"/>
            <el-table-column prop="teacher" label="教师" width="110"/>
            <el-table-column prop="location" label="地点" width="130"/>
            <el-table-column label="星期" width="80">
                <template #default="{row}">{{ weekdays[row.weekday] }}</template>
            </el-table-column>
            <el-table-column label="节次" width="100">
                <template #default="{row}">{{ row.startSection }}-{{ row.endSection }} 节</template>
            </el-table-column>
            <el-table-column prop="weeks" label="周次" width="100"/>
            <el-table-column label="操作" width="90" fixed="right">
                <template #default="{row}">
                    <el-button link type="danger" :icon="Delete" @click="remove(row.id)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="pager">
            <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total"
                           layout="total, prev, pager, next" background @current-change="load"/>
        </div>
    </div>
</template>

<style scoped>
.toolbar {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
}

.hint {
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.pager {
    display: flex;
    justify-content: flex-end;
    margin-top: 14px;
}
</style>
