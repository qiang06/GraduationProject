<script setup>
import {computed} from "vue";
import axios from "axios";
import {useStore} from "@/store";

const props = defineProps({
    comment: {type: Object, required: true},
    depth: {type: Number, default: 1},
    topic: Object,
    locked: Boolean
})

const emit = defineEmits(['reply', 'delete'])
const store = useStore()

// 视觉缩进最多 3 层：根 0，第 2 层 16px，第 3 层再 16px，第 4 层起不再增加。
// 不能用 depth*16 直接算，因为节点是嵌套的，margin 会逐层累加。
const indent = computed(() => props.depth > 1 && props.depth <= 3 ? 16 : 0)
const canDelete = computed(() => {
    if (!store.user || !props.comment) return false
    return store.user.id === props.comment.uid
        || (props.topic && store.user.id === props.topic.uid)
})
const showReplyTo = computed(() => !!props.comment.quote && !!props.comment.replyToUsername)

function avatarUrl(user) {
    return user?.avatar ? axios.defaults.baseURL + '/images' + user.avatar : ''
}

function formatTime(time) {
    if (!time) return ''
    const d = new Date(time)
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<template>
    <div class="comment-node" :style="{marginLeft: indent + 'px'}">
        <div class="comment-body">
            <div class="comment-avatar">
                <el-avatar :size="depth > 1 ? 26 : 32" :src="avatarUrl(comment)">
                    {{ comment.username?.charAt(0) }}
                </el-avatar>
            </div>
            <div class="comment-main">
                <div class="comment-header">
                    <span class="comment-author">{{ comment.username }}</span>
                    <span class="comment-time">{{ formatTime(comment.time) }}</span>
                </div>
                <div v-if="showReplyTo" class="reply-to">回复 @{{ comment.replyToUsername }}</div>
                <div class="comment-text rich-text" v-html="comment.content"></div>
                <div class="comment-actions">
                    <span v-if="!locked" class="comment-action-btn" @click="emit('reply', comment)">回复</span>
                    <span v-if="canDelete" class="comment-action-btn danger" @click="emit('delete', comment.id)">删除</span>
                </div>
            </div>
        </div>

        <div v-if="comment.replies && comment.replies.length" class="comment-children">
            <comment-item v-for="reply in comment.replies"
                          :key="reply.id"
                          :comment="reply"
                          :depth="depth + 1"
                          :topic="topic"
                          :locked="locked"
                          @reply="emit('reply', $event)"
                          @delete="emit('delete', $event)"/>
        </div>
    </div>
</template>

<style lang="less" scoped>
.comment-node {
    margin-top: 12px;
}

.comment-body {
    display: flex;
    gap: 10px;
}

.comment-avatar {
    flex: none;
}

.comment-main {
    flex: 1;
    min-width: 0;
}

.comment-header {
    display: flex;
    align-items: center;
    gap: 10px;
}

.comment-author {
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.comment-time {
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.reply-to {
    margin-top: 2px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.comment-text {
    margin-top: 4px;
    font-size: 14px;
    line-height: 1.65;
    color: var(--el-text-color-regular);
    word-break: break-word;
    overflow-wrap: anywhere;
}

.rich-text :deep(p) {
    margin: 4px 0;
}

.rich-text :deep(p:first-child) {
    margin-top: 0;
}

.rich-text :deep(p:last-child) {
    margin-bottom: 0;
}

.rich-text :deep(img) {
    max-width: 100%;
    height: auto;
    border-radius: 4px;
    vertical-align: middle;
}

.rich-text :deep(a) {
    color: var(--el-color-primary);
}

.rich-text :deep(blockquote) {
    margin: 6px 0;
    padding: 4px 10px;
    border-left: 3px solid var(--el-border-color);
    color: var(--el-text-color-secondary);
}

.rich-text :deep(pre) {
    margin: 6px 0;
    padding: 8px 10px;
    overflow-x: auto;
    border-radius: 4px;
    background: var(--el-fill-color-light);
}

.rich-text :deep(code) {
    padding: 1px 4px;
    border-radius: 3px;
    background: var(--el-fill-color-light);
    font-family: monospace;
}

.rich-text :deep(ul),
.rich-text :deep(ol) {
    margin: 4px 0;
    padding-left: 22px;
}

.comment-actions {
    display: flex;
    gap: 14px;
    margin-top: 4px;
    font-size: 12px;
}

.comment-action-btn {
    color: var(--el-text-color-secondary);
    cursor: pointer;
    user-select: none;
}

.comment-action-btn:hover {
    color: var(--el-color-primary);
}

.comment-action-btn.danger:hover {
    color: var(--el-color-danger);
}

.comment-children {
    position: relative;
}
</style>
