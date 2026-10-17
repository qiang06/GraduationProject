import {get, post, login, logout} from '@/net'

/** 认证相关接口：登录/登出由 net/index.js 特殊处理 token 存储 */
export const apiAuthLogin = (username, password, remember, success, failure) =>
    login(username, password, remember, success, failure)

export const apiAuthLogout = (success, failure) =>
    logout(success, failure)

export const apiAuthAskCode = (email, type, success, failure, error) =>
    get(`/api/auth/ask-code?email=${encodeURIComponent(email)}&type=${type}`, success, failure, error)

export const apiAuthRegister = (data, success, failure) =>
    post('/api/auth/register', data, success, failure)

export const apiAuthResetConfirm = (data, success, failure) =>
    post('/api/auth/reset-confirm', data, success, failure)

export const apiAuthResetPassword = (data, success, failure) =>
    post('/api/auth/reset-password', data, success, failure)
