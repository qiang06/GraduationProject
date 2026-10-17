<script setup>
import {Check, Document} from "@element-plus/icons-vue";
import {computed, reactive, ref, onMounted, watch} from "vue";
import {QuillEditor} from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import {ElMessage} from "element-plus";
import axios from "axios";
import {QuillWatch} from "@/utils/quillSetup";
import {accessHeader} from "@/net";
import {apiForumTypes} from "@/net/api";
import {useStore} from "@/store";
import ColorDot from "@/components/ColorDot.vue";

const store = useStore()

const props = defineProps({
    show: Boolean,
    editTopic: Object,
    // 从分类页进入时预设类型，如 lost / activity / confession
    defaultType: String
})

const emit = defineEmits(['close', 'submit', 'update'])

const editor = reactive({
    type: null,
    title: '',
    text: '',
    loading: false,
    types: [],
    // 类型专属字段：失物招领/校园活动会用到
    extra: {}
})

const refEditor = ref()

/**
 * 详情接口返回的 type 是整数 id，而下拉框绑定的是整个类型对象。
 * 不做转换的话，编辑帖子时下拉框会是空的。
 */
function resolveType(value) {
    if (!value) return null
    if (typeof value === 'object') return value
    return editor.types.find(item => item.id === value) || null
}

/** 当前选中的类型对象，供下拉框前缀圆点与下方说明使用 */
const selectedType = computed(() => resolveType(editor.type))
const selectedTypeCode = computed(() => selectedType.value ? selectedType.value.code : '')
const isEditMode = computed(() => !!props.editTopic && !!props.editTopic.id)

// 每种分类需要填写哪些结构化字段；表白墙不需要额外字段，匿名由类型控制
const extraFields = computed(() => {
    switch (selectedTypeCode.value) {
        case 'lost':
            return [
                {key: 'item', label: '物品', placeholder: '如：校园卡 / 黑色雨伞'},
                {key: 'place', label: '地点', placeholder: '如：一食堂二楼'},
                {key: 'happenedAt', label: '时间', placeholder: '选择日期时间', datetime: true},
                {key: 'contact', label: '联系方式', placeholder: '如：QQ / 微信 / 手机号'}
            ]
        case 'activity':
            return [
                {key: 'startAt', label: '开始时间', placeholder: '选择日期时间', datetime: true},
                {key: 'endAt', label: '结束时间', placeholder: '选择日期时间', datetime: true},
                {key: 'place', label: '活动地点', placeholder: '如：图书馆报告厅'},
                {key: 'organizer', label: '主办方', placeholder: '如：学生会 / 计算机协会'}
            ]
        default:
            return []
    }
})

function applyDefaultType() {
    if (props.editTopic || !props.defaultType || !editor.types.length) return
    const target = editor.types.find(item => item.code === props.defaultType)
    if (target) editor.type = target
}

watch(() => props.show, (val) => {
    if (val && props.editTopic) {
        editor.type = resolveType(props.editTopic.type)
        editor.title = props.editTopic.title || ''
        editor.text = props.editTopic.content || ''
        editor.extra = {...(props.editTopic.extra || {})}
    } else if (val && !props.editTopic) {
        editor.type = null
        editor.title = ''
        editor.text = ''
        editor.extra = {}
        applyDefaultType()
    }
})

function loadTypes() {
    // 分类元数据已在 IndexView 拉过一次，这里直接复用，避免每次打开编辑器都请求
    if (store.forum.types.length) {
        editor.types = store.forum.types
        if (props.show && props.editTopic) {
            editor.type = resolveType(props.editTopic.type)
        } else {
            applyDefaultType()
        }
        return
    }
    apiForumTypes((data) => {
        editor.types = data || []
        // store 里的分类由 IndexView 统一维护（含「全部」伪分类），这里只更新本地副本
        // 类型列表是异步加载的：编辑态下若打开抽屉时列表还没到，
        // 这里补一次解析，否则下拉框会一直空白
        if (props.show && props.editTopic) {
            editor.type = resolveType(props.editTopic.type)
        } else {
            applyDefaultType()
        }
    })
}

onMounted(loadTypes)

const CONTENT_LIMIT = 20000
const TITLE_LIMIT = 30
const WARN_THRESHOLD = Math.floor(CONTENT_LIMIT * 0.85)

// 纯文本字数：用户能理解的数字
const plainTextLength = ref(0)
// 提交内容的字符数：后端 @Length 校验的正是它（含 HTML 标签与图片地址）
const contentLength = ref(0)

const nearLimit = computed(() => contentLength.value >= WARN_THRESHOLD)
const overLimit = computed(() => contentLength.value > CONTENT_LIMIT)

/**
 * 同步计数。
 * 不能在 computed 里直接调 quill.getText()：Quill 的文档不是 Vue 响应式的，
 * 那样算一次就被缓存，用户打字时数字永远不动（这就是原来计数不变的原因）。
 * 这里改用 v-model 绑定的 editor.text（HTML 字符串）作为响应式来源。
 */
function syncCounts() {
    const html = editor.text || ''
    contentLength.value = html.length
    if (!refEditor.value) {
        plainTextLength.value = 0
        return
    }
    try {
        plainTextLength.value = refEditor.value.getQuill().getText().trim().length
    } catch (e) {
        plainTextLength.value = 0
    }
}

watch(() => editor.text, syncCounts, {immediate: true})

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
    const html = quill.root.innerHTML

    if (!editor.title || !editor.title.trim()) {
        ElMessage.warning('请填写标题！')
        return
    }
    if (editor.title.length > TITLE_LIMIT) {
        ElMessage.warning(`标题 ${editor.title.length} 字，超出 ${TITLE_LIMIT} 字上限，请删减 ${editor.title.length - TITLE_LIMIT} 字`)
        return
    }
    if (!editor.type) {
        ElMessage.warning('请选择一个合适的帖子类型！')
        return
    }
    if (!quill.getText().trim()) {
        ElMessage.warning('内容不能为空！')
        return
    }
    // 后端校验的是提交内容本身的字符数（含标签与图片地址）。
    // 原来用纯文本长度判断会漏挡：格式多、图片多的帖子会被后端打回，
    // 而用户只看到一句笼统的“请求参数有误”。
    if (html.length > CONTENT_LIMIT) {
        ElMessage.warning(`内容过长：当前 ${html.length} 字符，超出上限 ${CONTENT_LIMIT} 字符，请删减约 ${html.length - CONTENT_LIMIT} 字符（图片与格式同样占用额度）`)
        return
    }

    const data = {
        title: editor.title,
        // 下拉框现在绑定的是整个类型对象，后端要的是分类 id（整数）
        type: editor.type ? editor.type.id : null,
        content: html,
        // 失物招领/校园活动的结构化字段；其他类型后端会忽略
        extra: {...editor.extra}
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
                    <el-select placeholder="选择主题类型..." v-model="editor.type"
                               value-key="id" :disabled="!editor.types.length">
                        <!-- 选中项前面显示对应颜色，收起状态也能看出分类 -->
                        <template #prefix>
                            <color-dot v-if="selectedType" :color="selectedType.color"/>
                        </template>
                        <el-option v-for="item in editor.types.filter(type => type.id > 0)" :key="item.id"
                                   :value="item" :label="item.name">
                            <div class="type-option">
                                <color-dot :color="item.color"/>
                                <span class="type-option-name">{{ item.name }}</span>
                            </div>
                        </el-option>
                    </el-select>
                </div>
                <div style="flex: 1">
                    <el-input v-model="editor.title" placeholder="请输入帖子标题..." :prefix-icon="Document"
                              style="height: 100%" maxlength="30"/>
                </div>
            </div>
            <div class="type-desc">
                <color-dot v-if="selectedType" :color="selectedType.color"/>
                <span>{{ selectedType ? selectedType.desc : '请在上方选择一个帖子类型' }}</span>
            </div>
            <div v-if="extraFields.length" class="extra-fields">
                <div v-for="field in extraFields" :key="field.key" class="extra-field">
                    <span class="extra-label">{{ field.label }}</span>
                    <el-date-picker v-if="field.datetime"
                                    v-model="editor.extra[field.key]"
                                    type="datetime"
                                    value-format="YYYY-MM-DD HH:mm"
                                    :placeholder="field.placeholder"
                                    style="width: 100%"/>
                    <el-input v-else v-model="editor.extra[field.key]"
                              :placeholder="field.placeholder" maxlength="120"/>
                </div>
            </div>
            <div :style="{marginTop: '10px', height: (extraFields.length ? 340 : 440) + 'px', overflow: 'hidden', borderRadius: '5px'}"
                 v-loading="editor.uploading"
                 element-loading-text="正在上传图片，请稍后...">
                <quill-editor v-model:content="editor.text" style="height: calc(100% - 45px)"
                              content-type="html" ref="refEditor"
                              @ready="syncCounts"
                              placeholder="今天想分享点什么呢？" :options="editorOption"/>
            </div>
            <div style="display: flex;justify-content: space-between;margin-top: 5px">
                <div style="font-size: 13px"
                     :style="{color: overLimit ? 'var(--el-color-danger)' : (nearLimit ? 'var(--el-color-warning)' : 'grey')}">
                    <div>当前字数 {{ plainTextLength }}（最大支持 {{ CONTENT_LIMIT }} 字）</div>
                    <div v-if="nearLimit" style="font-size: 12px;margin-top: 2px">
                        含格式与图片共 {{ contentLength }} / {{ CONTENT_LIMIT }} 字符{{ overLimit ? '，已超出上限，无法发布' : '，已接近上限' }}
                    </div>
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
.type-option {
    display: flex;
    align-items: center;
    gap: 10px;
}

.type-option-name {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
}

.type-desc {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 5px;
    min-height: 18px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
}

.extra-fields {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px 12px;
    margin-top: 10px;
    padding: 10px;
    border-radius: 6px;
    background: var(--el-fill-color-lighter);
}

.extra-field {
    display: flex;
    align-items: center;
    gap: 8px;
}

.extra-label {
    flex: none;
    width: 56px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
    text-align: right;
}

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
