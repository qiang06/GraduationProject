<script setup>
import {computed} from 'vue'
import {useStore} from '@/store'

const props = defineProps({
    type: Number
})

const store = useStore()
const info = computed(() => store.findTypeById(props.type))
</script>

<template>
    <span v-if="info" class="topic-tag"
          :style="{
              color: info.color || '#909399',
              borderColor: (info.color || '#909399') + '66',
              background: (info.color || '#909399') + '1a'
          }">
        {{ info.name }}
    </span>
</template>

<style scoped>
.topic-tag {
    display: inline-block;
    flex: none;
    height: 20px;
    line-height: 18px;
    padding: 0 6px;
    border: 1px solid;
    border-radius: 4px;
    font-size: 12px;
}
</style>
