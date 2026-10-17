<script setup>
import LightCard from "@/components/LightCard.vue";
import {ArrowRightBold, Calendar, ChatDotSquare, CollectionTag, EditPen, Link, Loading, Star} from "@element-plus/icons-vue";
import {computed, reactive, ref, onMounted, onBeforeUnmount, onActivated, nextTick, watch} from 'vue'
import {useRoute, useRouter} from "vue-router";
import Weather from '@/components/Weather.vue'
import {ElMessage} from "element-plus";
import {
    apiForumCitySearch,
    apiForumCreate,
    apiForumIp,
    apiForumList,
    apiForumTopTopics,
    apiForumWeather,
    apiForumWeatherCity
} from "@/net/api";
import axios from "axios";
import TopicEditor from "@/components/TopicEditor.vue";
import TopicTag from "@/components/TopicTag.vue";
import {useStore} from "@/store";

const router = useRouter()
const route = useRoute()
const store = useStore()

// 帖子广场顶部的分类筛选：只用 ?type=id，和侧边栏的分类路由互不影响
const selectedType = computed(() => Number(route.query.type) || 0)

function selectType(id) {
    const query = {...route.query}
    if (id) {
        query.type = String(id)
    } else {
        delete query.type
    }
    router.push({path: '/index', query})
}

// 花园的三个分类页复用这个组件，标题与筛选项来自路由 meta
const typeCode = computed(() => route.meta.typeCode || '')
const pageTitle = computed(() => route.meta.title || '帖子广场')
const pageHint = computed(() => route.meta.hint || '')

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
    success: false,
    failed: false,
    error: ''
})

// 浏览器定位不可用时的默认城市；可以改成学校所在城市
const DEFAULT_CITY = {id: '101010100', name: '北京'}
// 上次成功解析或手动选择的城市：下次直接按 id 取，不再依赖浏览器定位
const CITY_CACHE_KEY = 'forum:weatherCity'
const cityDialog = ref(false)
const cityKeyword = ref('')
const cityResults = ref([])
const citySearching = ref(false)
const citySearched = ref(false)

const ipAddress = ref('正在获取...')
const editor = ref(false)
const topics = ref([])
const topTopics = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const loadError = ref('')
const page = ref(1)
const total = ref(0)
const pageSize = 10
const keyword = ref('')
const type = ref('')
const listSentinel = ref(null)

let listObserver = null
// 每次筛选/重置都递增，用来丢弃旧的异步响应，避免搜索时把旧列表拼回来
let listGeneration = 0

function buildListParams(targetPage) {
    keyword.value = route.query.keyword || ''
    type.value = route.query.type || ''
    const params = new URLSearchParams({page: String(targetPage), size: String(pageSize)})
    if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
    if (type.value) params.set('type', type.value)
    if (typeCode.value) params.set('typeCode', typeCode.value)
    return params
}

function scrollListToTop() {
    const wrap = document.querySelector('.main-content-page .el-scrollbar__wrap')
    if (wrap) wrap.scrollTop = 0
}

/**
 * 统一的分页加载：
 * - reset=true：清空列表，从第一页重新拉（筛选、搜索、发帖后）
 * - reset=false：把下一页追加到列表尾部（无限滚动）
 */
function loadPage(targetPage, reset = false) {
    if (!reset && (loading.value || loadingMore.value || !hasMore.value || loadError.value)) return

    if (reset) {
        listGeneration++
        loading.value = true
        loadingMore.value = false
        loadError.value = ''
        hasMore.value = true
        page.value = 1
        topics.value = []
    } else {
        loadingMore.value = true
    }

    const generation = listGeneration
    apiForumList(buildListParams(targetPage), (data) => {
        if (generation !== listGeneration) return
        const items = data.items || []
        if (reset) {
            topics.value = items
        } else {
            const existing = new Set(topics.value.map(t => t.id))
            topics.value.push(...items.filter(t => !existing.has(t.id)))
        }
        total.value = Number(data.total || 0)
        // 后端异常返回空页但 hasNext=true 时也要停下，避免无限请求
        hasMore.value = Boolean(data.hasNext) && items.length > 0
        page.value = Number(data.page || targetPage) + 1
        loading.value = false
        loadingMore.value = false
        nextTick(() => {
            // 首屏内容不够高时 sentinel 可能一直可见，继续补下一页直到撑满或没有更多
            if (hasMore.value) maybeLoadMore()
        })
    }, (message) => {
        if (generation !== listGeneration) return
        loading.value = false
        loadingMore.value = false
        loadError.value = message || '加载失败'
        ElMessage.warning(message)
    })
}

function maybeLoadMore() {
    if (!hasMore.value || loading.value || loadingMore.value || loadError.value) return
    const el = listSentinel.value
    if (!el) return
    const rect = el.getBoundingClientRect()
    // 提前 300px 触发，滚动到底前就开始加载
    if (rect.top <= window.innerHeight + 300 && rect.bottom >= 0) {
        loadPage(page.value, false)
    }
}

function retryLoadMore() {
    loadError.value = ''
    loadPage(page.value, false)
}

function resetList() {
    scrollListToTop()
    loadPage(1, true)
}

function setupListObserver() {
    if (!listSentinel.value || typeof IntersectionObserver === 'undefined') return
    listObserver?.disconnect()
    listObserver = new IntersectionObserver((entries) => {
        if (entries[0]?.isIntersecting) maybeLoadMore()
    }, {rootMargin: '300px 0px'})
    listObserver.observe(listSentinel.value)
}

function handleSubmit(data) {
    apiForumCreate(data, () => {
        ElMessage.success('发帖成功！')
        editor.value = false
        resetList()
    }, (message) => {
        ElMessage.warning(message)
    })
}

/**
 * 把库里存的图片地址补全成可直接访问的 URL。
 *
 * 两种形态都要兼容：
 *  - 头像存在 db_account.avatar，是相对路径，如 /avatar/xxx → 需补 /images 前缀
 *  - 正文里插入的图片是编辑器上传后拼好的绝对地址，如 http://localhost:8080/images/cache/xxx
 *
 * 前缀规则与项目既有约定保持一致（见 store/index.js 的 avatarUrl 与 TopicDetail 的 avatarUrl）。
 */
function imageUrl(path) {
    if (!path) return ''
    if (/^https?:\/\//i.test(path)) return path
    const base = axios.defaults.baseURL || ''
    // 已经带了 /images 的不重复叠加，避免出现 /images/images/xxx
    return path.startsWith('/images/') ? base + path : base + '/images' + path
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
    loadPage(1, true)
    apiForumTopTopics(data => topTopics.value = data || [])
    nextTick(setupListObserver)
})

// keep-alive 回来时如果底部 sentinel 仍然可见，补一次加载
onActivated(() => {
    nextTick(maybeLoadMore)
})

onBeforeUnmount(() => {
    listObserver?.disconnect()
})

watch(() => [route.query.keyword, route.query.type, route.meta.typeCode, route.path], () => {
    resetList()
})

const friendLinks = [
    {src: "https://www.itbaima.cn/image/welcome/outsource/image-2.webp", url: "https://www.itbaima.cn/zh-CN"},
    {src: "https://www.itbaima.cn/image/welcome/outsource/image-1.webp", url: "https://www.itbaima.cn/zh-CN"},
    {src: "https://www.itbaima.cn/image/welcome/outsource/image-3.webp", url: "https://www.itbaima.cn/zh-CN"}
];

const openLink = (url) => {
    window.open(url, '_blank');
};

function readSavedCity() {
    try {
        const saved = JSON.parse(localStorage.getItem(CITY_CACHE_KEY) || 'null')
        if (saved && saved.id) return saved
    } catch (e) {
        // 隐私模式下 localStorage 不可用，忽略即可
    }
    return null
}

function saveCity(city) {
    if (!city || !city.id) return
    try {
        localStorage.setItem(CITY_CACHE_KEY, JSON.stringify({
            id: city.id,
            name: city.name || '',
            adm2: city.adm2 || ''
        }))
    } catch (e) {
        // 写入失败不影响本次展示
    }
}

function applyWeather(data) {
    if (!data) return
    Object.assign(weather, data)
    weather.success = true
    weather.failed = false
    weather.error = ''
    // 记住这次解析出的城市：下次直接按 id 取，不再被浏览器定位带到别的城市
    if (data.location && data.location.id) saveCity(data.location)
}

function weatherFailed(message) {
    weather.success = false
    weather.failed = true
    weather.error = typeof message === 'string' ? message : (message && message.message) || '天气信息加载失败'
}

function weatherRequestError(error) {
    const status = error && error.response && error.response.status
    if (status === 404) {
        weatherFailed('后端还没有城市天气接口，请重启后端后再试')
        return
    }
    const timeout = error && (error.code === 'ECONNABORTED' || (error.message || '').includes('timeout'))
    weatherFailed(timeout ? '天气请求超时，请重试' : '无法连接天气服务，请确认后端已启动')
}

function requestWeatherByCity(id) {
    apiForumWeatherCity(id,
        applyWeather, weatherFailed, weatherRequestError)
}

function requestWeatherByCoords(longitude, latitude) {
    apiForumWeather(longitude, latitude,
        applyWeather, weatherFailed, weatherRequestError)
}

function useDefaultCity(message) {
    if (message) ElMessage.warning(message)
    requestWeatherByCity(DEFAULT_CITY.id)
}

function locateAndLoadWeather() {
    weather.failed = false
    weather.error = ''
    const saved = readSavedCity()
    if (saved) {
        requestWeatherByCity(saved.id)
        return
    }
    if (!navigator.geolocation) {
        useDefaultCity('浏览器不支持定位，已显示默认城市天气，可点「切换城市」')
        return
    }
    navigator.geolocation.getCurrentPosition(position => {
        const {longitude, latitude} = position.coords
        // 有些环境会给出 0,0 或非法值，这种坐标查不到城市，直接回退
        if (!Number.isFinite(longitude) || !Number.isFinite(latitude)
            || (longitude === 0 && latitude === 0)) {
            useDefaultCity('定位结果不可用，已显示默认城市天气，可点「切换城市」')
            return
        }
        requestWeatherByCoords(longitude, latitude)
    }, () => {
        // Chrome 的定位服务在国内经常不可用，定位失败是常态，不再当成错误
        useDefaultCity('定位不可用，已显示默认城市天气，可点「切换城市」手动选择')
    }, {
        timeout: 8000,
        enableHighAccuracy: false,
        maximumAge: 600000
    })
}

function retryWeather() {
    locateAndLoadWeather()
}

function openCityPicker() {
    cityDialog.value = true
    cityKeyword.value = ''
    cityResults.value = []
    citySearched.value = false
}

function searchCity() {
    const keyword = cityKeyword.value.trim()
    if (!keyword || citySearching.value) return
    citySearching.value = true
    citySearched.value = false
    apiForumCitySearch(keyword, data => {
        cityResults.value = Array.isArray(data) ? data : []
        citySearching.value = false
        citySearched.value = true
    }, message => {
        citySearching.value = false
        citySearched.value = true
        ElMessage.warning(message)
    }, () => {
        citySearching.value = false
        citySearched.value = true
        ElMessage.error('城市检索失败，请确认后端已重启')
    })
}

function chooseCity(city) {
    saveCity(city)
    cityDialog.value = false
    ElMessage.success(`已切换到 ${city.name}`)
    requestWeatherByCity(city.id)
}

locateAndLoadWeather()

apiForumIp(
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
            <div class="board-header">
                <div class="board-title">{{ pageTitle }}</div>
                <div v-if="pageHint" class="board-hint">{{ pageHint }}</div>
            </div>
            <light-card>
                <div class="creat-topic" @click="editor=true">
                    <el-icon>
                        <EditPen/>
                    </el-icon>
                    {{ typeCode ? '点击发布' + pageTitle + '...' : '点击发表主题...' }}
                </div>
            </light-card>
            <light-card v-if="!typeCode && store.forum.types.length" class="type-tabs" style="margin-top: 10px">
                <div v-for="item in store.forum.types" :key="item.id"
                     class="type-chip"
                     :class="{active: selectedType === item.id}"
                     @click="selectType(item.id)">
                    <span class="type-dot" :style="{background: item.color}"></span>
                    {{ item.name }}
                </div>
            </light-card>
            <light-card v-if="!typeCode && topTopics.length" style="margin-top: 10px">
                <div class="top-topic-row" v-for="item in topTopics" :key="item.id" @click="goDetail(item.id)">
                    <el-tag type="danger" size="small">置顶</el-tag>
                    <span class="top-topic-title">{{ item.title }}</span>
                    <span class="top-topic-time">{{ formatTime(item.time) }}</span>
                </div>
            </light-card>
            <div style="margin: 10px 0;display: flex;flex-direction: column;gap: 10px" v-loading="loading">
                <div v-if="topics.length === 0 && !loading" style="text-align: center;color: grey;padding: 40px 0">
                    {{ typeCode ? '还没有' + pageTitle + '，来发第一条吧！' : '还没有人发帖，快来发表第一个帖子吧！' }}
                </div>
                <div v-for="topic in topics" :key="topic.id" class="topic-card" @click="goDetail(topic.id)">
                    <light-card style="height: 100%">
                        <div class="topic-body">
                            <div class="topic-main">
                                <div class="topic-author">
                                    <el-avatar v-if="topic.avatar" :size="28" :src="imageUrl(topic.avatar)">
                                        {{ topic.username?.charAt(0) }}
                                    </el-avatar>
                                    <el-icon v-else><ChatDotSquare/></el-icon>
                                    <div class="topic-author-main">
                                        <div class="topic-author-name">{{ topic.username }}</div>
                                        <div class="topic-author-time">{{ formatTime(topic.time) }}</div>
                                    </div>
                                </div>
                                <div class="topic-header">
                                    <el-tag v-if="topic.top" type="danger" size="small" class="top-tag">置顶</el-tag>
                                    <div class="topic-title">{{ topic.title }}</div>
                                </div>
                                <!-- 摘要由后端提取为纯文本，所以这里不会出现正文里的图片 -->
                                <div v-if="topic.preview" class="topic-preview">{{ topic.preview }}</div>
                            </div>
                            <!-- 封面同样来自后端：只取正文第一张图，不会把整篇正文的图都加载出来 -->
                            <div v-if="topic.cover" class="topic-cover">
                                <el-image :src="imageUrl(topic.cover)" fit="cover" lazy/>
                            </div>
                        </div>
                        <div class="topic-meta">
                            <span class="meta-item">赞 {{ topic.likeCount || 0 }} · 藏 {{ topic.collectCount || 0 }}</span>
                            <topic-tag :type="topic.type"/>
                            <el-tag v-if="topic.typeCode === 'lost' && topic.extra && topic.extra.status === 'resolved'"
                                    type="success" size="small">已找回</el-tag>
                            <el-tag v-if="topic.locked === 1" type="warning" size="small">已锁定</el-tag>
                        </div>
                    </light-card>
                </div>
                <div ref="listSentinel" class="list-sentinel" aria-hidden="true"></div>
            </div>
            <div v-if="topics.length > 0 || loadError" class="list-footer">
                <template v-if="loadingMore">
                    <el-icon class="is-loading"><Loading/></el-icon>
                    <span>正在加载...</span>
                </template>
                <template v-else-if="loadError">
                    <span>{{ loadError }}</span>
                    <el-button link type="primary" @click="retryLoadMore">重试</el-button>
                </template>
                <template v-else-if="!hasMore">
                    <span>没有更多了 · 共 {{ total }} 篇</span>
                </template>
                <template v-else>
                    <el-button link type="primary" @click="loadPage(page, false)">加载更多</el-button>
                </template>
            </div>
        </div>
        <div style="width: 280px">
            <div style="position: sticky;top: 20px">
                <light-card>
                    <div class="collect-entry" @click="router.push('/index/collects')">
                        <span><el-icon><Star/></el-icon> 查看我的收藏</span>
                        <el-icon><ArrowRightBold/></el-icon>
                    </div>
                </light-card>
                <light-card style="margin-top: 10px">
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
                    <div style="display: flex; justify-content: space-between; align-items: center">
                        <div style="font-weight: bold">
                            <el-icon>
                                <Calendar/>
                            </el-icon>
                            天气信息
                        </div>
                        <el-button link type="primary" size="small" @click="openCityPicker">切换城市</el-button>
                    </div>
                    <el-divider style="margin: 10px 0"/>
                    <weather :data="weather" :failed="weather.failed" @retry="retryWeather"/>
                </light-card>

                <el-dialog v-model="cityDialog" title="选择城市" width="360px" append-to-body>
                    <el-input v-model="cityKeyword" placeholder="输入城市名，如 郑州" clearable
                              @keyup.enter="searchCity">
                        <template #append>
                            <el-button :loading="citySearching" @click="searchCity">搜索</el-button>
                        </template>
                    </el-input>
                    <div v-if="cityResults.length" class="city-results">
                        <div v-for="city in cityResults" :key="city.id" class="city-item"
                             @click="chooseCity(city)">
                            <span class="city-name">{{ city.name }}</span>
                            <span class="city-adm">{{ city.adm1 }} {{ city.adm2 }}</span>
                        </div>
                    </div>
                    <div v-else-if="citySearched && !citySearching" class="city-empty">没有找到匹配的城市</div>
                </el-dialog>
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
        <topic-editor :show="editor" :default-type="typeCode" @close="editor=false" @submit="handleSubmit"/>
    </div>
</template>

<style lang="less" scoped>
.info-text {
    display: flex;
    justify-content: space-between;
    color: grey;
    font-size: 14px;
}

.type-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 7px;
    padding: 10px 14px;
}

.type-chip {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 2px 7px;
    border: 1px solid transparent;
    border-radius: 3px;
    background: #f5f5f5;
    color: var(--el-text-color-regular);
    font-size: 14px;
    line-height: 20px;
    box-sizing: border-box;
    cursor: pointer;
    transition: background-color .3s;
    user-select: none;
}

.type-chip:hover {
    background: #dadada;
}

.type-chip.active {
    border-color: #ead4c4;
}

.type-chip .type-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex: none;
    box-shadow: inset 0 0 0 1px rgba(0, 0, 0, .08);
}

:global(.dark) .type-chip {
    background: #282828;
}

:global(.dark) .type-chip:hover {
    background: #5e5e5e;
}

:global(.dark) .type-chip.active {
    border-color: #64594b;
}

.collect-entry {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 14px;
    color: var(--el-text-color-regular);
    cursor: pointer;
    transition: opacity .2s;
}

.collect-entry:hover {
    opacity: 0.6;
}

.top-topic-row {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 4px 0;
    font-size: 14px;
    cursor: pointer;
}

.top-topic-row:hover {
    color: var(--el-color-primary);
}

.top-topic-title {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.top-topic-time {
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.board-header {
    margin-bottom: 12px;
}

.board-title {
    font-size: 20px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.board-hint {
    margin-top: 4px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
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

.topic-author {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
}

.topic-author-main {
    display: flex;
    flex-direction: column;
    line-height: 1.3;
}

.topic-author-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.topic-author-time {
    font-size: 12px;
    color: var(--el-text-color-secondary);
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

.topic-body {
    display: flex;
    gap: 12px;
    align-items: flex-start;
}

.topic-main {
    flex: 1;
    /* 必须允许收缩，否则长标题会把右侧缩略图挤出卡片 */
    min-width: 0;
}

.topic-cover {
    flex: none;
    /* 固定尺寸：预留空间可避免图片加载完成后发生布局跳动（CLS） */
    width: 108px;
    height: 76px;
    border-radius: 6px;
    overflow: hidden;
    background: var(--el-fill-color-light);
}

.topic-cover .el-image {
    display: block;
    width: 100%;
    height: 100%;
}

.topic-preview {
    /* 两行截断：摘要长度已由后端限制，这里只做视觉收口 */
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin-top: 6px;
    font-size: 13px;
    line-height: 1.6;
    color: var(--el-text-color-regular);
}

.top-tag {
    flex-shrink: 0;
    margin-right: 6px;
}

.topic-meta {
    display: flex;
    align-items: center;
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

.list-sentinel {
    height: 1px;
}

.list-footer {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 16px 0 8px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
}

.list-footer .el-button {
    font-size: 13px;
}

.city-results {
    margin-top: 12px;
    max-height: 280px;
    overflow-y: auto;
}

.city-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 6px;
    cursor: pointer;
    border-bottom: 1px solid var(--el-border-color-lighter);
}

.city-item:hover {
    background: var(--el-fill-color-light);
}

.city-name {
    font-size: 14px;
}

.city-adm {
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.city-empty {
    margin-top: 16px;
    text-align: center;
    color: var(--el-text-color-secondary);
    font-size: 13px;
}

@media (max-width: 640px) {
    .topic-cover {
        width: 88px;
        height: 64px;
    }
}
</style>
