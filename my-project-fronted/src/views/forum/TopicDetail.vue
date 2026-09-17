<script setup>
import LightCard from "@/components/LightCard.vue";
import TopicEditor from "@/components/TopicEditor.vue";
import {ArrowLeft, ChatDotSquare, CollectionTag, Delete, Edit, Star} from "@element-plus/icons-vue";
import {ref, computed, onMounted} from "vue";
import {useRoute, useRouter} from "vue-router";
import {useStore} from "@/store";
import {get, post} from "@/net";
import {ElMessage, ElMessageBox} from "element-plus";
import axios from "axios";

const route = useRoute()
const router = useRouter()
const store = useStore()

const topic = ref(null)
const loading = ref(true)
const editor = ref(false)
const editData = ref(null)

const comments = ref([])
const commentTotal = ref(0)
const commentPages = ref(0)
const commentPage = ref(1)
const commentLoading = ref(false)

const newComment = ref('')
const commentSubmitting = ref(false)

const replyTarget = ref(null)
const replyContent = ref('')
const replySubmitting = ref(false)
const interaction = ref({likeCount: 0, collectCount: 0, liked: false, collected: false})
const interactionLoading = ref(false)

function loadDetail() {
    const id = route.params.id
    loading.value = true
    get(`/api/forum/detail?id=${id}`, (data) => {
        topic.value = data
        loading.value = false
    }, (message) => {
        ElMessage.warning(message)
        loading.value = false
        router.push('/index')
    })
}

function loadInteraction() {
    get(`/api/forum/interaction?tid=${route.params.id}`, data => interaction.value = data)
}

function toggleInteraction(kind) {
    interactionLoading.value = true
    post(`/api/forum/interaction/${kind}`, {id: Number(route.params.id)}, data => {
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
    get(`/api/forum/comments?tid=${id}&page=${commentPage.value}`, (data) => {
        comments.value = data.items || []
        commentTotal.value = data.total || 0
        commentPages.value = data.pages || 0
        commentLoading.value = false
    }, (message) => {
        ElMessage.warning(message)
        commentLoading.value = false
    })
}

function submitComment() {
    if (!newComment.value.trim()) return
    commentSubmitting.value = true
    post('/api/forum/comment', {
        tid: topic.value.id,
        content: newComment.value
    }, (data) => {
        ElMessage.success('评论成功！')
        newComment.value = ''
        commentPage.value = 1
        loadComments()
        commentSubmitting.value = false
    }, (message) => {
        ElMessage.warning(message)
        commentSubmitting.value = false
    })
}

function startReply(comment) {
    replyTarget.value = comment.id
    replyContent.value = ''
}

function cancelReply() {
    replyTarget.value = null
    replyContent.value = ''
}

function submitReply() {
    if (!replyContent.value.trim()) return
    replySubmitting.value = true
    post('/api/forum/comment', {
        tid: topic.value.id,
        content: replyContent.value,
        quote: replyTarget.value
    }, (data) => {
        ElMessage.success('回复成功！')
        replyTarget.value = null
        replyContent.value = ''
        commentPage.value = 1
        loadComments()
        replySubmitting.value = false
    }, (message) => {
        ElMessage.warning(message)
        replySubmitting.value = false
    })
}

function deleteComment(id) {
    ElMessageBox.confirm('确定要删除这条评论吗？', '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        post('/api/forum/comment-delete', { id }, () => {
            ElMessage.success('删除成功！')
            loadComments()
        }, (message) => {
            ElMessage.warning(message)
        })
    }).catch(() => {})
}

function canDelete(comment) {
    return store.user.id === comment.uid || (topic.value && store.user.id === topic.value.uid)
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
        content: topic.value.content
    }
    editor.value = true
}

function handleUpdate(data) {
    post('/api/forum/update', data, () => {
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
        post('/api/forum/delete', { id: topic.value.id }, () => {
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

onMounted(() => {
    loadDetail()
    loadInteraction()
    loadComments()
})
</script>

<template>
    <div style="display: flex;margin: 20px auto;gap: 20px;max-width: 900px">
        <div style="flex: 1" v-loading="loading">
            <div v-if="topic" style="display: flex;flex-direction: column;gap: 10px">
                <div class="back-bar" @click="goBack">
                    <el-icon><ArrowLeft/></el-icon>
                    返回帖子列表
                </div>
                <light-card>
                    <div class="detail-title">{{ topic.title }}</div>
                    <div class="detail-meta">
                        <span>{{ topic.username }}</span>
                        <el-divider direction="vertical"/>
                        <span>{{ formatTime(topic.time) }}</span>
                        <el-divider direction="vertical"/>
                        <el-tag v-if="topic.top" type="danger" size="small">置顶</el-tag>
                    </div>
                    <el-divider style="margin: 12px 0"/>
                    <div class="detail-content" v-html="renderedContent"/>
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
                        <el-button type="primary" :icon="Edit" @click="openEditor" plain size="small">
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
                        评论 ({{ comments.length }})
                    </div>
                    <el-divider style="margin: 10px 0"/>

                    <div class="comment-input-area">
                        <el-input
                            v-model="newComment"
                            type="textarea"
                            :rows="2"
                            placeholder="写下你的评论..."
                            maxlength="500"
                            show-word-limit
                        />
                        <div class="comment-input-action">
                            <el-button
                                type="primary"
                                size="small"
                                :loading="commentSubmitting"
                                @click="submitComment"
                                :disabled="!newComment.trim()">
                                发表评论
                            </el-button>
                        </div>
                    </div>

                    <div v-loading="commentLoading" style="min-height:60px">
                        <div v-if="comments.length === 0 && !commentLoading" class="comment-empty">
                            暂无评论，来发表第一条吧
                        </div>
                        <div v-else class="comment-list">
                            <div v-for="comment in comments" :key="comment.id" class="comment-item">
                                <div class="comment-body">
                                    <div class="comment-avatar">
                                        <el-avatar :size="32" :src="avatarUrl(comment)">{{ comment.username?.charAt(0) }}</el-avatar>
                                    </div>
                                    <div class="comment-main">
                                        <div class="comment-header">
                                            <span class="comment-author">{{ comment.username }}</span>
                                            <span class="comment-time">{{ formatTime(comment.time) }}</span>
                                        </div>
                                        <div class="comment-text">{{ comment.content }}</div>
                                        <div class="comment-actions">
                                            <span class="comment-action-btn" @click="startReply(comment)">回复</span>
                                            <span v-if="canDelete(comment)" class="comment-action-btn danger" @click="deleteComment(comment.id)">删除</span>
                                        </div>

                                        <div v-if="replyTarget === comment.id" class="reply-input-area">
                                            <el-input
                                                v-model="replyContent"
                                                type="textarea"
                                                :rows="2"
                                                placeholder="回复 {{ comment.username }}..."
                                                maxlength="500"
                                                show-word-limit
                                            />
                                            <div class="reply-input-action">
                                                <el-button size="small" @click="cancelReply" plain>取消</el-button>
                                                <el-button
                                                    type="primary"
                                                    size="small"
                                                    :loading="replySubmitting"
                                                    @click="submitReply"
                                                    :disabled="!replyContent.trim()">
                                                    回复
                                                </el-button>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div v-if="comment.replies && comment.replies.length > 0" class="comment-replies">
                                    <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                                        <div class="comment-avatar">
                                            <el-avatar :size="28" :src="avatarUrl(reply)">{{ reply.username?.charAt(0) }}</el-avatar>
                                        </div>
                                        <div class="comment-main">
                                            <div class="comment-header">
                                                <span class="comment-author">{{ reply.username }}</span>
                                                <span class="comment-time">{{ formatTime(reply.time) }}</span>
                                            </div>
                                            <div class="comment-text">{{ reply.content }}</div>
                                            <div class="comment-actions">
                                                <span v-if="canDelete(reply)" class="comment-action-btn danger" @click="deleteComment(reply.id)">删除</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </light-card>
            </div>
            <div v-else-if="!loading" style="text-align:center;color:grey;padding:40px 0">
                帖子不存在或已被删除
            </div>
        </div>
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

.comment-section-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    font-size: 15px;
}

.comment-input-area {
    margin-bottom: 16px;

    .comment-input-action {
        display: flex;
        justify-content: flex-end;
        margin-top: 8px;
    }
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
