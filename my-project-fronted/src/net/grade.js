import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 成绩查询服务的专用网络层。
 *
 * 刻意与 @/net 分开：@/net 绑定论坛 JWT，且收到 401 会自动登出论坛。
 * 超星会话过期属于另一个系统的凭据问题，不应该把用户踢出论坛。
 */
const SESSION_KEY = 'gradeSession'

const client = axios.create({
    baseURL: '/grade/api',
    // 登录要过滑块并可换图重试，耗时可达数十秒，超时给足
    timeout: 120000
})

function takeToken() {
    const raw = sessionStorage.getItem(SESSION_KEY)
    if (!raw) return null
    try {
        const saved = JSON.parse(raw)
        if (!saved.token || (saved.expireAt && Date.now() > saved.expireAt)) {
            sessionStorage.removeItem(SESSION_KEY)
            return null
        }
        return saved.token
    } catch (e) {
        sessionStorage.removeItem(SESSION_KEY)
        return null
    }
}

function storeSession(token, student, expiresIn) {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify({
        token: token,
        student: student,
        expireAt: expiresIn ? Date.now() + expiresIn * 1000 : null
    }))
}

function readStudent() {
    const raw = sessionStorage.getItem(SESSION_KEY)
    if (!raw) return null
    try {
        return JSON.parse(raw).student || null
    } catch (e) {
        return null
    }
}

function clearSession() {
    sessionStorage.removeItem(SESSION_KEY)
}

function authHeader() {
    const token = takeToken()
    return token ? { 'X-Grade-Session': token } : {}
}

/**
 * 统一把服务端错误转成带 code 的 Error，便于页面区分滑块失败等情况。
 */
function normalize(error) {
    if (error.response) {
        const data = error.response.data || {}
        const err = new Error(data.message || '成绩服务返回了异常响应')
        err.code = data.code || 'HTTP_' + error.response.status
        err.status = error.response.status
        return err
    }
    if (error.code === 'ECONNABORTED') {
        const err = new Error('请求超时，超星服务响应过慢，请稍后重试')
        err.code = 'TIMEOUT'
        return err
    }
    const err = new Error('无法连接成绩服务，请确认本地服务已启动（127.0.0.1:8000）')
    err.code = 'SERVICE_DOWN'
    return err
}

async function request(config) {
    try {
        const { data } = await client.request({ ...config, headers: { ...authHeader(), ...(config.headers || {}) } })
        return data
    } catch (error) {
        throw normalize(error)
    }
}

export async function gradeLogin(studentId, password) {
    try {
        const { data } = await client.post('/login', { studentId, password })
        storeSession(data.token, data.student, data.expiresIn)
        return data
    } catch (error) {
        throw normalize(error)
    }
}

/**
 * 仅用已保存的凭据恢复会话，不会触发密码登录与滑块。
 */
export async function gradeResume(studentId) {
    try {
        const { data } = await client.post('/resume', { studentId })
        storeSession(data.token, data.student, data.expiresIn)
        return data
    } catch (error) {
        throw normalize(error)
    }
}

/** 查询服务端是否已保存该学号的凭据（只返回布尔值） */
export function checkSaved(studentId) {
    return request({ url: '/saved', method: 'get', params: { studentId } })
}

export async function gradeLogout() {
    try {
        await client.post('/logout', {}, { headers: authHeader() })
    } catch (e) {
        // 退出失败也要清掉本地会话，避免卡在半个登录态
    }
    clearSession()
}

export function fetchTerms() {
    return request({ url: '/terms', method: 'get' })
}

export function fetchTerm(term, refresh = false) {
    return request({
        url: '/term',
        method: 'get',
        params: refresh ? { term, refresh: true } : { term }
    })
}

export function fetchHealth() {
    return request({ url: '/health', method: 'get' })
}

/** 会话失效（401）时页面需要退回登录态 */
export function isSessionError(error) {
    return error && (error.code === 'SESSION_EXPIRED' || error.code === 'SESSION_REQUIRED')
}

const LAST_ID_KEY = 'gradeLastStudentId'

/** 只记学号，绝不记密码 */
export function readLastStudentId() {
    try {
        return localStorage.getItem(LAST_ID_KEY) || ''
    } catch (e) {
        return ''
    }
}

export function rememberStudentId(studentId) {
    try {
        localStorage.setItem(LAST_ID_KEY, studentId)
    } catch (e) {
        // 隐私模式下 localStorage 可能不可用，忽略即可
    }
}

export function forgetStudentId() {
    try {
        localStorage.removeItem(LAST_ID_KEY)
    } catch (e) {
    }
}

export { takeToken, readStudent, clearSession }
