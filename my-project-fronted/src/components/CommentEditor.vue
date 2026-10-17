<script setup>
import {computed, nextTick, ref, watch} from "vue";
import {QuillEditor} from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import axios from "axios";
import {ElMessage} from "element-plus";
import {accessHeader} from "@/net";
import {apiForumCommentCreate} from "@/net/api";
import {QuillWatch} from "@/utils/quillSetup";

const props = defineProps({
    show: Boolean,
    tid: [String, Number],
    quote: Object
})

const emit = defineEmits(['close', 'submitted'])

const content = ref('')
const htmlContent = ref('')
const refEditor = ref()
const uploading = ref(false)
const submitting = ref(false)
const textLength = ref(0)
const htmlLength = ref(0)

const TEXT_LIMIT = 2000
const HTML_LIMIT = 20000

const hasImage = computed(() => /<img\s/i.test(htmlContent.value))

const quotePreview = computed(() => {
    if (!props.quote) return ''
    const div = document.createElement('div')
    div.innerHTML = props.quote.content || ''
    const text = (div.textContent || '').replace(/\s+/g, ' ').trim()
    return text.length > 30 ? text.slice(0, 30) + '…' : text
})

const title = computed(() => props.quote
    ? `回复 @${props.quote.username || '匿名同学'}`
    : '发表评论')

function syncCounts() {
    const quill = refEditor.value?.getQuill()
    if (!quill) {
        textLength.value = 0
        htmlLength.value = 0
        htmlContent.value = ''
        return
    }
    textLength.value = quill.getText().trim().length
    htmlContent.value = quill.root.innerHTML
    htmlLength.value = htmlContent.value.length
}

function resetEditor() {
    content.value = ''
    nextTick(() => {
        const quill = refEditor.value?.getQuill()
        if (quill) {
            quill.setText('')
            quill.focus()
        }
        syncCounts()
    })
}

watch(() => props.show, (show) => {
    if (show) resetEditor()
})

function submit() {
    const quill = refEditor.value?.getQuill()
    if (!quill) return
    const html = quill.root.innerHTML
    const text = quill.getText().trim()
    if (!text && !/<img\s/i.test(html)) {
        ElMessage.warning('请先写点评论内容')
        return
    }
    if (text.length > TEXT_LIMIT) {
        ElMessage.warning(`评论最多 ${TEXT_LIMIT} 字，当前 ${text.length} 字`)
        return
    }
    if (html.length > HTML_LIMIT) {
        ElMessage.warning('评论内容过长（含格式与图片），请删减后重试')
        return
    }
    if (submitting.value) return
    submitting.value = true
    apiForumCommentCreate({
        tid: Number(props.tid),
        quote: props.quote ? props.quote.id : null,
        content: html
    }, () => {
        submitting.value = false
        ElMessage.success(props.quote ? '回复成功！' : '评论成功！')
        emit('submitted')
    }, (message) => {
        submitting.value = false
        ElMessage.warning(message)
    })
}

const editorOption = {
    modules: {
        toolbar: {
            container: [
                "bold", "italic", "underline", "strike", "clean",
                {color: []}, {'background': []},
                {size: ["small", false, "large", "huge"]},
                {header: [1, 2, 3, 4, 5, 6, false]},
                {list: "ordered"}, {list: "bullet"}, {align: []},
                "blockquote", "code-block", "link", "image",
                {indent: '-1'}, {indent: '+1'}
            ],
            handlers: {
                'image': function () {
                    QuillWatch.emit(this.quill.id)
                }
            }
        },
        imageResize: {
            modules: ['Resize', 'DisplaySize']
        },
        ImageExtend: {
            action: axios.defaults.baseURL + '/api/image/cache',
            name: 'file',
            size: 5,
            loading: true,
            accept: 'image/png, image/jpeg, image/gif, image/webp',
            response: (resp) => {
                const data = typeof resp === 'string' ? JSON.parse(resp) : resp
                if (data && data.data) {
                    return axios.defaults.baseURL + '/images' + data.data
                }
                return null
            },
            methods: 'POST',
            headers: xhr => {
                xhr.setRequestHeader('Authorization', accessHeader().Authorization)
            },
            start: () => uploading.value = true,
            success: () => {
                ElMessage.success('图片上传成功!')
                uploading.value = false
            },
            error: () => {
                ElMessage.warning('图片上传失败，请联系管理员!')
                uploading.value = false
            }
        }
    }
}
</script>

<template>
    <el-drawer class="comment-editor-drawer"
               :model-value="show"
               direction="btt"
               size="420"
               :close-on-click-modal="false"
               @open="resetEditor"
               @close="emit('close')">
        <template #header>
            <div class="comment-editor-header">
                <div class="comment-editor-title">{{ title }}</div>
                <div v-if="quote" class="comment-quote">“{{ quotePreview }}”</div>
            </div>
        </template>
        <div class="comment-editor-box" v-loading="uploading" element-loading-text="正在上传图片...">
            <quill-editor v-model:content="content"
                          content-type="html"
                          :options="editorOption"
                          ref="refEditor"
                          style="height: 230px"
                          @ready="syncCounts"
                          @text-change="syncCounts"
                          placeholder="友善发言，不要骂人；支持加粗、列表、链接和图片"/>
        </div>
        <div class="comment-editor-footer">
            <span :class="{over: textLength > TEXT_LIMIT}">
                {{ textLength }} / {{ TEXT_LIMIT }} 字
                <span v-if="htmlLength > HTML_LIMIT * 0.8" class="html-count">
                    （含格式 {{ htmlLength }} / {{ HTML_LIMIT }}）
                </span>
            </span>
            <el-button type="primary" :loading="submitting" @click="submit">发表</el-button>
        </div>
    </el-drawer>
</template>

<style lang="less" scoped>
.comment-editor-header {
    line-height: 1.5;
}

.comment-editor-title {
    font-weight: 700;
    font-size: 15px;
}

.comment-quote {
    margin-top: 2px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.comment-editor-box {
    height: 280px;
    overflow: hidden;
}

.comment-editor-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 6px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
}

.comment-editor-footer .over {
    color: var(--el-color-danger);
}

.html-count {
    font-size: 12px;
}

/* el-drawer 的内容是 teleport 到 body 的，scoped 的 :deep(.el-drawer) 选不中，
   用传进 $attrs 的 class 走全局选择器。 */
:global(.el-drawer.comment-editor-drawer) {
    width: 820px !important;
    max-width: calc(100vw - 24px);
    height: 420px;
    left: 0;
    right: 0;
    bottom: 24px !important;
    margin: 0 auto;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 16px 48px rgba(0, 0, 0, .24);
}

:global(.el-drawer.comment-editor-drawer .el-drawer__body) {
    padding: 10px 16px 16px;
}

@media (max-width: 860px) {
    :global(.el-drawer.comment-editor-drawer) {
        width: calc(100vw - 24px) !important;
        bottom: 12px !important;
        border-radius: 14px;
    }
}
</style>
