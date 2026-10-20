<script setup>
import {ref, watch} from 'vue'
import {QuillEditor} from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import axios from 'axios'
import {ElMessage} from 'element-plus'
import {accessHeader} from '@/net'
import {QuillWatch} from '@/utils/quillSetup'

const props = defineProps({
    modelValue: {type: String, default: ''},
    placeholder: {type: String, default: '请输入正文...'},
    height: {type: Number, default: 260}
})

const emit = defineEmits(['update:modelValue'])
const inner = ref(props.modelValue || '')
const uploading = ref(false)

watch(() => props.modelValue, value => {
    if ((value || '') !== inner.value) inner.value = value || ''
})

watch(inner, value => {
    emit('update:modelValue', value || '')
})

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
    <div class="rich-editor" v-loading="uploading" element-loading-text="正在上传图片...">
        <quill-editor v-model:content="inner"
                      content-type="html"
                      :options="editorOption"
                      :style="{height: height + 'px'}"
                      :placeholder="placeholder"/>
    </div>
</template>

<style scoped>
.rich-editor {
    border: 1px solid var(--el-border-color);
    border-radius: 5px;
    overflow: hidden;
}
</style>
