import {get, post} from '@/net'

const query = (params) => new URLSearchParams(params).toString()

// 用户端
export const apiCampusNotices = (params, success, failure) =>
    get(`/api/campus/notices?${query(params)}`, success, failure)

export const apiCampusNoticeDetail = (id, success, failure) =>
    get(`/api/campus/notices/${id}`, success, failure)

export const apiCampusCourses = (success, failure) =>
    get('/api/campus/courses', success, failure)

export const apiCampusCourseSave = (data, success, failure) =>
    post('/api/campus/courses/save', data, success, failure)

export const apiCampusCourseDelete = (id, success, failure) =>
    post('/api/campus/courses/delete', {id}, success, failure)

export const apiCampusLibrary = (params, success, failure) =>
    get(`/api/campus/library?${query(params)}`, success, failure)

export const apiCampusLibraryDetail = (id, success, failure) =>
    get(`/api/campus/library/${id}`, success, failure)

// 管理端
export const apiAdminCampusNotices = (params, success, failure) =>
    get(`/api/admin/campus/notices?${query(params)}`, success, failure)

export const apiAdminCampusNoticeSave = (data, success, failure) =>
    post('/api/admin/campus/notices/save', data, success, failure)

export const apiAdminCampusNoticeDelete = (id, success, failure) =>
    post('/api/admin/campus/notices/delete', {id}, success, failure)

export const apiAdminCampusNoticeTop = (id, top, success, failure) =>
    post('/api/admin/campus/notices/top', {id, top}, success, failure)

export const apiAdminCampusCourses = (params, success, failure) =>
    get(`/api/admin/campus/courses?${query(params)}`, success, failure)

export const apiAdminCampusCourseDelete = (id, success, failure) =>
    post('/api/admin/campus/courses/delete', {id}, success, failure)

export const apiAdminCampusLibrary = (params, success, failure) =>
    get(`/api/admin/campus/library?${query(params)}`, success, failure)

export const apiAdminCampusLibrarySave = (data, success, failure) =>
    post('/api/admin/campus/library/save', data, success, failure)

export const apiAdminCampusLibraryDelete = (id, success, failure) =>
    post('/api/admin/campus/library/delete', {id}, success, failure)
