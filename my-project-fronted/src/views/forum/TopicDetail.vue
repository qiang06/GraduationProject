<script setup>
import LightCard from "@/components/LightCard.vue";
import TopicEditor from "@/components/TopicEditor.vue";
import TopicTag from "@/components/TopicTag.vue";
import CommentEditor from "@/components/CommentEditor.vue";
import CommentItem from "@/components/CommentItem.vue";
import {ArrowLeft, ChatDotSquare, CollectionTag, Delete, Edit, EditPen, Female, Male, Star} from "@element-plus/icons-vue";
import {ref, computed, onMounted, onBeforeUnmount, nextTick, reactive, watch} from "vue";
import {useRoute, useRouter} from "vue-router";
import {useStore} from "@/store";
import {
    apiForumCollect,
    apiForumCommentDelete,
    apiForumComments,
    apiForumDelete,
    apiForumDetail,
    apiForumInteraction,
    apiForumLike,
    apiForumStatus,
    apiForumUpdate
} from "@/net/api";
import {ElMessage, ElMessageBox} from "element-plus";
import axios from "axios";

const route = useRoute()
const router = useRouter()
const store = useStore()

const topic = ref(null)
const loading = ref(true)
const editor = ref(false)
const editData = ref(null)

// 文章标题滑出视口后，顶部出现吸顶导航条
const titleRef = ref(null)
const showBanner = ref(false)
let bannerObserver = null

function setupBannerObserver() {
    if (!titleRef.value || typeof IntersectionObserver === 'undefined') return
    bannerObserver?.disconnect()
    bannerObserver = new IntersectionObserver(([entry]) => {
        showBanner.value = !entry.isIntersecting
    }, {threshold: 0})
    bannerObserver.observe(titleRef.value)
}

const comments = ref([])
const commentTotal = ref(0)
const commentRootTotal = ref(0)
const commentPages = ref(0)
const commentPage = ref(1)
const commentLoading = ref(false)
const COMMENT_PAGE_SIZE = 10

// 发评论和回复共用一个富文本抽屉；quote 为 null 表示顶层评论
const commentEditor = reactive({show: false, quote: null})

// 作者在「账号安全设置」里隐藏的字段，后端会下发 null；统一用这句话提示
const HIDDEN_TEXT = '已隐藏或未填写'
const interaction = ref({likeCount: 0, collectCount: 0, liked: false, collected: false})
const interactionLoading = ref(false)

function loadDetail() {
    const id = route.params.id
    loading.value = true
    apiForumDetail(id, (data) => {
        topic.value = data
        loading.value = false
    }, (message) => {
        ElMessage.warning(message)
        loading.value = false
        router.push('/index')
    })
}

function loadInteraction() {
    apiForumInteraction(route.params.id, data => interaction.value = data)
}

function toggleInteraction(kind) {
    interactionLoading.value = true
    const action = kind === 'like' ? apiForumLike : apiForumCollect
    action(Number(route.params.id), data => {
        interaction.value = data
        interactionLoading.value = false
    }, message => {
        ElMessage.warning(message)
        interactionLoading.value = false
    })
}

function loadComments() {
    const id = route.params.id
    commentLoading.value = true
    apiForumComments(id, commentPage.value, (data) => {
        comments.value = data.items || []
        commentRootTotal.value = data.total || 0
        commentTotal.value = data.allTotal || 0
        commentPages.value = data.pages || 0
        commentLoading.value = false
    }, (message) => {
        ElMessage.warning(message)
        commentLoading.value = false
    })
}

function changeCommentPage(page) {
    commentPage.value = page
    loadComments()
}

function openCommentEditor(quote = null) {
    commentEditor.quote = quote
    commentEditor.show = true
}

function onCommentSubmitted() {
    const wasReply = !!commentEditor.quote
    commentEditor.show = false
    commentEditor.quote = null
    if (wasReply) {
        // 回复挂当前页的顶层评论下，刷新当前页即可
        loadComments()
    } else {
        // 顶层评论按时间正序，新评论在最后一页
        commentPage.value = Math.max(1, Math.ceil((commentRootTotal.value + 1) / COMMENT_PAGE_SIZE))
        loadComments()
    }
}

function deleteComment(id) {
    ElMessageBox.confirm('确定要删除这条评论吗？它下面的回复也会一起删除。', '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        const wasRoot = comments.value.some(c => c.id === id)
        apiForumCommentDelete(id, () => {
            ElMessage.success('删除成功！')
            // 只有删掉最后一页最后一条顶层评论时才回退页码；
            // 删回复不会减少顶层评论数，不能误退页
            if (wasRoot && comments.value.length === 1 && commentPage.value > 1) {
                commentPage.value--
            }
            loadComments()
        }, (message) => {
            ElMessage.warning(message)
        })
    }).catch(() => {})
}

function avatarUrl(user) {
    if (user?.avatar) {
        return axios.defaults.baseURL + '/images' + user.avatar
    }
    return ''
}

function formatTime(time) {
    if (!time) return ''
    const d = new Date(time)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hour = String(d.getHours()).padStart(2, '0')
    const minute = String(d.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hour}:${minute}`
}

function isOwner() {
    return topic.value && store.user.id === topic.value.uid
}

function isLost() {
    return !!topic.value && topic.value.typeCode === 'lost'
}

function isResolved() {
    return !!(topic.value && topic.value.extra && topic.value.extra.status === 'resolved')
}

// 详情页的结构化信息：失物招领看物品/地点，校园活动看时间/地点
const extraRows = computed(() => {
    const data = topic.value && topic.value.extra
    if (!data) return []
    const rows = isLost()
        ? [['物品', data.item], ['地点', data.place], ['时间', data.happenedAt], ['联系方式', data.contact]]
        : topic.value.typeCode === 'activity'
            ? [['开始时间', data.startAt], ['结束时间', data.endAt], ['活动地点', data.place], ['主办方', data.organizer]]
            : []
    return rows.filter(([, value]) => value).map(([label, value]) => ({label, value}))
})

function toggleStatus() {
    const next = isResolved() ? 'open' : 'resolved'
    apiForumStatus(topic.value.id, next, () => {
        ElMessage.success(next === 'resolved' ? '已标记为已找回' : '已撤销「已找回」')
        loadDetail()
    }, message => {
        ElMessage.warning(message)
    })
}

function deltaToHtml(content) {
    if (!content) return ''
    if (!content.startsWith('{"ops":')) return content
    try {
        const delta = JSON.parse(content)
        return delta.ops.map(op => {
            if (typeof op.insert === 'string') {
                return op.insert.replace(/\n/g, '<br/>')
            }
            if (op.insert && op.insert.image) {
                return `<img src="${op.insert.image}" style="max-width:100%"/>`
            }
            return ''
        }).join('')
    } catch {
        return content
    }
}

const renderedContent = computed(() => {
    return topic.value ? deltaToHtml(topic.value.content) : ''
})

function openEditor() {
    editData.value = {
        id: topic.value.id,
        title: topic.value.title,
        type: topic.value.type,
        content: topic.value.content,
        extra: topic.value.extra || {}
    }
    editor.value = true
}

function handleUpdate(data) {
    apiForumUpdate(data, () => {
        ElMessage.success('修改成功！')
        editor.value = false
        editData.value = null
        loadDetail()
    }, (message) => {
        ElMessage.warning(message)
    })
}

function handleDelete() {
    ElMessageBox.confirm('确定要删除这篇帖子吗？删除后无法恢复。', '确认删除', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        apiForumDelete(topic.value.id, () => {
            ElMessage.success('删除成功！')
            router.push('/index')
        }, (message) => {
            ElMessage.warning(message)
        })
    }).catch(() => {})
}

function goBack() {
    router.push('/index')
}

watch(topic, (value) => {
    showBanner.value = false
    if (value) nextTick(setupBannerObserver)
})

onMounted(() => {
    loadDetail()
    loadInteraction()
    loadComments()
    nextTick(setupBannerObserver)
})

onBeforeUnmount(() => {
    bannerObserver?.disconnect()
})
</script>

<template>
    <div class="detail-page">
        <aside v-if="topic && topic.author" class="author-panel">
            <light-card>
                <div class="author-panel-avatar">
                    <el-avatar :size="72" :src="avatarUrl(topic.author)"/>
                </div>
                <div class="author-panel-name">
                    {{ topic.author.username }}
                    <el-icon v-if="topic.author.gender === 1" style="color: hotpink"><Female/></el-icon>
                    <el-icon v-else-if="topic.author.gender === 0" style="color: dodgerblue"><Male/></el-icon>
                </div>
                <div class="author-panel-email">{{ topic.author.email || HIDDEN_TEXT }}</div>
                <el-divider style="margin: 12px 0"/>
                <div class="author-contact">
                    <span>微信号</span>
                    <span>{{ topic.author.wx || HIDDEN_TEXT }}</span>
                </div>
                <div class="author-contact">
                    <span>QQ号</span>
                    <span>{{ topic.author.qq || HIDDEN_TEXT }}</span>
                </div>
                <div class="author-contact">
                    <span>手机号</span>
                    <span>{{ topic.author.phone || HIDDEN_TEXT }}</span>
                </div>
                <el-divider style="margin: 12px 0"/>
                <div class="author-panel-desc">{{ topic.author.desc || '未填写' }}</div>
            </light-card>
        </aside>
        <div class="detail-main" v-loading="loading">
            <div v-if="topic" class="banner-slot">
                <transition name="el-fade-in-linear">
                    <div v-show="showBanner" class="topic-banner">
                        <el-button :icon="ArrowLeft" size="small" plain round @click="goBack">返回列表</el-button>
                        <div class="topic-banner-main">
                            <el-tag v-if="topic.locked === 1" type="warning" size="small">已锁定</el-tag>
                            <topic-tag :type="topic.type"/>
                            <span class="topic-banner-title">{{ topic.title }}</span>
                        </div>
                    </div>
                </transition>
            </div>
            <div v-if="topic" style="display: flex;flex-direction: column;gap: 10px">
                <div class="back-bar" @click="goBack">
                    <el-icon><ArrowLeft/></el-icon>
                    返回帖子列表
                </div>
                <light-card>
                    <div ref="titleRef" class="detail-title">{{ topic.title }}</div>
                    <div class="detail-meta">
                        <span>{{ formatTime(topic.time) }}</span>
                        <el-divider direction="vertical"/>
                        <el-tag v-if="topic.top" type="danger" size="small">置顶</el-tag>
                        <el-tag v-if="topic.locked === 1" type="warning" size="small">已锁定</el-tag>
                        <topic-tag :type="topic.type"/>
                        <el-tag v-if="topic.typeCode === 'confession'" type="warning" size="small">匿名</el-tag>
                        <el-tag v-if="isLost() && isResolved()" type="success" size="small">已找回</el-tag>
                    </div>
                    <el-divider style="margin: 12px 0"/>
                    <div class="detail-content" v-html="renderedContent"/>
                    <div v-if="extraRows.length" class="extra-box">
                        <div v-for="row in extraRows" :key="row.label" class="extra-row">
                            <span class="extra-label">{{ row.label }}</span>
                            <span class="extra-value">{{ row.value }}</span>
                        </div>
                    </div>
                    <el-divider style="margin: 16px 0"/>
                    <div class="detail-actions">
                        <el-button :type="interaction.liked ? 'primary' : 'default'" :icon="Star" :loading="interactionLoading" @click="toggleInteraction('like')" plain>
                            {{ interaction.liked ? '已点赞' : '点赞' }} {{ interaction.likeCount }}
                        </el-button>
                        <el-button :type="interaction.collected ? 'warning' : 'default'" :icon="CollectionTag" :loading="interactionLoading" @click="toggleInteraction('collect')" plain>
                            {{ interaction.collected ? '已收藏' : '收藏' }} {{ interaction.collectCount }}
                        </el-button>
                    </div>
                    <div v-if="isOwner()" class="detail-actions owner-actions">
                        <el-button v-if="isLost()" :type="isResolved() ? 'success' : 'warning'"
                                   @click="toggleStatus" plain size="small">
                            {{ isResolved() ? '已找回（点击撤销）' : '标记为已找回' }}
                        </el-button>
                        <el-button v-if="topic.locked !== 1" type="primary" :icon="Edit" @click="openEditor" plain size="small">
                            编辑
                        </el-button>
                        <el-button type="danger" :icon="Delete" @click="handleDelete" plain size="small">
                            删除
                        </el-button>
                    </div>
                </light-card>

                <light-card>
                    <div class="comment-section-title">
                        <el-icon><ChatDotSquare/></el-icon>
                        评论 ({{ commentTotal }})
                    </div>
                    <el-divider style="margin: 10px 0"/>

                    <div v-if="topic.locked !== 1" class="comment-entry" @click="openCommentEditor()">
                        <el-icon><EditPen/></el-icon>
                        <span>写下你的评论...（支持加粗、列表、链接和图片）</span>
                    </div>
                    <div v-else class="locked-hint">该帖子已被管理员锁定，暂时不能评论</div>

                    <div v-loading="commentLoading" style="min-height:60px">
                        <div v-if="comments.length === 0 && !commentLoading" class="comment-empty">
                            暂无评论，来发表第一条吧
                        </div>
                        <div v-else class="comment-list">
                            <comment-item v-for="comment in comments"
                                          :key="comment.id"
                                          :comment="comment"
                                          :depth="1"
                                          :topic="topic"
                                          :locked="topic.locked === 1"
                                          @reply="openCommentEditor"
                                          @delete="deleteComment"/>
                        </div>
                    </div>
                    <div v-if="commentPages > 1" class="comment-pagination">
                        <el-pagination v-model:current-page="commentPage"
                                       :page-size="COMMENT_PAGE_SIZE"
                                       :total="commentRootTotal"
                                       layout="prev, pager, next"
                                       background
                                       @current-change="changeCommentPage"/>
                    </div>
                </light-card>
            </div>
            <div v-else-if="!loading" style="text-align:center;color:grey;padding:40px 0">
                帖子不存在或已被删除
            </div>
        </div>
        <comment-editor
            :show="commentEditor.show"
            :tid="topic ? topic.id : null"
            :quote="commentEditor.quote"
            @close="commentEditor.show = false"
            @submitted="onCommentSubmitted"
        />
        <topic-editor
            :show="editor"
            :edit-topic="editData"
            @close="editor=false; editData=null"
            @update="handleUpdate"
            @submit="() => {}"
        />
    </div>
</template>

<style lang="less" scoped>
.back-bar {
    display: flex;
    align-items: center;
    gap: 4px;
    color: grey;
    font-size: 14px;
    cursor: pointer;
    padding: 4px 0;

    &:hover {
        color: var(--el-color-primary);
    }
}

.detail-title {
    font-size: 22px;
    font-weight: 700;
    line-height: 1.4;
    color: var(--el-text-color-primary);
}

.detail-page {
    display: flex;
    align-items: flex-start;
    gap: 20px;
    margin: 20px auto;
    max-width: 960px;
    padding: 0 16px;
}

.detail-main {
    flex: 1;
    min-width: 0;
}

/* 标题滚出视口后出现：slot 高度为 0 + sticky，避免出现/消失时把正文顶动 */
.banner-slot {
    position: sticky;
    top: 0;
    z-index: 30;
    height: 0;
}

.topic-banner {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 12px;
    box-sizing: border-box;
    background: var(--el-bg-color);
    border-bottom: 1px solid var(--el-border-color-lighter);
    border-radius: 0 0 12px 12px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, .08);
}

.topic-banner-main {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
}

.topic-banner-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

@media (max-width: 760px) {
    .topic-banner-title {
        font-size: 13px;
    }
}

.author-panel {
    width: 220px;
    flex: none;
    position: sticky;
    top: 20px;
}

.author-panel-avatar {
    display: flex;
    justify-content: center;
    margin-bottom: 10px;
}

.author-panel-name {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 5px;
    font-size: 18px;
    font-weight: 700;
    color: var(--el-text-color-primary);
}

.author-panel-email {
    margin-top: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    text-align: center;
    word-break: break-all;
}

.author-contact {
    display: flex;
    justify-content: space-between;
    gap: 8px;
    padding: 3px 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.author-contact span:last-child {
    color: var(--el-text-color-regular);
    text-align: right;
    word-break: break-all;
}

.author-panel-desc {
    font-size: 13px;
    line-height: 1.6;
    color: var(--el-text-color-regular);
    word-break: break-all;
}

@media (max-width: 760px) {
    .detail-page {
        flex-direction: column;
    }

    .author-panel {
        width: 100%;
        position: static;
    }
}

.detail-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 12px;
    font-size: 13px;
    color: grey;
}

.detail-content {
    font-size: 15px;
    line-height: 1.8;
    color: var(--el-text-color-regular);

    :deep(img) {
        max-width: 100%;
        border-radius: 4px;
    }

    :deep(p) {
        margin: 8px 0;
    }

    :deep(blockquote) {
        border-left: 4px solid var(--el-border-color);
        padding-left: 12px;
        color: grey;
        margin: 12px 0;
    }

    :deep(pre) {
        background-color: var(--el-fill-color-light);
        border-radius: 4px;
        padding: 12px;
        overflow-x: auto;
    }
}

.detail-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
}

.locked-hint {
    margin-bottom: 16px;
    padding: 10px 12px;
    border-radius: 6px;
    font-size: 13px;
    color: var(--el-color-warning);
    background: var(--el-color-warning-light-9);
}

.extra-box {
    margin-top: 16px;
    padding: 12px 14px;
    border-radius: 6px;
    background: var(--el-fill-color-lighter);
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.extra-row {
    display: flex;
    gap: 12px;
    font-size: 14px;
}

.extra-label {
    flex: none;
    width: 72px;
    color: var(--el-text-color-secondary);
}

.extra-value {
    color: var(--el-text-color-primary);
    word-break: break-all;
}

.comment-section-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    font-size: 15px;
}

.comment-entry {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    padding: 10px 12px;
    border: 1px dashed var(--el-border-color);
    border-radius: 6px;
    color: var(--el-text-color-secondary);
    font-size: 14px;
    cursor: pointer;
    transition: all .2s;

    &:hover {
        border-color: var(--el-color-primary);
        color: var(--el-color-primary);
        background: var(--el-fill-color-lighter);
    }
}

.comment-pagination {
    display: flex;
    justify-content: center;
    margin-top: 16px;
}

.comment-empty {
    text-align: center;
    color: grey;
    font-size: 14px;
    padding: 24px 0;
}

.comment-list {
    display: flex;
    flex-direction: column;
}

.comment-item {
    padding: 12px 0;
    border-bottom: 1px solid var(--el-border-color-lighter);

    &:last-child {
        border-bottom: none;
    }
}

.comment-body, .reply-item {
    display: flex;
    gap: 10px;
}

.comment-avatar {
    flex-shrink: 0;
}

.comment-main {
    flex: 1;
    min-width: 0;
}

.comment-header {
    display: flex;
    align-items: center;
    gap: 8px;
}

.comment-author {
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.comment-time {
    font-size: 12px;
    color: grey;
}

.comment-text {
    font-size: 14px;
    line-height: 1.6;
    color: var(--el-text-color-regular);
    margin: 4px 0;
    word-break: break-all;
}

.comment-actions {
    display: flex;
    gap: 12px;
    margin-top: 2px;
}

.comment-action-btn {
    font-size: 12px;
    color: grey;
    cursor: pointer;

    &:hover {
        color: var(--el-color-primary);
    }

    &.danger:hover {
        color: var(--el-color-danger);
    }
}

.comment-replies {
    margin-top: 8px;
    margin-left: 42px;
    padding: 8px 12px;
    background-color: var(--el-fill-color-lighter);
    border-radius: 4px;
}

.reply-item {
    padding: 8px 0;

    &:first-child {
        padding-top: 0;
    }

    &:last-child {
        padding-bottom: 0;
    }
}

.reply-input-area {
    margin-top: 8px;

    .reply-input-action {
        display: flex;
        justify-content: flex-end;
        gap: 8px;
        margin-top: 6px;
    }
}
</style>
