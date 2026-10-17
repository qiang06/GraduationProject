<script setup>
import {ref} from 'vue'
import {apiForumTypes} from '@/net/api'
import ForumTopicAdmin from '@/views/admin/ForumTopicAdmin.vue'
import ForumTopicTypeAdmin from '@/views/admin/ForumTopicTypeAdmin.vue'

const types = ref([])

function loadTypes() {
    apiForumTypes(data => types.value = data || [])
}

loadTypes()
</script>

<template>
    <div class="admin-forum">
        <forum-topic-admin :types="types"/>
        <forum-topic-type-admin :types="types" @update="loadTypes"/>
    </div>
</template>

<style scoped>
.admin-forum {
    display: flex;
    flex-direction: column;
    gap: 28px;
}
</style>
