<script setup>

import LightCard from "@/components/LightCard.vue";
import {Calendar, CollectionTag, EditPen, Link} from "@element-plus/icons-vue";

import {computed,reactive,ref,onMounted} from 'vue'
import Weather from '@/components/Weather.vue'

import {ElMessage} from "element-plus";
import {get} from "@/net";

const today = computed(() => {
    const date = new Date()
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}年${month}月${day}日`
})
const weather = reactive({
    location: {},
    now: {},
    hourly: [],
    success: false
})

// 添加IP地址的响应式数据
const ipAddress = ref('正在获取...')



const friendLinks = [
    {src: "https://www.itbaima.cn/image/welcome/outsource/image-2.webp", url: "https://www.itbaima.cn/zh-CN"},
    {src: "https://www.itbaima.cn/image/welcome/outsource/image-1.webp", url: "https://www.itbaima.cn/zh-CN"},
    {src: "https://www.itbaima.cn/image/welcome/outsource/image-3.webp", url: "https://www.itbaima.cn/zh-CN"}
];

const openLink = (url) => {
    window.open(url, '_blank');
};

navigator.geolocation.getCurrentPosition(position => {
    const longitude = position.coords.longitude
    const latitude = position.coords.latitude
    get(`/api/forum/weather?longitude=${longitude}&latitude=${latitude}`, data => {
        Object.assign(weather, data)
        weather.success = true
    })
}, error => {
    console.info(error)
    ElMessage.warning('位置信息获取超时，请检测网络设置')
    get(`/api/forum/weather?longitude=116.40529&latitude=39.90499`, data => {
        Object.assign(weather, data)
        weather.success = true
    })
}, {
    timeout: 3000,
    enableHighAccuracy: true
})

onMounted(() => {
    get("/api/ip", // 确保路径以/开头
        success => {
            ipAddress.value = success;
        },
        (message, status, url) => {
            console.error(`IP获取失败: ${message} | 状态码: ${status} | URL: ${url}`);

            if (status === 401) {
                ipAddress.value = "需要登录才能获取";
            } else if (status === 429) {
                ipAddress.value = "请求过于频繁";
            } else {
                ipAddress.value = "无法获取IP地址";
            }
        },
        error => {
            console.error('网络请求失败:', error.message);

            if (error.code === "ECONNABORTED" || error.message.includes("timeout")) {
                ipAddress.value = "请求超时";
            } else {
                ipAddress.value = "网络连接异常";
            }
        }
    );
});







</script>

<template>
    <div style="display: flex;margin: 20px auto;gap: 20px;max-width: 900px">
        <div style="flex: 1">
            <light-card>
                <div class="creat-topic">
                    <el-icon>
                        <EditPen/>
                    </el-icon>
                    点击发表主题...
                </div>
            </light-card>
            <light-card style="margin-top: 10px;height: 30px">

            </light-card>
            <div style="margin: 10px 0 ;display: flex;flex-direction: column;gap: 10px">
                <light-card style="height: 150px" v-for="item in 10">

                </light-card>
            </div>

        </div>
        <div style="width: 280px">
            <div style="position: sticky;top: 20px">
                <light-card>
                    <div style="font-weight: bold">
                        <el-icon>
                            <CollectionTag/>
                        </el-icon>
                        论坛公告
                    </div>
                    <el-divider style="margin: 10px 0"/>
                    <div style="font-size: 14px;margin: 10px;color: grey">
                        亲爱的同学们：

                        为营造健康、文明、和谐的校园网络环境，保障论坛交流质量，现就校园论坛发帖规范及相关注意事项公告如下：

                        ​​内容要求​​
                        发帖内容需积极向上，与校园生活、学习、活动等相关，禁止发布广告、虚假信息、人身攻击或违反法律法规的内容。
                        鼓励分享学习经验、活动资讯、校园趣事等正能量内容。
                        ​​文明交流​​
                        讨论时请保持理性，尊重他人观点，禁止恶意引战、谩骂或使用不文明语言。
                        如遇争议，可联系版主或管理员协调处理。

                        <el-divider style="margin: 10px 0"/>
                        2025年8月9日
                    </div>
                </light-card>
                <light-card style="margin-top: 10px">
                    <div style="font-weight: bold">
                        <el-icon>
                            <Calendar/>
                        </el-icon>
                        天气信息
                    </div>
                    <el-divider style="margin: 10px 0"/>
                    <weather :data="weather"/>
                </light-card>
                <light-card style="margin-top: 10px">
                    <div class="info-text">
                        <div>当前日期</div>
                        <div>{{ today }}</div>
                    </div>
                    <div class="info-text">
                        <div>当前IP地址</div>
                        <div>{{ipAddress.ip }}</div>
                    </div>
                </light-card>
                <div style="font-size: 14px ;margin-top: 10px;color: grey">
                    <el-icon>
                        <Link/>
                    </el-icon>
                    友情链接
                    <el-divider style="margin:10px 0"/>
                </div>
                <div style="display: grid;grid-template-columns: repeat(2,1fr); grid-gap: 10px;margin-top: 10px">
                    <div v-for="link in friendLinks" :key="link.url" class="friend-link" @click="openLink(link.url)">
                        <el-image :src="link.src" style="height: 100%"/>
                    </div>


                </div>
            </div>

        </div>
    </div>
</template>

<style lang="less" scoped>
.info-text {
    display: flex;
    justify-content: space-between;
    color: grey;
    font-size: 14px;
}

.friend-link {
    border-radius: 5px;
    overflow: hidden;
    cursor: pointer;

}

.creat-topic {
    background-color: #efefef;
    border-radius: 5px;
    height: 40px;
    font-size: 14px;
    line-height: 40px;
    padding: 0 10px;
    color: grey;

    &:hover {
        cursor: pointer;
    }
}
</style>