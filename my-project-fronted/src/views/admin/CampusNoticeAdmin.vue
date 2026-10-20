<script setup>
import {onMounted, reactive, ref} from 'vue'
import {Delete, Edit, Plus} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import RichTextEditor from '@/components/RichTextEditor.vue'
import {
    apiAdminCampusNoticeDelete,
    apiAdminCampusNoticeSave,
    apiAdminCampusNotices,
    apiAdminCampusNoticeTop
} from '@/net/api'

const categories = ['教务', '考试', '选课', '其他']
const list = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const keyword = ref('')
const category = ref('')
const dialog = ref(false)
const saving = ref(false)

const form = reactive({
    id: null,
    title: '',
    category: '教务',
    content: '',
    top: false,
    publisher: '教务处'
})

function load(target = 1) {
    loading.value = true
    page.value = target
    const params = {page: String(target), size: String(pageSize)}
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (category.value) params.category = category.value
    apiAdminCampusNotices(params, data => {
        list.value = data.items || []
        total.value = Number(data.total || 0)
        loading.value = false
    }, message => {
        loading.value = false
        ElMessage.warning(message)
    })
}

function openAdd() {
    Object.assign(form, {
        id: null,
        title: '',
        category: '教务',
        content: '',
        top: false,
        publisher: '教务处'
    })
    dialog.value = true
}

function openEdit(row) {
    Object.assign(form, {
        id: row.id,
        title: row.title,
        category: row.category,
        content: row.content || '',
        top: row.top === 1,
        publisher: row.publisher || '教务处'
    })
    dialog.value = true
}

function save() {
    if (!form.title.trim()) {
        ElMessage.warning('请填写通知标题')
        return
    }
    if (!form.content || !form.content.replace(/<[^>]+>/g, '').trim()) {
        ElMessage.warning('请填写通知正文')
        return
    }
    saving.value = true
    apiAdminCampusNoticeSave({...form, id: form.id || null}, () => {
        saving.value = false
        dialog.value = false
        ElMessage.success(form.id ? '通知已更新' : '通知已发布')
        load(page.value)
    }, message => {
        saving.value = false
        ElMessage.warning(message)
    })
}

function toggleTop(row, value) {
    apiAdminCampusNoticeTop(row.id, value, () => {
        row.top = value ? 1 : 0
        ElMessage.success(value ? '已置顶' : '已取消置顶')
    }, message => ElMessage.warning(message))
}

function remove(id) {
    ElMessageBox.confirm('确定删除这条通知吗？', '删除通知', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        apiAdminCampusNoticeDelete(id, () => {
            ElMessage.success('已删除')
            if (list.value.length === 1 && page.value > 1) page.value--
            load(page.value)
        }, message => ElMessage.warning(message))
    }).catch(() => {})
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
    <div>
        <div class="toolbar">
            <el-input v-model="keyword" placeholder="搜索标题..." clearable style="width: 220px"
                      @keyup.enter="load(1)" @clear="load(1)"/>
            <el-select v-model="category" placeholder="全部分类" clearable style="width: 140px" @change="load(1)">
                <el-option v-for="item in categories" :key="item" :label="item" :value="item"/>
            </el-select>
            <el-button @click="load(1)">查询</el-button>
            <el-button type="primary" :icon="Plus" @click="openAdd">发布通知</el-button>
        </div>

        <el-table :data="list" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="70"/>
            <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip/>
            <el-table-column prop="category" label="分类" width="90"/>
            <el-table-column label="置顶" width="80">
                <template #default="{row}">
                    <el-switch :model-value="row.top === 1" @change="value => toggleTop(row, value)"/>
                </template>
            </el-table-column>
            <el-table-column prop="publisher" label="发布人" width="120"/>
            <el-table-column label="发布时间" width="160">
                <template #default="{row}">{{ formatTime(row.publishTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
                <template #default="{row}">
                    <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
                    <el-button link type="danger" :icon="Delete" @click="remove(row.id)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="pager">
            <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total"
                           layout="total, prev, pager, next" background @current-change="load"/>
        </div>

        <el-dialog v-model="dialog" :title="form.id ? '编辑通知' : '发布通知'" width="860px"
                   :close-on-click-modal="false" destroy-on-close>
            <el-form label-width="80px">
                <el-form-item label="标题" required>
                    <el-input v-model="form.title" maxlength="120" placeholder="通知标题"/>
                </el-form-item>
                <div class="form-row">
                    <el-form-item label="分类" required>
                        <el-select v-model="form.category" style="width: 160px">
                            <el-option v-for="item in categories" :key="item" :label="item" :value="item"/>
                        </el-select>
                    </el-form-item>
                    <el-form-item label="发布人">
                        <el-input v-model="form.publisher" maxlength="50" style="width: 180px"/>
                    </el-form-item>
                    <el-form-item label="置顶">
                        <el-switch v-model="form.top"/>
                    </el-form-item>
                </div>
                <el-form-item label="正文" required>
                    <rich-text-editor v-model="form.content" :height="300" placeholder="请输入通知正文..."/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialog = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style scoped>
.toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 12px;
}

.form-row {
    display: flex;
    align-items: flex-start;
    gap: 10px;
}

.pager {
    display: flex;
    justify-content: flex-end;
    margin-top: 14px;
}
</style>
