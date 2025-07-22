<script setup>
import { logout,get } from '@/net'
import router from "@/router";
import {useStore} from "@/store";
import { ref } from 'vue'
const store = useStore()

const loading = ref(true)
get('/api/user/info',(data)=>{
  store.user = data
  loading.value = false
})

function userLogout() {
  logout(() => router.push("/"))
}
</script>


<template>

  <div class="main-content" v-loading="loading" element-loading-text="正在加载,请稍后...">
    <el-container style="height: 100%" v-if="!loading">
      <el-header class="main-content-header">
        <el-image class="logo" src="https://element-plus.org/images/element-plus-logo.svg"/>

        <div class="user-info" style="flex: 1">
          <div class="profile">
            <div>{{store.user.username}}</div>
            <div>{{ store.user.email }}</div>

          </div>
          <el-avatar src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"/>

        </div>


      </el-header>
      <el-container>
        <el-aside width="200px">Aside</el-aside>
        <el-main>Main</el-main>
      </el-container>
    </el-container>
  </div>


</template>



<style scoped>

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

    .profile {
      margin-right: 20px;
      text-align: right;

      :first-child{
        font-size: 18px;
        font-weight: bold;
        line-height: 20px;

      }
      :last-child{
        font-size: 10px;

        color: grey;
      }
    }
  }
}

</style>
