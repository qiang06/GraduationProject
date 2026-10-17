import {get, post} from '@/net'

// ==================== 帖子管理 ====================

export const apiAdminTopicList = (page, size, keyword, success, failure) => {
    const query = keyword ? `&keyword=${encodeURIComponent(keyword)}` : ''
    return get(`/api/admin/forum/list?page=${page}&size=${size}${query}`, success, failure)
}

export const apiAdminTopicDelete = (tid, success, failure) =>
    post('/api/admin/forum/delete', {id: tid}, success, failure)

export const apiAdminTopicTop = (tid, status, success, failure) =>
    post('/api/admin/forum/top', {tid, status}, success, failure)

export const apiAdminTopicLocked = (tid, status, success, failure) =>
    post('/api/admin/forum/locked', {tid, status}, success, failure)

export const apiAdminTopicInvisible = (tid, status, success, failure) =>
    post('/api/admin/forum/invisible', {tid, status}, success, failure)

export const apiAdminTopicChangeType = (tid, type, success, failure) =>
    post('/api/admin/forum/change-type', {tid, type}, success, failure)

// ==================== 分类管理 ====================

export const apiAdminTypeCreate = (data, success, failure) =>
    post('/api/admin/forum/type/create', data, success, failure)

export const apiAdminTypeUpdate = (data, success, failure) =>
    post('/api/admin/forum/type/update', data, success, failure)

export const apiAdminTypeDelete = (id, success, failure) =>
    post('/api/admin/forum/type/delete', {id}, success, failure)

// ==================== 用户管理 ====================

export const apiAdminUserList = (page, size, keyword, success, failure) => {
    const query = keyword ? `&keyword=${encodeURIComponent(keyword)}` : ''
    return get(`/api/admin/user/list?page=${page}&size=${size}${query}`, success, failure)
}

export const apiAdminUserSave = (data, success, failure) =>
    post('/api/admin/user/save', data, success, failure)

export const apiAdminUserPassword = (id, newPassword, success, failure) =>
    post('/api/admin/user/change-password', {id, newPassword}, success, failure)
