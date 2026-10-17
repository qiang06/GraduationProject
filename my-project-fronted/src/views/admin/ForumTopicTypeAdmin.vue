<script setup>
import {reactive} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Delete, Edit, Plus} from '@element-plus/icons-vue'
import {apiAdminTypeCreate, apiAdminTypeDelete, apiAdminTypeUpdate} from '@/net/api'

const props = defineProps({
    types: Array
})

const emit = defineEmits(['update'])

const dialog = reactive({
    visible: false,
    mode: 'create',
    form: {}
})

function openCreate() {
    dialog.mode = 'create'
    dialog.form = {code: '', name: '', desc: '', color: '#409eff', anonymous: false}
    dialog.visible = true
}

function openEdit(row) {
    dialog.mode = 'update'
    dialog.form = {
        id: row.id,
        code: row.code,
        name: row.name,
        desc: row.desc,
        color: row.color,
        anonymous: !!row.anonymous
    }
    dialog.visible = true
}

function success(message) {
    ElMessage.success(message)
    dialog.visible = false
    emit('update')
}

function fail(message) {
    ElMessage.warning(message)
}

function save() {
    if (!dialog.form.name || !dialog.form.name.trim()) {
        ElMessage.warning('请填写分类名称')
        return
    }
    if (dialog.mode === 'create') {
        if (!/^[a-z0-9_]{2,32}$/.test(dialog.form.code || '')) {
            ElMessage.warning('code 只能是小写字母、数字、下划线（2-32 位）')
            return
        }
        apiAdminTypeCreate({...dialog.form}, () => success('创建成功'), fail)
    } else {
        apiAdminTypeUpdate({
            id: dialog.form.id,
            name: dialog.form.name,
            desc: dialog.form.desc,
            color: dialog.form.color,
            anonymous: dialog.form.anonymous
        }, () => success('保存成功'), fail)
    }
}

function remove(row) {
    ElMessageBox.confirm(`确定删除分类「${row.name}」吗？分类下还有帖子时不能删除。`, '删除分类', {
        type: 'warning'
    }).then(() => {
        apiAdminTypeDelete(row.id, () => success('删除成功'), fail)
    }).catch(() => {})
}
</script>

<template>
    <div>
        <div class="section-header">
            <div>
                <div class="title">分类管理</div>
                <div class="desc">code 是稳定标识，创建后不可修改；anonymous 控制该分类是否匿名展示</div>
            </div>
            <el-button type="primary" :icon="Plus" @click="openCreate">新建分类</el-button>
        </div>

        <el-table :data="types" border>
            <el-table-column prop="id" label="ID" width="70" align="center"/>
            <el-table-column prop="code" label="code" width="140" align="center"/>
            <el-table-column label="名称" width="160">
                <template #default="{ row }">
                    <div class="type-name">
                        <span class="dot" :style="{background: row.color || '#909399'}"></span>
                        {{ row.name }}
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="desc" label="简介" min-width="260" show-overflow-tooltip/>
            <el-table-column label="匿名" width="90" align="center">
                <template #default="{ row }">
                    <el-tag v-if="row.anonymous" type="warning" size="small">匿名</el-tag>
                    <span v-else>否</span>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="center">
                <template #default="{ row }">
                    <el-button size="small" type="primary" :icon="Edit" plain @click="openEdit(row)">编辑</el-button>
                    <el-button size="small" type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新建分类' : '编辑分类'" width="460px">
            <el-form label-position="top">
                <el-form-item label="code（稳定标识）">
                    <el-input v-model="dialog.form.code" :disabled="dialog.mode === 'update'"
                              placeholder="如 lost / activity / confession"/>
                </el-form-item>
                <el-form-item label="名称">
                    <div style="display: flex; gap: 10px; width: 100%">
                        <el-input v-model="dialog.form.name" maxlength="20"/>
                        <el-color-picker v-model="dialog.form.color"/>
                    </div>
                </el-form-item>
                <el-form-item label="简介">
                    <el-input v-model="dialog.form.desc" type="textarea" :rows="3" maxlength="255"/>
                </el-form-item>
                <el-form-item label="匿名展示">
                    <el-switch v-model="dialog.form.anonymous"/>
                    <span class="hint">开启后普通用户看不到作者身份（表白墙）</span>
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

.type-name {
    display: flex;
    align-items: center;
    gap: 8px;
}

.dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex: none;
}

.hint {
    margin-left: 10px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
}
</style>
