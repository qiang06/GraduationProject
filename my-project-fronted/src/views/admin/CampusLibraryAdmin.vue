<script setup>
import {onMounted, reactive, ref} from 'vue'
import axios from 'axios'
import {Delete, Edit, Plus, UploadFilled} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {accessHeader} from '@/net'
import {
    apiAdminCampusLibrary,
    apiAdminCampusLibraryDelete,
    apiAdminCampusLibrarySave
} from '@/net/api'

const categories = ['专业', '文学', '考试', '其他']
const list = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const keyword = ref('')
const category = ref('')
const dialog = ref(false)
const saving = ref(false)
const uploadHeaders = accessHeader()

const form = reactive({
    id: null,
    title: '',
    author: '',
    category: '专业',
    cover: '',
    description: '',
    url: ''
})

function load(target = 1) {
    loading.value = true
    page.value = target
    const params = {page: String(target), size: String(pageSize)}
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (category.value) params.category = category.value
    apiAdminCampusLibrary(params, data => {
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
        author: '',
        category: '专业',
        cover: '',
        description: '',
        url: ''
    })
    dialog.value = true
}

function openEdit(row) {
    Object.assign(form, {
        id: row.id,
        title: row.title,
        author: row.author || '',
        category: row.category || '其他',
        cover: row.cover || '',
        description: row.description || '',
        url: row.url || ''
    })
    dialog.value = true
}

function save() {
    if (!form.title.trim()) {
        ElMessage.warning('请填写书名/资源名')
        return
    }
    saving.value = true
    apiAdminCampusLibrarySave({...form, id: form.id || null}, () => {
        saving.value = false
        dialog.value = false
        ElMessage.success(form.id ? '资源已更新' : '资源已添加')
        load(page.value)
    }, message => {
        saving.value = false
        ElMessage.warning(message)
    })
}

function remove(id) {
    ElMessageBox.confirm('确定删除这条资源吗？', '删除资源', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        apiAdminCampusLibraryDelete(id, () => {
            ElMessage.success('已删除')
            if (list.value.length === 1 && page.value > 1) page.value--
            load(page.value)
        }, message => ElMessage.warning(message))
    }).catch(() => {})
}

function imageUrl(path) {
    if (!path) return ''
    if (/^https?:\/\//i.test(path)) return path
    const base = axios.defaults.baseURL || ''
    return path.startsWith('/images/') ? base + path : base + '/images' + path
}

function beforeCover(file) {
    if (file.size > 5 * 1024 * 1024) {
        ElMessage.warning('封面不能超过 5MB')
        return false
    }
    return true
}

function handleCover(response) {
    if (response && response.code === 200 && response.data) {
        form.cover = response.data
        ElMessage.success('封面上传成功')
    } else {
        ElMessage.warning(response?.message || '封面上传失败')
    }
}

onMounted(() => load(1))
</script>

<template>
    <div>
        <div class="toolbar">
            <el-input v-model="keyword" placeholder="搜索书名/作者..." clearable style="width: 220px"
                      @keyup.enter="load(1)" @clear="load(1)"/>
            <el-select v-model="category" placeholder="全部分类" clearable style="width: 140px" @change="load(1)">
                <el-option v-for="item in categories" :key="item" :label="item" :value="item"/>
            </el-select>
            <el-button @click="load(1)">查询</el-button>
            <el-button type="primary" :icon="Plus" @click="openAdd">添加资源</el-button>
        </div>

        <el-table :data="list" v-loading="loading" border stripe>
            <el-table-column label="封面" width="90">
                <template #default="{row}">
                    <img v-if="row.cover" :src="imageUrl(row.cover)" alt="" class="cover-thumb"/>
                    <div v-else class="cover-empty">{{ row.title?.charAt(0) }}</div>
                </template>
            </el-table-column>
            <el-table-column prop="title" label="书名/资源" min-width="200" show-overflow-tooltip/>
            <el-table-column prop="author" label="作者" width="140"/>
            <el-table-column prop="category" label="分类" width="90"/>
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

        <el-dialog v-model="dialog" :title="form.id ? '编辑资源' : '添加资源'" width="640px"
                   :close-on-click-modal="false" destroy-on-close>
            <el-form label-width="80px">
                <el-form-item label="书名" required>
                    <el-input v-model="form.title" maxlength="120"/>
                </el-form-item>
                <el-form-item label="作者">
                    <el-input v-model="form.author" maxlength="80"/>
                </el-form-item>
                <el-form-item label="分类" required>
                    <el-select v-model="form.category" style="width: 180px">
                        <el-option v-for="item in categories" :key="item" :label="item" :value="item"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="封面">
                    <div class="cover-upload">
                        <img v-if="form.cover" :src="imageUrl(form.cover)" alt="" class="cover-preview"/>
                        <el-upload :action="axios.defaults.baseURL + '/api/image/cache'"
                                   :headers="uploadHeaders" name="file" :show-file-list="false"
                                   accept="image/png,image/jpeg,image/webp"
                                   :before-upload="beforeCover" :on-success="handleCover">
                            <el-button :icon="UploadFilled">{{ form.cover ? '重新上传' : '上传封面' }}</el-button>
                        </el-upload>
                        <el-button v-if="form.cover" link type="danger" @click="form.cover = ''">移除</el-button>
                    </div>
                </el-form-item>
                <el-form-item label="简介">
                    <el-input v-model="form.description" type="textarea" :rows="4" maxlength="2000"
                              show-word-limit/>
                </el-form-item>
                <el-form-item label="阅读链接">
                    <el-input v-model="form.url" maxlength="500" placeholder="https://..."/>
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

.cover-thumb {
    width: 46px;
    height: 60px;
    object-fit: cover;
    border-radius: 4px;
    display: block;
}

.cover-empty {
    width: 46px;
    height: 60px;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-weight: 700;
    background: linear-gradient(135deg, #409eff, #9b59b6);
}

.cover-upload {
    display: flex;
    align-items: center;
    gap: 10px;
}

.cover-preview {
    width: 60px;
    height: 80px;
    object-fit: cover;
    border-radius: 4px;
}

.pager {
    display: flex;
    justify-content: flex-end;
    margin-top: 14px;
}
</style>
