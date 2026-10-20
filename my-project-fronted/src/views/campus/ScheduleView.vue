<script setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {Delete, Edit, Plus} from '@element-plus/icons-vue'
import LightCard from '@/components/LightCard.vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {apiCampusCourseDelete, apiCampusCourseSave, apiCampusCourses} from '@/net/api'

const weekdays = [
    {value: 1, label: '周一'},
    {value: 2, label: '周二'},
    {value: 3, label: '周三'},
    {value: 4, label: '周四'},
    {value: 5, label: '周五'},
    {value: 6, label: '周六'},
    {value: 7, label: '周日'}
]
const sections = Array.from({length: 12}, (_, i) => i + 1)
const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9B59B6', '#1ABC9C', '#909399', '#FF7F50']

const courses = ref([])
const loading = ref(false)
const view = ref('grid')
const dialog = ref(false)
const saving = ref(false)

const emptyForm = () => ({
    id: null,
    name: '',
    teacher: '',
    location: '',
    weekday: 1,
    startSection: 1,
    endSection: 2,
    weeks: '1-16周',
    color: '#409EFF',
    note: ''
})

const form = reactive(emptyForm())

const grouped = computed(() => weekdays.map(day => ({
    ...day,
    items: courses.value.filter(course => course.weekday === day.value)
        .sort((a, b) => a.startSection - b.startSection)
})))

function load() {
    loading.value = true
    apiCampusCourses(data => {
        courses.value = data || []
        loading.value = false
    }, message => {
        loading.value = false
        ElMessage.warning(message)
    })
}

function openAdd() {
    Object.assign(form, emptyForm())
    dialog.value = true
}

function openEdit(course) {
    Object.assign(form, emptyForm(), course)
    dialog.value = true
}

function save() {
    if (!form.name.trim()) {
        ElMessage.warning('请填写课程名称')
        return
    }
    if (form.endSection < form.startSection) {
        ElMessage.warning('结束节次不能早于开始节次')
        return
    }
    saving.value = true
    const payload = {
        id: form.id || null,
        name: form.name,
        teacher: form.teacher,
        location: form.location,
        weekday: form.weekday,
        startSection: form.startSection,
        endSection: form.endSection,
        weeks: form.weeks,
        color: form.color,
        note: form.note
    }
    apiCampusCourseSave(payload, () => {
        saving.value = false
        dialog.value = false
        ElMessage.success(form.id ? '课程已更新' : '课程已添加')
        load()
    }, message => {
        saving.value = false
        ElMessage.warning(message)
    })
}

function remove(id) {
    ElMessageBox.confirm('确定删除这门课程吗？', '删除课程', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        apiCampusCourseDelete(id, () => {
            ElMessage.success('已删除')
            dialog.value = false
            load()
        }, message => ElMessage.warning(message))
    }).catch(() => {})
}

function weekdayLabel(value) {
    return weekdays.find(day => day.value === value)?.label || ''
}

function courseStyle(course) {
    const span = Math.max(1, course.endSection - course.startSection + 1)
    return {
        gridColumn: String(course.weekday + 1),
        gridRow: `${course.startSection + 1} / span ${span}`,
        background: `${course.color || '#409EFF'}22`,
        borderLeft: `3px solid ${course.color || '#409EFF'}`
    }
}

onMounted(load)
</script>

<template>
    <div class="schedule-page">
        <div class="schedule-head">
            <div>
                <div class="schedule-title">我的课程表</div>
                <div class="schedule-hint">当前学期 · 共 {{ courses.length }} 门课，点击课程可编辑</div>
            </div>
            <div class="head-actions">
                <el-radio-group v-model="view" size="small">
                    <el-radio-button value="grid">周视图</el-radio-button>
                    <el-radio-button value="list">列表</el-radio-button>
                </el-radio-group>
                <el-button type="primary" :icon="Plus" @click="openAdd">添加课程</el-button>
            </div>
        </div>

        <div v-loading="loading">
            <div v-if="!loading && courses.length === 0" class="empty">
                <div>还没有课程</div>
                <el-button type="primary" plain :icon="Plus" @click="openAdd">添加第一门课</el-button>
            </div>

            <div v-show="view === 'grid' && courses.length" class="schedule-scroll">
                <div class="schedule-grid">
                    <div class="head-cell" :style="{gridColumn: 1, gridRow: 1}">节次</div>
                    <div v-for="day in weekdays" :key="day.value" class="head-cell"
                         :style="{gridColumn: day.value + 1, gridRow: 1}">{{ day.label }}</div>
                    <template v-for="section in sections" :key="section">
                        <div class="section-cell"
                             :style="{gridColumn: 1, gridRow: section + 1}">{{ section }}</div>
                        <div v-for="day in weekdays" :key="day.value + '-' + section" class="slot-cell"
                             :style="{gridColumn: day.value + 1, gridRow: section + 1}"></div>
                    </template>
                    <div v-for="course in courses" :key="course.id" class="course-card"
                         :style="courseStyle(course)" @click="openEdit(course)">
                        <div class="course-name">{{ course.name }}</div>
                        <div v-if="course.location" class="course-sub">{{ course.location }}</div>
                        <div v-if="course.weeks" class="course-sub">{{ course.weeks }}</div>
                    </div>
                </div>
            </div>

            <div v-show="view === 'list' && courses.length" class="course-list">
                <div v-for="day in grouped" :key="day.value" class="day-group">
                    <div class="day-title">{{ day.label }}</div>
                    <div v-if="day.items.length === 0" class="day-empty">没课</div>
                    <light-card v-for="course in day.items" :key="course.id" class="course-row">
                        <div class="course-time">
                            <span>{{ course.startSection }}-{{ course.endSection }} 节</span>
                            <span class="course-weeks">{{ course.weeks }}</span>
                        </div>
                        <div class="course-info">
                            <div class="course-name">{{ course.name }}</div>
                            <div class="course-sub">
                                {{ course.teacher || '未填写教师' }}
                                <span v-if="course.location">· {{ course.location }}</span>
                            </div>
                            <div v-if="course.note" class="course-note">{{ course.note }}</div>
                        </div>
                        <div class="row-actions">
                            <el-button link type="primary" :icon="Edit" @click="openEdit(course)">编辑</el-button>
                            <el-button link type="danger" :icon="Delete" @click="remove(course.id)">删除</el-button>
                        </div>
                    </light-card>
                </div>
            </div>
        </div>

        <el-dialog v-model="dialog" :title="form.id ? '编辑课程' : '添加课程'" width="560px"
                   :close-on-click-modal="false">
            <el-form label-width="80px">
                <el-form-item label="课程名称" required>
                    <el-input v-model="form.name" maxlength="80" placeholder="如：高等数学A"/>
                </el-form-item>
                <el-form-item label="教师">
                    <el-input v-model="form.teacher" maxlength="50" placeholder="如：张老师"/>
                </el-form-item>
                <el-form-item label="地点">
                    <el-input v-model="form.location" maxlength="80" placeholder="如：教1-101"/>
                </el-form-item>
                <el-form-item label="星期">
                    <el-select v-model="form.weekday" style="width: 100%">
                        <el-option v-for="day in weekdays" :key="day.value" :label="day.label" :value="day.value"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="节次" required>
                    <div class="section-row">
                        <el-select v-model="form.startSection" style="width: 46%">
                            <el-option v-for="section in sections" :key="section" :label="`第 ${section} 节`"
                                       :value="section"/>
                        </el-select>
                        <span>至</span>
                        <el-select v-model="form.endSection" style="width: 46%">
                            <el-option v-for="section in sections" :key="section" :label="`第 ${section} 节`"
                                       :value="section"/>
                        </el-select>
                    </div>
                </el-form-item>
                <el-form-item label="周次">
                    <el-input v-model="form.weeks" maxlength="40" placeholder="如：1-16周"/>
                </el-form-item>
                <el-form-item label="颜色">
                    <el-color-picker v-model="form.color" :predefine="colors"/>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="form.note" maxlength="200" placeholder="如：实验课在周四晚上"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button v-if="form.id" type="danger" plain @click="remove(form.id)">删除</el-button>
                    <div>
                        <el-button @click="dialog = false">取消</el-button>
                        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
                    </div>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<style scoped>
.schedule-page {
    max-width: 1100px;
    margin: 20px auto;
    padding: 0 16px;
}

.schedule-head {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 14px;
}

.schedule-title {
    font-size: 22px;
    font-weight: 700;
    color: var(--el-text-color-primary);
}

.schedule-hint {
    margin-top: 4px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
}

.head-actions {
    display: flex;
    align-items: center;
    gap: 12px;
}

.schedule-scroll {
    overflow-x: auto;
    padding-bottom: 6px;
}

.schedule-grid {
    display: grid;
    grid-template-columns: 54px repeat(7, minmax(110px, 1fr));
    grid-auto-rows: 58px;
    min-width: 900px;
    border-top: 1px solid var(--el-border-color-lighter);
    border-left: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
    overflow: hidden;
    background: var(--el-bg-color);
}

.head-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--el-fill-color-lighter);
    border-right: 1px solid var(--el-border-color-lighter);
    border-bottom: 1px solid var(--el-border-color-lighter);
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.section-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    border-right: 1px solid var(--el-border-color-lighter);
    border-bottom: 1px solid var(--el-border-color-lighter);
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.slot-cell {
    border-right: 1px solid var(--el-border-color-lighter);
    border-bottom: 1px solid var(--el-border-color-lighter);
}

.course-card {
    z-index: 3;
    margin: 3px;
    padding: 4px 7px;
    border-radius: 5px;
    overflow: hidden;
    cursor: pointer;
    transition: filter .15s;
}

.course-card:hover {
    filter: brightness(.96);
}

.course-name {
    font-size: 12px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.course-sub {
    margin-top: 2px;
    font-size: 11px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.course-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.day-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.day-title {
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.day-empty {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    padding: 2px 0;
}

.course-row {
    display: flex;
    align-items: center;
    gap: 14px;
}

.course-time {
    width: 105px;
    flex: none;
    display: flex;
    flex-direction: column;
    gap: 2px;
    font-size: 13px;
    font-weight: 600;
    color: var(--el-color-primary);
}

.course-weeks {
    font-size: 11px;
    font-weight: 400;
    color: var(--el-text-color-secondary);
}

.course-info {
    flex: 1;
    min-width: 0;
}

.course-note {
    margin-top: 2px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.row-actions {
    flex: none;
}

.section-row {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
}

.dialog-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    padding: 60px 0;
    color: grey;
}

@media (max-width: 760px) {
    .schedule-head {
        flex-direction: column;
        align-items: stretch;
    }

    .head-actions {
        justify-content: space-between;
    }
}
</style>
