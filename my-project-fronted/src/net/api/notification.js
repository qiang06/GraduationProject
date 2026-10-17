import {get, post} from '@/net'

export const apiNotificationList = (page, size, success, failure) =>
    get(`/api/notification/list?page=${page}&size=${size}`, success, failure)

export const apiNotificationUnread = (success, failure) =>
    get('/api/notification/unread', success, failure)

export const apiNotificationRead = (id, success, failure) =>
    post('/api/notification/read', {id}, success, failure)

export const apiNotificationReadAll = (success, failure) =>
    post('/api/notification/read-all', {}, success, failure)
