<script setup>
import {logout, get} from '@/net'
import router from "@/router";
import {useStore} from "@/store";
import {ref,reactive} from 'vue'
import {
  Back,
  Bell,
  ChatDotSquare, Collection, DataLine,
  Document, Files,
  Location, Lock, Message, Monitor,
  Notification, Operation,
  Position,
  School, Search,
  Umbrella, User
} from "@element-plus/icons-vue";

const store = useStore()

const loading = ref(true)
get('/api/user/info', (data) => {
  store.user = data
  loading.value = false
})

function userLogout() {
  logout(() => router.push("/"))
}

const searchInput = reactive({
  type: "1",
  text: ""
})
</script>


<template>

  <div class="main-content" v-loading="loading" element-loading-text="正在加载,请稍后...">
    <el-container style="height: 100%" v-if="!loading">
      <el-header class="main-content-header">
        <el-image class="logo" src="https://element-plus.org/images/element-plus-logo.svg"/>

        <div class="user-info" style="flex: 1">
          <div style="flex: 1;padding: 0 20px;text-align: center" >
            <el-input v-model="searchInput.text" style="width: 100%;max-width: 500px " placeholder="搜索论坛相关内容...">
            <template #prefix>
              <el-icon><Search/></el-icon>
            </template>
            <template #append>
              <el-select style="width: 120px" v-model="searchInput.type">
                <el-option value="1" label="帖子广场"/>
                <el-option value="2" label="校园活动"/>
                <el-option value="3" label="教务通知"/>
              </el-select>
            </template>
            </el-input>
          </div>
          <el-dropdown>
            <el-avatar src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"/>
            <template #dropdown>
              <el-dropdown-item>
                <el-icon><operation/></el-icon>
                个人设置
              </el-dropdown-item>
              <el-dropdown-item>
                <el-icon><message/></el-icon>
                消息列表
              </el-dropdown-item>
              <el-dropdown-item @click="userLogout" divided>
                <el-icon><Back/></el-icon>
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
                router
                :default-active="$route.path"
                style="height: calc(100vh - 55px);">
              <el-sub-menu index="1">
                <template #title>
                  <el-icon>
                    <location/>
                  </el-icon>
                  <span><b>校园论坛</b></span>
                </template>
                <el-menu-item index="1-1">
                  <template #title>
                    <el-icon>
                      <chat-dot-square/>
                    </el-icon>
                    <span>帖子广场</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon>
                      <bell/>
                    </el-icon>
                    <span>失物招领</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon>
                      <notification/>
                    </el-icon>
                    <span>校园活动</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon>
                      <umbrella/>
                    </el-icon>
                    <span>表白墙</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon>
                      <school/>
                    </el-icon>
                    <span>海文考研</span>
                    <el-tag>合作机构</el-tag>
                  </template>
                </el-menu-item>
              </el-sub-menu>
              <el-sub-menu index="2">
                <template #title>
                  <el-icon>
                    <position/>
                  </el-icon>
                  <span><b>探索与发现</b></span>
                </template>
                <el-menu-item>
                  <template #title>
                    <el-icon>
                      <document/>
                    </el-icon>
                    <span>成绩查询</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon>
                      <Files/>
                    </el-icon>
                    <span>班级课程表</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon>
                      <Monitor/>
                    </el-icon>
                    <span>教务通知</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon>
                      <Collection/>
                    </el-icon>
                    <span>在线图书馆</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon>
                      <data-line/>
                    </el-icon>
                    <span>预约教室</span>
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
                <el-menu-item>
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
             <transition name="el-fade-in-linear" mode="out-in">
               <component :is="Component" style="height: 100%" />
             </transition>
           </router-view>
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
