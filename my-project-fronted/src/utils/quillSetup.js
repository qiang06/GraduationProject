import {Quill} from '@vueup/vue-quill'
import ImageResize from 'quill-image-resize-vue'
import {ImageExtend, QuillWatch} from 'quill-image-super-solution-module'

// 帖子编辑器和评论编辑器共用同一套模块；overwrite=true 避免两个组件都注册时告警
Quill.register('modules/imageResize', ImageResize, true)
Quill.register('modules/ImageExtend', ImageExtend, true)

export {QuillWatch}
