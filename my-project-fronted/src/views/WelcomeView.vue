<template>
    <div style="width: 100vw;height: 100vh;overflow: hidden;display: flex">
        <div style="flex: 1">
            <el-image style="width: 100%;height: 100%" fit="cover"
                      src="http://hxqblog.cn/wp-content/uploads/2025/04/71875434_p0-3c0bae5cf7db4c0fab6986d02b8385fc-scaled.png"/>
        </div>
        <div class="welcome-title">
            <div style="font-size: 30px;font-weight: bold">欢迎来到我们的学习平台</div>
            <div style="margin-top: 10px">在这里你可以学习如何使用Java，如何搭建网站，并且与Java之父密切交流。</div>
            <div style="margin-top: 5px">在这里你可以同性交友，因为都是男的，没有学Java的女生。</div>
        </div>
        <div class="right-card">
            <router-view v-slot="{ Component }">
                <transition name="el-fade-in-linear" mode="out-in">
                    <component :is="Component" style="height: 100%"/>
                </transition>
            </router-view>
        </div>
    </div>
</template>

<script setup>

</script>

<style scoped>
.right-card {
  width: 400px;
  z-index: 1;
  background-color: var(--el-bg-color);
}

.welcome-title {
    position: absolute;
    bottom: 30px;
    left: 30px;
    color: white;
    text-shadow: 0 0 10px black;
}
</style>
