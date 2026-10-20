<script setup>
import {ref} from 'vue'
import CampusNoticeAdmin from './CampusNoticeAdmin.vue'
import CampusCourseAdmin from './CampusCourseAdmin.vue'
import CampusLibraryAdmin from './CampusLibraryAdmin.vue'

const active = ref('notices')
</script>

<template>
    <div class="campus-admin">
        <div class="page-title">校园服务管理</div>
        <div class="page-hint">通知、课程表和图书馆数据均由管理员在本地维护，不依赖学校接口</div>
        <el-tabs v-model="active" class="campus-tabs">
            <el-tab-pane label="通知管理" name="notices">
                <campus-notice-admin/>
            </el-tab-pane>
            <el-tab-pane label="课程表" name="courses">
                <campus-course-admin/>
            </el-tab-pane>
            <el-tab-pane label="图书馆" name="library">
                <campus-library-admin/>
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<style scoped>
.campus-admin {
    background: var(--el-bg-color);
    border-radius: 8px;
    padding: 18px 20px 20px;
}

.page-title {
    font-size: 20px;
    font-weight: 700;
    color: var(--el-text-color-primary);
}

.page-hint {
    margin-top: 4px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
}

.campus-tabs {
    margin-top: 12px;
}
</style>
