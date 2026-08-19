<script setup>
import {Check, Document} from "@element-plus/icons-vue";
import {computed, reactive, ref, onMounted, watch} from "vue";
import {Quill, QuillEditor} from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import {ElMessage} from "element-plus";
import axios from "axios";
import ImageResize from "quill-image-resize-vue";
import { ImageExtend, QuillWatch } from "quill-image-super-solution-module";
import {accessHeader, get} from "@/net/index.js";

const props = defineProps({
    show: Boolean,
    editTopic: Object
})

const emit = defineEmits(['close', 'submit', 'update'])

const editor = reactive({
    type: null,
    title: '',
    text: '',
    loading: false,
    types: []
})

const refEditor = ref()
const isEditMode = computed(() => !!props.editTopic && !!props.editTopic.id)

watch(() => props.show, (val) => {
    if (val && props.editTopic) {
        editor.type = props.editTopic.type
        editor.title = props.editTopic.title || ''
        editor.text = props.editTopic.content || ''
    } else if (val && !props.editTopic) {
        editor.type = null
        editor.title = ''
        editor.text = ''
    }
})

onMounted(() => {
    get('/api/forum/types', (data) => {
        editor.types = data
    })
})

const currentWordCount = computed(() => {
    if (!refEditor.value) return 0
    try {
        const quill = refEditor.value.getQuill()
        return quill.getText().trim().length
    } catch {
        return 0
    }
})

Quill.register('modules/imageResize', ImageResize)
Quill.register('modules/ImageExtend', ImageExtend)

const editorOption = {
    modules: {
        toolbar: {
            container: [
                "bold", "italic", "underline", "strike","clean",
                {color: []}, {'background': []},
                {size: ["small", false, "large", "huge"]},
                { header: [1, 2, 3, 4, 5, 6, false] },
                {list: "ordered"}, {list: "bullet"}, {align: []},
                "blockquote", "code-block", "link", "image",
                { indent: '-1' }, { indent: '+1' }
            ],
            handlers: {
                'image': function () {
                    QuillWatch.emit(this.quill.id)
                }
            }
        },
        imageResize: {
            modules: [ 'Resize', 'DisplaySize' ]
        },
        ImageExtend: {
            action:  axios.defaults.baseURL + '/api/image/cache',
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
                xhr.setRequestHeader('Authorization', accessHeader().Authorization);
            },
            start: () => editor.uploading = true,
            success: () => {
                ElMessage.success('图片上传成功!')
                editor.uploading = false
            },
            error: () => {
                ElMessage.warning('图片上传失败，请联系管理员!')
                editor.uploading = false
            }
        }
    }
}

function submitTopic() {
    if (!refEditor.value) return
    const quill = refEditor.value.getQuill()
    const text = quill.getText()
    if (text.trim().length > 20000) {
        ElMessage.warning('字数超出限制，无法发布主题！')
        return
    }
    if (!editor.title) {
        ElMessage.warning('请填写标题！')
        return
    }
    if (!editor.type) {
        ElMessage.warning('请选择一个合适的帖子类型！')
        return
    }
    const html = quill.root.innerHTML
    const data = {
        title: editor.title,
        type: editor.type,
        content: html
    }
    if (isEditMode.value) {
        data.id = props.editTopic.id
        emit('update', data)
    } else {
        emit('submit', data)
    }
}
</script>

<template>
    <div>
        <el-drawer :model-value="show"
                   direction="btt"
                   :close-on-click-modal="false"
                   size="650"
                   @close="emit('close')">
            <template #header>
                <div>
                    <div style="font-weight: bold">
                        {{ isEditMode ? '编辑帖子' : '发表新的帖子' }}
                    </div>
                    <div style="font-size: 13px">
                        发表内容之前，请遵守相关法律法规，不要出现骂人等不文明行为。
                    </div>
                </div>
            </template>
            <div style="display: flex;gap: 10px">
                <div style="width: 150px">
                    <el-select placeholder="选择主题类型..." v-model="editor.type">
                        <el-option v-for="item in editor.types" :value="item.id" :label="item.name"/>
                    </el-select>
                </div>
                <div style="flex: 1">
                    <el-input v-model="editor.title" placeholder="请输入帖子标题..." :prefix-icon="Document"
                              style="height: 100%" maxlength="30"/>
                </div>
            </div>
            <div style="margin-top: 10px;height: 440px;overflow: hidden;border-radius: 5px"
                 v-loading="editor.uploading"
                 element-loading-text="正在上传图片，请稍后...">
                <quill-editor v-model:content="editor.text" style="height: calc(100% - 45px)"
                              content-type="html" ref="refEditor"
                              placeholder="今天想分享点什么呢？" :options="editorOption"/>
            </div>
            <div style="display: flex;justify-content: space-between;margin-top: 5px">
                <div style="color: grey;font-size: 13px">
                    当前字数 {{ currentWordCount }}（最大支持 20000 字）
                </div>
                <div>
                    <el-button type="success" :icon="Check" @click="submitTopic" plain>
                        {{ isEditMode ? '保存修改' : '立即发表主题' }}
                    </el-button>
                </div>
            </div>
        </el-drawer>
    </div>
</template>

<style scoped>
:deep(.el-drawer) {
    width: 800px;
    margin: auto;
    border-radius: 10px 10px 0 0;
}
:deep(.el-drawer__header) {
    margin: 0;
}
:deep(.ql-toolbar) {
    border-radius: 5px 5px 0 0;
    border-color: var(--el-border-color);
}
:deep(.ql-container) {
    border-radius: 0 0 5px 5px;
    border-color: var(--el-border-color);
}
:deep(.ql-editor.ql-blank::before){
    color: var(--el-text-color-placeholder);
    font-style: normal;
}
:deep(.ql-editor) {
    font-size: 14px;
}
</style>
