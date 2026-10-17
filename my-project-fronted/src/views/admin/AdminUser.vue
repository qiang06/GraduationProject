<script setup>
import {onMounted, reactive, ref, watch} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {EditPen, Search, Unlock} from '@element-plus/icons-vue'
import {useStore} from '@/store'
import {apiAdminUserList, apiAdminUserPassword, apiAdminUserSave} from '@/net/api'

const store = useStore()
const table = reactive({list: [], page: 1, size: 10, total: 0})
const keyword = ref('')
const searchText = ref('')
const dialog = reactive({visible: false, form: {}})

function load() {
    apiAdminUserList(table.page, table.size, keyword.value, data => {
        table.list = data.items || []
        table.total = data.total || 0
    }, message => ElMessage.warning(message))
}

function search() {
    keyword.value = searchText.value.trim()
    table.page = 1
    load()
}

function openEdit(row) {
    dialog.form = {
        id: row.id,
        username: row.username,
        role: row.role,
        mute: !!row.mute,
        banned: !!row.banned
    }
    dialog.visible = true
}

function save() {
    apiAdminUserSave({
        id: dialog.form.id,
        role: dialog.form.role,
        mute: dialog.form.mute,
        banned: dialog.form.banned
    }, () => {
        ElMessage.success('保存成功')
        dialog.visible = false
        load()
    }, message => ElMessage.warning(message))
}

function changePassword(row) {
    ElMessageBox.prompt(`修改用户「${row.username}」的密码`, '修改密码', {
        inputPattern: /^.{6,20}$/,
        inputErrorMessage: '密码长度必须在 6-20 位',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
    }).then(({value}) => {
        apiAdminUserPassword(row.id, value, () => ElMessage.success('密码已修改'), message => ElMessage.warning(message))
    }).catch(() => {})
}

function statusText(row) {
    if (row.mute && row.banned) return '禁言 + 封禁'
    if (row.mute) return '禁言'
    if (row.banned) return '封禁'
    return '正常'
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
                <div class="title">用户管理</div>
                <div class="desc">禁言只限制发帖/评论；封禁会让用户无法登录。不能封禁或降级当前登录的管理员。</div>
            </div>
            <el-input v-model="searchText" :prefix-icon="Search" placeholder="搜索用户名/邮箱..."
                      clearable style="width: 260px" @clear="search" @keyup.enter="search">
                <template #append>
                    <el-button @click="search">搜索</el-button>
                </template>
            </el-input>
        </div>

        <el-table :data="table.list" height="440" border>
            <el-table-column prop="id" label="ID" width="70" align="center"/>
            <el-table-column label="用户" width="200">
                <template #default="{ row }">
                    <div class="user-cell">
                        <el-avatar :size="26" :src="store.avatarUserUrl(row.avatar)"/>
                        <span>{{ row.username }}</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip/>
            <el-table-column label="角色" width="100" align="center">
                <template #default="{ row }">
                    <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
                        {{ row.role === 'admin' ? '管理员' : '普通用户' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="状态" width="120" align="center">
                <template #default="{ row }">
                    <el-tag v-if="row.mute || row.banned" type="warning" size="small">{{ statusText(row) }}</el-tag>
                    <span v-else>正常</span>
                </template>
            </el-table-column>
            <el-table-column label="注册时间" width="170">
                <template #default="{ row }">{{ formatTime(row.registerTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="190" align="center">
                <template #default="{ row }">
                    <el-button size="small" type="primary" :icon="EditPen" plain @click="openEdit(row)">编辑</el-button>
                    <el-button size="small" type="warning" :icon="Unlock" @click="changePassword(row)">改密</el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="pagination">
            <el-pagination v-model:current-page="table.page" v-model:page-size="table.size"
                           :total="table.total" :page-sizes="[10, 20, 50]"
                           layout="total, sizes, prev, pager, next"/>
        </div>

        <el-dialog v-model="dialog.visible" title="编辑用户" width="420px">
            <el-form label-position="top">
                <el-form-item label="用户名">
                    <el-input :model-value="dialog.form.username" disabled/>
                </el-form-item>
                <el-form-item label="角色">
                    <el-select v-model="dialog.form.role" style="width: 100%">
                        <el-option label="普通用户" value="user"/>
                        <el-option label="管理员" value="admin"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="禁言（不能发帖/评论）">
                    <el-switch v-model="dialog.form.mute"/>
                </el-form-item>
                <el-form-item label="封禁（不能登录）">
                    <el-switch v-model="dialog.form.banned"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialog.visible = false">取消</el-button>
                <el-button type="primary" @click="save">保存</el-button>
            </template>
        </el-dialog>
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

.user-cell {
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
