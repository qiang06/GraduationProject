import { defineStore } from "pinia";
import axios from "axios";

export const useStore = defineStore('general', {
    state: () => {
        return {
            user: {
                id: 0,
                username: '',
                email: '',
                role: '',
                avatar: '',
                registerTime: null
            },
            forum: {
                // 分类元数据在应用内只拉一次，TopicTag / 编辑器共用
                types: []
            }
        }
    },
    getters: {
        avatarUrl() {
            if (this.user.avatar)
                return `${axios.defaults.baseURL}/images${this.user.avatar}`
            else
                return 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
        }
    },
    actions: {
        findTypeById(id) {
            return this.forum.types.find(type => type.id === id) || null
        },
        avatarUserUrl(avatar) {
            if (avatar)
                return `${axios.defaults.baseURL}/images${avatar}`
            else
                return 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
        }
    }
})
