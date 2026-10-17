import {get, post} from '@/net'

export const apiForumTypes = (success, failure) =>
    get('/api/forum/types', success, failure)

export const apiForumList = (params, success, failure) =>
    get(`/api/forum/list?${params}`, success, failure)

export const apiForumDetail = (id, success, failure) =>
    get(`/api/forum/detail?id=${id}`, success, failure)

export const apiForumCreate = (data, success, failure) =>
    post('/api/forum/create', data, success, failure)

export const apiForumUpdate = (data, success, failure) =>
    post('/api/forum/update', data, success, failure)

export const apiForumDelete = (id, success, failure) =>
    post('/api/forum/delete', {id}, success, failure)

export const apiForumStatus = (id, status, success, failure) =>
    post('/api/forum/status', {id, status}, success, failure)

export const apiForumCommentCreate = (data, success, failure) =>
    post('/api/forum/comment', data, success, failure)

export const apiForumComments = (tid, page, success, failure) =>
    get(`/api/forum/comments?tid=${tid}&page=${page}`, success, failure)

export const apiForumCommentDelete = (id, success, failure) =>
    post('/api/forum/comment-delete', {id}, success, failure)

export const apiForumInteraction = (tid, success, failure) =>
    get(`/api/forum/interaction?tid=${tid}`, success, failure)

export const apiForumLike = (tid, success, failure) =>
    post('/api/forum/interaction/like', {id: tid}, success, failure)

export const apiForumCollect = (tid, success, failure) =>
    post('/api/forum/interaction/collect', {id: tid}, success, failure)

export const apiForumWeather = (longitude, latitude, success, failure, error) =>
    get(`/api/forum/weather?longitude=${longitude}&latitude=${latitude}`, success, failure, error)

export const apiForumWeatherCity = (id, success, failure, error) =>
    get(`/api/forum/weather/city?id=${encodeURIComponent(id)}`, success, failure, error)

export const apiForumCitySearch = (keyword, success, failure, error) =>
    get(`/api/forum/weather/search?keyword=${encodeURIComponent(keyword)}`, success, failure, error)

export const apiForumIp = (success, failure, error) =>
    get('/api/ip', success, failure, error)

export const apiForumCollects = (success, failure) =>
    get('/api/forum/collects', success, failure)

export const apiForumTopTopics = (success, failure) =>
    get('/api/forum/top', success, failure)
