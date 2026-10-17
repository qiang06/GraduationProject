import {get, post} from '@/net'

export const apiUserInfo = (success, failure) =>
    get('/api/user/info', success, failure)

export const apiUserDetails = (success, failure) =>
    get('/api/user/details', success, failure)

export const apiUserSaveDetails = (data, success, failure) =>
    post('/api/user/save-details', data, success, failure)

export const apiUserModifyEmail = (data, success, failure) =>
    post('/api/user/modify-email', data, success, failure)

export const apiUserChangePassword = (data, success, failure) =>
    post('/api/user/change-password', data, success, failure)

export const apiUserPrivacy = (success, failure) =>
    get('/api/user/privacy', success, failure)

export const apiUserSavePrivacy = (data, success, failure) =>
    post('/api/user/save-privacy', data, success, failure)
