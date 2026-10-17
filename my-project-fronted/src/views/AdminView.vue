<script setup>
import {useStore} from '@/store'
import {apiAuthLogout} from '@/net/api'
import router from '@/router'
import {Back, ChatDotSquare, User} from '@element-plus/icons-vue'

const store = useStore()

function logout() {
    apiAuthLogout(() => router.push('/'))
}
</script>

<template>
    <div class="admin-layout">
        <el-container style="height: 100%">
            <el-aside width="220px" class="admin-aside">
                <div class="logo">校园论坛 · 管理后台</div>
                <el-menu router :default-active="$route.path" style="border: none">
                    <el-menu-item index="/admin/forum">
                        <el-icon><ChatDotSquare/></el-icon>
                        帖子管理
                    </el-menu-item>
                    <el-menu-item index="/admin/user">
                        <el-icon><User/></el-icon>
                        用户管理
                    </el-menu-item>
                </el-menu>
            </el-aside>
            <el-container>
                <el-header class="admin-header">
                    <div class="admin-title">管理后台</div>
                    <el-button link type="primary" @click="router.push('/index')">
                        <el-icon><Back/></el-icon>
                        回到用户端
                    </el-button>
                    <el-avatar :size="28" :src="store.avatarUrl"/>
                    <span class="admin-user">{{ store.user.username }}</span>
                    <el-button link type="danger" @click="logout">退出登录</el-button>
                </el-header>
                <el-main class="admin-main">
                    <router-view/>
                </el-main>
            </el-container>
        </el-container>
    </div>
</template>

<style scoped>
.admin-layout {
    height: 100vh;
    width: 100vw;
    background: var(--el-bg-color-page);
}

.admin-aside {
    border-right: 1px solid var(--el-border-color);
    background: var(--el-bg-color);
}

.logo {
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: 15px;
    border-bottom: 1px solid var(--el-border-color);
    color: var(--el-text-color-primary);
}

.admin-header {
    height: 56px;
    display: flex;
    align-items: center;
    gap: 14px;
    border-bottom: 1px solid var(--el-border-color);
    background: var(--el-bg-color);
}

.admin-title {
    flex: 1;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.admin-user {
    font-size: 14px;
    color: var(--el-text-color-regular);
}

.admin-main {
    padding: 20px;
    overflow-y: auto;
}
</style>
