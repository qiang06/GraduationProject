<script setup>
import {computed, nextTick, onMounted, onUnmounted, reactive, ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import {ArrowLeft, ArrowRight, DataLine, Lock, Refresh, SwitchButton, User} from '@element-plus/icons-vue'
import LightCard from '@/components/LightCard.vue'
import {
    checkSaved,
    clearSession,
    fetchHealth,
    fetchTerm,
    fetchTerms,
    gradeLogin,
    gradeLogout,
    gradeResume,
    isSessionError,
    readLastStudentId,
    readStudent,
    rememberStudentId,
    takeToken
} from '@/net/grade'

const signedIn = ref(false)
const student = ref(null)
const loggingIn = ref(false)
const loginError = ref('')
const loginErrorCode = ref('')
const passwordRef = ref()
const form = reactive({studentId: '', password: ''})
const resumableId = ref('')
const resuming = ref(false)
const persistEnabled = ref(false)
const savedAvailable = ref(false)
let savedProbeTimer = null

/** 可恢复的目标学号：优先用记住的，否则用当前输入的 */
const resumeTarget = computed(() => resumableId.value || form.studentId.trim())
const canResume = computed(() => persistEnabled.value && savedAvailable.value && !!resumeTarget.value)

const terms = ref([])
const termScroll = ref(null)
const canSlideLeft = ref(false)
const canSlideRight = ref(false)
const currentTerm = ref('')
const activeTerm = ref('')
const loadingTerms = ref(false)
const loadingTerm = ref(false)
const termData = ref(null)
const termError = ref('')

const courses = computed(() => (termData.value && termData.value.courses) || [])
const summary = computed(() => (termData.value && termData.value.summary) || {})

/**
 * 学期区高度固定为三行，超出部分靠左右滑动查看。
 * 必须同步按钮的可用状态，否则用户会以为“滑到头了”。
 */
function updateSlideState() {
    const el = termScroll.value
    if (!el) {
        canSlideLeft.value = false
        canSlideRight.value = false
        return
    }
    // 留 4px 容差，避免小数宽度导致边界抖动
    canSlideLeft.value = el.scrollLeft > 4
    canSlideRight.value = el.scrollLeft + el.clientWidth < el.scrollWidth - 4
}

function slideTerms(direction) {
    const el = termScroll.value
    if (!el) return
    el.scrollBy({
        left: direction * Math.max(el.clientWidth * 0.8, 240),
        behavior: 'smooth'
    })
}

function termLabel(term) {
    const matched = /^(\d{4})-(\d{4})-(\d+)$/.exec(term || '')
    if (!matched) return term
    const names = {'1': '第一学期', '2': '第二学期', '3': '第三学期'}
    const suffix = names[matched[3]] || ('第' + matched[3] + '学期')
    return matched[1] + '-' + matched[2] + ' ' + suffix
}

function scoreValue(row) {
    const raw = row.finalScore
    return raw === null || raw === undefined || raw === '' ? '—' : raw
}

function isFail(row) {
    const numeric = Number(row.finalScore)
    return !Number.isNaN(numeric) && numeric < 60
}

function sortByNumber(field) {
    return (a, b) => {
        const left = Number(a[field])
        const right = Number(b[field])
        const safeLeft = Number.isNaN(left) ? -1 : left
        const safeRight = Number.isNaN(right) ? -1 : right
        return safeLeft - safeRight
    }
}

function readableError(error) {
    if (error.code === 'CAPTCHA_FAILED') {
        return '滑块验证未通过。这是超星的风控环节，识别率并非百分之百，请再点一次登录重试。'
    }
    if (error.code === 'CAPTCHA_UNAVAILABLE') {
        return '服务端缺少 ddddocr 依赖，无法自动识别滑块。请先安装依赖后再试。'
    }
    if (error.code === 'SERVICE_DOWN') {
        return '没有连上成绩服务。请确认本地成绩服务已启动（127.0.0.1:8000）。'
    }
    return error.message || '登录失败，请稍后重试'
}

async function handleLogin() {
    loginError.value = ''
    loginErrorCode.value = ''
    if (!form.studentId.trim()) {
        loginError.value = '请填写学号'
        return
    }
    if (!form.password) {
        loginError.value = '请填写密码'
        passwordRef.value?.focus()
        return
    }
    loggingIn.value = true
    try {
        const data = await gradeLogin(form.studentId.trim(), form.password)
        student.value = data.student
        signedIn.value = true
        rememberStudentId(form.studentId.trim())
        resumableId.value = ''
        form.password = ''
        ElMessage.success('登录成功')
        await loadTerms()
    } catch (error) {
        loginErrorCode.value = error.code || ''
        loginError.value = readableError(error)
    } finally {
        loggingIn.value = false
    }
}

async function handleResume() {
    if (!canResume.value) return
    loginError.value = ''
    loginErrorCode.value = ''
    resuming.value = true
    try {
        const data = await gradeResume(resumeTarget.value)
        student.value = data.student
        signedIn.value = true
        ElMessage.success('已恢复上次登录状态')
        await loadTerms()
    } catch (error) {
        savedAvailable.value = false
        loginError.value = '上次登录的凭据已失效，请输入密码重新登录'
    } finally {
        resuming.value = false
    }
}

async function handleLogout() {
    const lastId = resumableId.value || form.studentId || readLastStudentId()
    await gradeLogout()
    signedIn.value = false
    student.value = null
    terms.value = []
    termData.value = null
    activeTerm.value = ''
    currentTerm.value = ''
    // 后端在退出时已删除落盘凭据，这里必须同步清掉恢复入口，
    // 否则按钮还在、点下去必然失败
    resumableId.value = ''
    form.password = ''
    if (lastId) form.studentId = lastId
}

async function loadTerms() {
    loadingTerms.value = true
    termError.value = ''
    try {
        const data = await fetchTerms()
        terms.value = data.terms || []
        currentTerm.value = data.current || (terms.value[0] || '')
        await nextTick()
        updateSlideState()
        if (data.student) student.value = data.student
        if (currentTerm.value) {
            await loadTerm(currentTerm.value)
        }
    } catch (error) {
        if (isSessionError(error)) {
            fallbackToLogin('超星登录状态已过期，请重新登录')
            return
        }
        termError.value = readableError(error)
    } finally {
        loadingTerms.value = false
    }
}

function formatFetched(seconds) {
    if (!seconds) return ''
    return new Date(seconds * 1000).toLocaleTimeString('zh-CN', {hour: '2-digit', minute: '2-digit'})
}

async function refreshTerm() {
    if (!activeTerm.value) return
    await loadTerm(activeTerm.value, true)
    if (!termError.value) ElMessage.success('已从超星重新获取最新成绩')
}

async function loadTerm(term, force = false) {
    if (!term) return
    activeTerm.value = term
    loadingTerm.value = true
    termError.value = ''
    try {
        termData.value = await fetchTerm(term, force)
    } catch (error) {
        if (isSessionError(error)) {
            fallbackToLogin('超星登录状态已过期，请重新登录')
            return
        }
        termData.value = null
        termError.value = readableError(error)
    } finally {
        loadingTerm.value = false
    }
}

function retryTerm() {
    if (activeTerm.value) loadTerm(activeTerm.value)
}

/** 向后端核实该学号是否真的有可用凭据，不依赖浏览器记忆 */
/**
 * 会话失效时退回登录态。
 * 关键：必须同时重新探测可恢复会话——服务重启后 sessionStorage 里的旧令牌
 * 会先让页面进入已登录分支，失败后才落到这里，如果不再探测就永远看不到“继续上次会话”。
 */
function fallbackToLogin(message) {
    clearSession()
    signedIn.value = false
    loginError.value = message
    loginErrorCode.value = ''
    termData.value = null
    termError.value = ''
    probeResumable()
}

async function probeSaved(studentId) {
    if (!persistEnabled.value || !studentId) {
        savedAvailable.value = false
        return
    }
    try {
        const data = await checkSaved(studentId)
        savedAvailable.value = !!data.available
    } catch (e) {
        savedAvailable.value = false
    }
}

async function probeResumable() {
    try {
        const health = await fetchHealth()
        persistEnabled.value = !!health.persistCredential
    } catch (e) {
        // 服务没起时不必打扰用户，登录表单的错误提示已经覆盖这种情况
        return
    }
    const lastId = readLastStudentId()
    if (lastId) {
        resumableId.value = lastId
        if (!form.studentId) form.studentId = lastId
    }
    await probeSaved(resumeTarget.value)
}

// 用户改学号时（含粘贴）延迟核实，避免每次按键都打请求
watch(() => form.studentId, (value) => {
    clearTimeout(savedProbeTimer)
    savedProbeTimer = setTimeout(() => probeSaved(value.trim()), 500)
})

function handleResize() {
    updateSlideState()
}

onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
})

onMounted(() => {
    window.addEventListener('resize', handleResize)
    if (takeToken()) {
        student.value = readStudent()
        signedIn.value = true
        loadTerms()
        return
    }
    probeResumable()
})
</script>

<template>
    <div class="grade-page">
        <!-- 未登录：超星凭据表单 -->
        <LightCard v-if="!signedIn" class="login-card">
            <div class="login-head">
                <div class="login-badge" aria-hidden="true">
                    <el-icon>
                        <DataLine/>
                    </el-icon>
                </div>
                <h1>成绩查询</h1>
                <p>使用超星（学习通）账号登录，即可按学期查看成绩与绩点</p>
            </div>

            <el-form label-position="top" @submit.prevent="handleLogin">
                <div v-if="canResume" class="resume-block">
                    <el-button :loading="resuming" class="login-submit" size="large" type="primary" plain @click="handleResume">
                        <el-icon>
                            <Refresh/>
                        </el-icon>
                        继续上次会话（{{ resumeTarget }}）
                    </el-button>
                    <p class="resume-note">上次登录凭据仍有效，可直接查询，无需再次验证滑块。</p>
                </div>
                <el-form-item label="学号" required>
                    <el-input
                        v-model="form.studentId"
                        :prefix-icon="User"
                        autocomplete="username"
                        placeholder="请输入学号"
                        size="large"
                        clearable/>
                </el-form-item>
                <el-form-item label="密码" required>
                    <el-input
                        ref="passwordRef"
                        v-model="form.password"
                        :prefix-icon="Lock"
                        autocomplete="current-password"
                        placeholder="请输入密码"
                        size="large"
                        type="password"
                        show-password
                        @keyup.enter="handleLogin"/>
                </el-form-item>

                <el-alert
                    v-if="loginError"
                    :closable="false"
                    class="login-alert"
                    role="alert"
                    show-icon
                    type="error">
                    <template #title>{{ loginError }}</template>
                    <el-button
                        v-if="loginErrorCode === 'CAPTCHA_FAILED'"
                        :loading="loggingIn"
                        class="alert-retry"
                        size="small"
                        type="primary"
                        @click="handleLogin">
                        重新尝试登录
                    </el-button>
                </el-alert>

                <el-button
                    :loading="loggingIn"
                    class="login-submit"
                    size="large"
                    type="primary"
                    @click="handleLogin">
                    {{ loggingIn ? '正在登录（含滑块验证，请稍候）' : '登录并查询成绩' }}
                </el-button>
            </el-form>

            <p class="login-hint">
                学号与密码仅用于本次会话向超星换取登录态，不会保存在本系统。关闭页面后需要重新登录。
            </p>
        </LightCard>

        <template v-else>
            <!-- 已登录：学期与成绩 -->
            <div class="grade-head">
                <div class="grade-identity">
                    <h2>{{ student?.name || '同学' }}</h2>
                    <span>学号 {{ student?.studentId }}</span>
                </div>
                <el-button :icon="SwitchButton" plain @click="handleLogout">退出超星账号</el-button>
            </div>

            <LightCard class="term-bar">
                <div v-loading="loadingTerms" class="term-inner">
                    <span class="term-label">学期</span>
                    <!-- 箭头与列表必须包在同一个 v-if 里，
                         否则后面的 v-else-if 会挂到箭头按钮上，导致空状态误渲染 -->
                    <template v-if="terms.length">
                        <el-button
                            :disabled="!canSlideLeft"
                            :icon="ArrowLeft"
                            aria-label="查看更早的学期"
                            circle
                            class="term-nav"
                            size="small"
                            @click="slideTerms(-1)"/>
                        <div
                            ref="termScroll"
                            class="term-list"
                            role="group"
                            aria-label="选择学期"
                            @scroll.passive="updateSlideState">
                            <el-button
                                v-for="item in terms"
                                :key="item"
                                :aria-pressed="item === activeTerm"
                                :title="item"
                                :type="item === activeTerm ? 'primary' : 'default'"
                                class="term-chip"
                                size="large"
                                @click="loadTerm(item)">
                                {{ termLabel(item) }}
                                <el-tag v-if="item === currentTerm" class="term-tag" size="small" type="success">当前</el-tag>
                            </el-button>
                        </div>
                        <el-button
                            :disabled="!canSlideRight"
                            :icon="ArrowRight"
                            aria-label="查看更晚的学期"
                            circle
                            class="term-nav"
                            size="small"
                            @click="slideTerms(1)"/>
                    </template>
                    <el-empty v-else-if="!loadingTerms" description="没有可查询的学期"/>
                </div>
            </LightCard>

            <el-alert
                v-if="termError"
                :closable="false"
                class="grade-alert"
                role="alert"
                show-icon
                type="error">
                <template #title>{{ termError }}</template>
                <el-button :icon="Refresh" size="small" type="primary" @click="retryTerm">重新加载</el-button>
            </el-alert>

            <el-skeleton v-if="loadingTerm" :rows="6" animated class="grade-skeleton"/>

            <template v-else-if="termData">
                <el-alert
                    v-if="termData.offline"
                    :closable="false"
                    class="grade-alert"
                    show-icon
                    type="warning"
                    title="当前展示的是离线缓存数据。超星服务暂时不可用，数据可能不是最新的。"/>

                <div class="summary-grid">
                    <LightCard class="summary-item">
                        <span class="summary-label">平均绩点</span>
                        <strong class="summary-value">{{ summary.gpa ?? '—' }}</strong>
                        <span class="summary-note">按学分加权</span>
                    </LightCard>
                    <LightCard class="summary-item">
                        <span class="summary-label">学分合计</span>
                        <strong class="summary-value">{{ summary.totalCredit ?? '—' }}</strong>
                        <span class="summary-note">本学期所选课程</span>
                    </LightCard>
                    <LightCard class="summary-item">
                        <span class="summary-label">课程门数</span>
                        <strong class="summary-value">{{ summary.courseCount ?? courses.length }}</strong>
                        <span class="summary-note">{{ termLabel(termData.term) }}</span>
                    </LightCard>
                    <LightCard class="summary-item">
                        <span class="summary-label">通过情况</span>
                        <strong class="summary-value">
                            {{ summary.passCount ?? '—' }} <em>/ {{ summary.noPassCount ?? '—' }}</em>
                        </strong>
                        <span class="summary-note">通过 / 未通过</span>
                    </LightCard>
                </div>

                <LightCard v-if="courses.length" class="course-card">
                    <div class="course-head">
                        <h3>{{ termLabel(termData.term) }} 成绩单</h3>
                        <el-tooltip
                            content="本学期成绩在 10 分钟内已从超星取回，直接复用可避免频繁请求。点上方“刷新成绩”可强制重新获取。"
                            placement="top">
                            <el-tag v-if="termData.cached" size="small" type="info">10 分钟内已获取</el-tag>
                        </el-tooltip>
                        <el-tag v-if="!termData.released" size="small" type="warning">尚未公布</el-tag>
                        <div class="course-head-right">
                            <span v-if="termData.fetchedAt" class="fetched-at">更新于 {{ formatFetched(termData.fetchedAt) }}</span>
                            <el-tooltip content="强制重新向超星获取本学期成绩" placement="top">
                                <el-button
                                    :icon="Refresh"
                                    :loading="loadingTerm"
                                    size="small"
                                    text
                                    @click="refreshTerm">
                                    刷新
                                </el-button>
                            </el-tooltip>
                        </div>
                    </div>
                    <div class="table-scroll">
                        <el-table :data="courses" stripe style="width: 100%">
                            <el-table-column label="课程名称" min-width="220" prop="courseName" show-overflow-tooltip/>
                            <el-table-column :sort-method="sortByNumber('finalScore')" label="分数" prop="finalScore" sortable width="100">
                                <template #default="{row}">
                                    <span :class="{'score-fail': isFail(row)}">{{ scoreValue(row) }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column :sort-method="sortByNumber('finalGradePoint')" label="绩点" prop="finalGradePoint" sortable width="100">
                                <template #default="{row}">{{ row.finalGradePoint ?? '—' }}</template>
                            </el-table-column>
                            <el-table-column :sort-method="sortByNumber('finalCredit')" label="学分" prop="finalCredit" sortable width="90">
                                <template #default="{row}">{{ row.finalCredit ?? '—' }}</template>
                            </el-table-column>
                            <el-table-column label="考核方式" prop="finalSource" width="110">
                                <template #default="{row}">{{ row.finalSource || '—' }}</template>
                            </el-table-column>
                            <el-table-column label="任课教师" prop="teacherName" width="130" show-overflow-tooltip>
                                <template #default="{row}">{{ row.teacherName || '—' }}</template>
                            </el-table-column>
                            <el-table-column label="状态" width="100">
                                <template #default="{row}">
                                    <el-tag :type="isFail(row) ? 'danger' : 'success'" size="small">
                                        {{ isFail(row) ? '未通过' : '通过' }}
                                    </el-tag>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                </LightCard>

                <LightCard v-else class="empty-card">
                    <el-empty :description="termData.released ? '本学期没有成绩记录' : '本学期成绩尚未公布'">
                        <div class="empty-actions">
                            <span class="empty-hint">可以切换到其他学期查看</span>
                            <el-button :icon="Refresh" :loading="loadingTerm" plain size="small" @click="refreshTerm">
                                刷新成绩
                            </el-button>
                        </div>
                    </el-empty>
                </LightCard>
            </template>
        </template>
    </div>
</template>

<style scoped>
.grade-page {
    max-width: 1100px;
    margin: 20px auto;
    padding: 0 16px 40px;
}

/* ---------- 登录态 ---------- */
.login-card {
    max-width: 460px;
    margin: 48px auto;
    padding: 28px 24px;
}

.login-head {
    text-align: center;
    margin-bottom: 20px;
}

.login-badge {
    width: 48px;
    height: 48px;
    margin: 0 auto 12px;
    display: grid;
    place-items: center;
    border-radius: 14px;
    font-size: 24px;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
}

.login-head h1 {
    margin: 0 0 6px;
    font-size: 22px;
    color: var(--el-text-color-primary);
}

.login-head p {
    margin: 0;
    font-size: 13px;
    line-height: 1.6;
    color: var(--el-text-color-secondary);
}

.login-alert {
    margin-bottom: 16px;
}

.resume-block {
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);
}

.resume-note {
    margin: 8px 0 0;
    font-size: 12px;
    line-height: 1.6;
    color: var(--el-text-color-secondary);
}

.alert-retry {
    margin-top: 8px;
}

.login-submit {
    width: 100%;
    min-height: 44px;
}

.login-hint {
    margin: 16px 0 0;
    font-size: 12px;
    line-height: 1.6;
    color: var(--el-text-color-secondary);
}

/* ---------- 成绩态 ---------- */
.grade-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
}

.grade-identity h2 {
    margin: 0 0 4px;
    font-size: 20px;
    color: var(--el-text-color-primary);
}

.grade-identity span {
    font-size: 13px;
    color: var(--el-text-color-secondary);
}

.term-bar {
    margin-bottom: 16px;
    padding: 14px 16px;
}

.term-inner {
    display: flex;
    /* 三行标签块较高，标签与箭头都对整块垂直居中 */
    align-items: center;
    gap: 8px;
    min-height: 44px;
}

.term-label {
    flex: none;
    font-size: 13px;
    color: var(--el-text-color-secondary);
}

.term-list {
    /* 固定最多三行，列优先填充；超出的学期靠左右滑动查看 */
    display: grid;
    grid-auto-flow: column;
    grid-template-rows: repeat(3, auto);
    grid-auto-columns: max-content;
    gap: 8px;
    flex: 1;
    min-width: 0;
    overflow-x: auto;
    overflow-y: hidden;
    /* 彻底隐藏原生滚动条：它就是之前看起来像进度条的东西 */
    scrollbar-width: none;
    -ms-overflow-style: none;
    scroll-behavior: smooth;
    /* 给 hover 时的边框留出空间，避免被裁切 */
    padding: 2px;
}

.term-list::-webkit-scrollbar {
    display: none;
}

.term-nav {
    flex: none;
}

.term-chip {
    flex: none;
    margin: 0;
    border-radius: 8px;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.term-chip:not(.el-button--primary):hover {
    border-color: var(--el-color-primary);
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
}

.term-tag {
    margin-left: 4px;
}

.course-head-right {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-left: auto;
}

.fetched-at {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    font-variant-numeric: tabular-nums;
}

.empty-actions {
    display: flex;
    align-items: center;
    gap: 12px;
}

.grade-alert {
    margin-bottom: 16px;
}

.grade-skeleton {
    margin-top: 8px;
}

.summary-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 12px;
    margin-bottom: 16px;
}

.summary-item {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.summary-label {
    font-size: 13px;
    color: var(--el-text-color-secondary);
}

.summary-value {
    font-size: 26px;
    line-height: 1.2;
    color: var(--el-text-color-primary);
    font-variant-numeric: tabular-nums;
}

.summary-value em {
    font-size: 15px;
    font-style: normal;
    color: var(--el-text-color-secondary);
}

.summary-note {
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.course-card {
    padding: 16px;
}

.course-head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
}

.course-head h3 {
    margin: 0;
    font-size: 16px;
    color: var(--el-text-color-primary);
}

.table-scroll {
    overflow-x: auto;
}

.score-fail {
    color: var(--el-color-danger);
    font-weight: 600;
}

.empty-card {
    padding: 24px;
}

.empty-hint {
    font-size: 13px;
    color: var(--el-text-color-secondary);
}

@media (max-width: 640px) {
    .grade-head {
        align-items: flex-start;
        flex-direction: column;
    }

    .term-inner {
        align-items: stretch;
        flex-direction: column;
    }

    /* 窄屏收成两行，避免学期区占掉半屏 */
    .term-list {
        grid-template-rows: repeat(2, auto);
    }

    .summary-value {
        font-size: 22px;
    }
}

@media (prefers-reduced-motion: reduce) {
    .grade-page * {
        animation-duration: 0.01ms !important;
        transition-duration: 0.01ms !important;
    }

    .term-list {
        scroll-behavior: auto;
    }
}
</style>
