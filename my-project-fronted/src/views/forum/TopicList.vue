<script setup>
import LightCard from "@/components/LightCard.vue";
import {Calendar, ChatDotSquare, CollectionTag, EditPen, Link} from "@element-plus/icons-vue";
import {computed, reactive, ref, onMounted, watch} from 'vue'
import {useRoute, useRouter} from "vue-router";
import Weather from '@/components/Weather.vue'
import {ElMessage} from "element-plus";
import {get, post} from "@/net";
import TopicEditor from "@/components/TopicEditor.vue";

const router = useRouter()
const route = useRoute()

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

const ipAddress = ref('正在获取...')
const editor = ref(false)
const topics = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const pageSize = 10
const keyword = ref('')
const type = ref('')

function loadTopics() {
    loading.value = true
    keyword.value = route.query.keyword || ''
    type.value = route.query.type || ''
    const params = new URLSearchParams({ page: page.value, size: pageSize })
    if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
    if (type.value) params.set('type', type.value)
    get(`/api/forum/list?${params}`, (data) => {
        topics.value = data.items || []
        total.value = data.total || 0
        loading.value = false
    }, (message) => {
        ElMessage.warning(message)
        loading.value = false
    })
}

function nextPage() {
    page.value++
    loadTopics()
}

function prevPage() {
    if (page.value > 1) {
        page.value--
        loadTopics()
    }
}

function handleSubmit(data) {
    post('/api/forum/create', data, () => {
        ElMessage.success('发帖成功！')
        editor.value = false
        page.value = 1
        loadTopics()
    }, (message) => {
        ElMessage.warning(message)
    })
}

function goDetail(id) {
    router.push(`/index/topic/${id}`)
}

function formatTime(time) {
    if (!time) return ''
    const d = new Date(time)
    const now = new Date()
    const diff = now - d
    if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
    if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
    if (diff < 172800000) return '昨天'
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    return `${mm}-${dd}`
}

onMounted(() => {
    loadTopics()
})

watch(() => [route.query.keyword, route.query.type], () => {
    page.value = 1
    loadTopics()
})

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

get("/api/ip",
    success => {
        ipAddress.value = success.ip || JSON.stringify(success)
    },
    (message, status, url) => {
        console.error(`IP获取失败: ${message} | 状态码: ${status} | URL: ${url}`);
        if (status === 401) {
            ipAddress.value = "需要登录才能获取"
        } else if (status === 429) {
            ipAddress.value = "请求过于频繁"
        } else {
            ipAddress.value = "无法获取IP地址"
        }
    },
    error => {
        console.error('网络请求失败:', error.message);
        if (error.code === "ECONNABORTED" || error.message.includes("timeout")) {
            ipAddress.value = "请求超时"
        } else {
            ipAddress.value = "网络连接异常"
        }
    }
);
</script>

<template>
    <div style="display: flex;margin: 20px auto;gap: 20px;max-width: 900px">
        <div style="flex: 1">
            <light-card>
                <div class="creat-topic" @click="editor=true">
                    <el-icon>
                        <EditPen/>
                    </el-icon>
                    点击发表主题...
                </div>
            </light-card>
            <div style="margin: 10px 0;display: flex;flex-direction: column;gap: 10px" v-loading="loading">
                <div v-if="topics.length === 0 && !loading" style="text-align: center;color: grey;padding: 40px 0">
                    还没有人发帖，快来发表第一个帖子吧！
                </div>
                <div v-for="topic in topics" :key="topic.id" class="topic-card" @click="goDetail(topic.id)">
                    <light-card style="height: 100%">
                        <div class="topic-header">
                            <el-tag v-if="topic.top" type="danger" size="small" class="top-tag">置顶</el-tag>
                            <div class="topic-title">{{ topic.title }}</div>
                        </div>
                        <div class="topic-meta">
                            <span class="meta-item">
                                <el-icon><ChatDotSquare/></el-icon>
                                {{ topic.username }}
                            </span>
                            <span class="meta-item">{{ formatTime(topic.time) }}</span>
                            <span class="meta-item">赞 {{ topic.likeCount || 0 }} · 藏 {{ topic.collectCount || 0 }}</span>
                        </div>
                    </light-card>
                </div>
                <div v-if="topics.length > 0" class="pagination">
                    <el-button :disabled="page <= 1" @click="prevPage" size="small">上一页</el-button>
                    <span style="margin: 0 10px;color: grey;font-size: 13px">第 {{ page }} 页</span>
                    <el-button :disabled="page >= Math.ceil(total / pageSize)" @click="nextPage" size="small">下一页</el-button>
                </div>
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
                        亲爱的同学们：<br/><br/>
                        为营造健康、文明、和谐的校园网络环境，保障论坛交流质量，现就校园论坛发帖规范及相关注意事项公告如下：<br/><br/>
                        <b>内容要求</b><br/>
                        发帖内容需积极向上，与校园生活、学习、活动等相关，禁止发布广告、虚假信息、人身攻击或违反法律法规的内容。<br/>
                        鼓励分享学习经验、活动资讯、校园趣事等正能量内容。<br/><br/>
                        <b>文明交流</b><br/>
                        讨论时请保持理性，尊重他人观点，禁止恶意引战、谩骂或使用不文明语言。<br/>
                        如遇争议，可联系版主或管理员协调处理。<br/>
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
                        <div>{{ ipAddress }}</div>
                    </div>
                </light-card>
                <div style="font-size: 14px;margin-top: 10px;color: grey">
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
        <topic-editor :show="editor" @close="editor=false" @submit="handleSubmit"/>
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

.dark .creat-topic {
    background-color: #1c1c1c;
}

.topic-card {
    cursor: pointer;
    transition: transform 0.15s ease;

    &:hover {
        transform: translateY(-1px);
    }
}

.topic-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
}

.topic-title {
    font-size: 15px;
    font-weight: 600;
    line-height: 1.5;
    flex: 1;
}

.top-tag {
    flex-shrink: 0;
    margin-right: 6px;
}

.topic-meta {
    display: flex;
    gap: 16px;
    margin-top: 8px;
    font-size: 13px;
    color: grey;
}

.meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
}

.pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 10px 0;
}
</style>
