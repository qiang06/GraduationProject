<script setup>
import {apiAuthLogout, apiForumTypes, apiUserInfo} from '@/net/api'
import {isRoleAdmin} from '@/net'
import router from "@/router";
import {useStore} from "@/store";
import {ref, reactive} from 'vue'
import {ElMessage} from "element-plus";
import {
    Back,
    Bell,
    Calendar,
    ChatDotSquare, Collection,
    Document,
    Location, Lock, Message,
    Notification, Operation,
    Position,
    Search,
    Umbrella, User
} from "@element-plus/icons-vue";

const store = useStore()

const loading = ref(true)
apiUserInfo((data) => {
    store.user = data
    loading.value = false
})
// 分类元数据全应用只拉一次，TopicTag / 发帖编辑器共用
if (!store.forum.types.length) {
    // 和 jwt-1.0 一样：在真实分类前插一个「全部」伪分类，只有分类筛选栏会用到它；
    // 发帖编辑器的下拉框会过滤掉 id=0，避免把「全部」当成可发布类型
    apiForumTypes(data => {
        store.forum.types = [
            {name: '全部', id: 0, color: 'linear-gradient(45deg, white, red, orange, gold, green, blue)'},
            ...(data || [])
        ]
    })
}

function userLogout() {
    apiAuthLogout(() => router.push("/"))
}

function goUserSetting() {
    router.push('/index/user-setting')
}

function showCollects() {
    router.push('/index/collects')
}

function showMessages() {
    router.push('/index/notifications')
}

function searchTopics() {
    const text = searchInput.text.trim()
    router.push({ path: '/index', query: text ? { keyword: text } : {} })
}

const searchInput = reactive({
    type: "1",
    text: ""
})
</script>


<template>

    <div v-loading="loading" class="main-content" element-loading-text="正在加载,请稍后...">
        <el-container v-if="!loading" style="height: 100%">
            <el-header class="main-content-header">
                <el-image class="logo" src="https://element-plus.org/images/element-plus-logo.svg"/>

                <div class="user-info" style="flex: 1">
                    <div style="flex: 1;padding: 0 20px;text-align: center">
                        <el-input v-model="searchInput.text" placeholder="搜索帖子标题..."
                                  clearable @keyup.enter="searchTopics"
                                  style="width: 100%;max-width: 500px ">
                            <template #prefix>
                                <el-icon>
                                    <Search/>
                                </el-icon>
                            </template>
                            <template #append>
                                <el-button :icon="Search" aria-label="搜索帖子" @click="searchTopics"/>
                            </template>
                        </el-input>
                    </div>
                    <el-dropdown>
                        <el-avatar :src="store.avatarUrl"/>
                        <template #dropdown>
                            <el-dropdown-item @click="showCollects">
                                <el-icon>
                                    <collection/>
                                </el-icon>
                                我的收藏
                            </el-dropdown-item>
                            <el-dropdown-item v-if="isRoleAdmin()" @click="router.push('/admin')">
                                <el-icon>
                                    <operation/>
                                </el-icon>
                                管理后台
                            </el-dropdown-item>
                            <el-dropdown-item @click="goUserSetting">
                                <el-icon>
                                    <operation/>
                                </el-icon>
                                个人设置
                            </el-dropdown-item>
                            <el-dropdown-item @click="showMessages">
                                <el-icon>
                                    <message/>
                                </el-icon>
                                消息列表
                            </el-dropdown-item>
                            <el-dropdown-item divided @click="userLogout">
                                <el-icon>
                                    <Back/>
                                </el-icon>
                                退出登录
                            </el-dropdown-item>
                        </template>
                    </el-dropdown>
                    <div class="profile">
                        <div>{{ store.user.username }}</div>
                        <div>{{ store.user.email }}</div>
                    </div>
                </div>
            </el-header>
            <el-container>
                <el-aside width="230px">
                    <el-scrollbar style="min-height: calc(100vh - 55px);">
                        <el-menu
                            :default-active="$route.fullPath"
                            :default-openeds="['1','2','3']"
                            router
                            style="height: calc(100vh - 55px);">
                            <el-sub-menu index="1">
                                <template #title>
                                    <el-icon>
                                        <location/>
                                    </el-icon>
                                    <span><b>校园论坛</b></span>
                                </template>
                                <el-menu-item index="/index">
                                    <template #title>
                                        <el-icon>
                                            <chat-dot-square/>
                                        </el-icon>
                                        <span>帖子广场</span>
                                    </template>
                                </el-menu-item>
                                <el-menu-item index="/index/lost">
                                    <template #title>
                                        <el-icon>
                                            <bell/>
                                        </el-icon>
                                        <span>失物招领</span>
                                    </template>
                                </el-menu-item>
                                <el-menu-item index="/index/activity">
                                    <template #title>
                                        <el-icon>
                                            <notification/>
                                        </el-icon>
                                        <span>校园活动</span>
                                    </template>
                                </el-menu-item>
                                <el-menu-item index="/index/confession">
                                    <template #title>
                                        <el-icon>
                                            <umbrella/>
                                        </el-icon>
                                        <span>表白墙</span>
                                    </template>
                                </el-menu-item>
                            </el-sub-menu>
                            <el-sub-menu index="2">
                                <template #title>
                                    <el-icon>
                                        <position/>
                                    </el-icon>
                                    <span><b>校园服务</b></span>
                                </template>
                                <el-menu-item index="/index/grade">
                                    <template #title>
                                        <el-icon>
                                            <document/>
                                        </el-icon>
                                        <span>成绩查询</span>
                                    </template>
                                </el-menu-item>
                                <el-menu-item index="/index/notices">
                                    <template #title>
                                        <el-icon>
                                            <notification/>
                                        </el-icon>
                                        <span>教务通知</span>
                                    </template>
                                </el-menu-item>
                                <el-menu-item index="/index/schedule">
                                    <template #title>
                                        <el-icon>
                                            <calendar/>
                                        </el-icon>
                                        <span>我的课程表</span>
                                    </template>
                                </el-menu-item>
                                <el-menu-item index="/index/library">
                                    <template #title>
                                        <el-icon>
                                            <collection/>
                                        </el-icon>
                                        <span>在线图书馆</span>
                                    </template>
                                </el-menu-item>
                            </el-sub-menu>
                            <el-sub-menu index="3">
                                <template #title>
                                    <el-icon>
                                        <operation/>
                                    </el-icon>
                                    <span><b>个人设置</b></span>
                                </template>
                                <el-menu-item index="/index/user-setting">
                                    <template #title>
                                        <el-icon>
                                            <user/>
                                        </el-icon>
                                        <span>个人信息设置</span>
                                    </template>
                                </el-menu-item>
                                <el-menu-item index="/index/privacy-setting">
                                    <template #title>
                                        <el-icon>
                                            <Lock/>
                                        </el-icon>
                                        <span>账号安全设置</span>
                                    </template>
                                </el-menu-item>
                            </el-sub-menu>

                        </el-menu>
                    </el-scrollbar>

                </el-aside>
                <el-main class="main-content-page">
                    <el-scrollbar style="height: calc(100vh - 55px)">
                        <router-view v-slot="{ Component }">
                            <transition mode="out-in" name="el-fade-in-linear">
                                <keep-alive include="TopicList">
                                    <component :is="Component" style="height: 100%"/>
                                </keep-alive>
                            </transition>
                        </router-view>
                        <el-backtop target=".main-content-page .el-scrollbar__wrap" :right="20" :bottom="70"/>
                    </el-scrollbar>
                </el-main>
            </el-container>
        </el-container>
    </div>


</template>


<style scoped>

.main-content-page {
    padding: 0;
    background-color: #f7f8fa;
}

.dark .main-content-page {
    background-color: #212225;
}

.main-content {
    height: 100vh;
    width: 100vw;
}

.main-content-header {
    border-bottom: solid 1px var(--el-border-color);
    height: 55px;
    display: flex;
    align-items: center;
    box-sizing: border-box;

    .logo {
        height: 32px;
    }

    .user-info {
        display: flex;
        justify-content: flex-end;
        align-items: center;

        .el-avatar:hover {
            cursor: pointer;
        }

        .profile {
            margin-right: 20px;
            text-align: right;

            :first-child {
                font-size: 18px;
                font-weight: bold;
                line-height: 20px;

            }

            :last-child {
                font-size: 10px;

                color: grey;
            }
        }
    }
}

</style>
