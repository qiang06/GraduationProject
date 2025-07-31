<script setup>


import Card from "@/components/Card.vue";
import {Message, Refresh, Select, User} from "@element-plus/icons-vue";
import {useStore} from "@/store/index.js";
import {computed,reactive,ref} from 'vue'
import {post,get} from "@/net/index.js";
import {ElMessage} from "element-plus";



const store = useStore()
const registerTime = computed( ()=>new Date(store.user.registerTime).toLocaleString())

const baseFormRef = ref()
const emailFormRef = ref()
const desc = ref('')

const baseForm = reactive({
  username: '',
  gender: 1,
  phone: '',
  qq: '',
  wx: '',
  desc: ''
})
const emailForm = reactive({
  email: '',
  code: ''
})
const loading = reactive({
  form: true,
  base: false
})

const validateUsername = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入用户名'))
  } else if (!/^[a-zA-Z0-9\u4e00-\u9fa5]+$/.test(value)) {
    callback(new Error('用户名不能包含特殊字符，只能是中文/英文'))
  } else {
    callback()
  }
}
// const rules = {
//   username: [
//     {validator: validateUsername, trigger: ['blur', 'change']},
//     {min: 2, max: 10, message: '用户名的长度必须在2-10个字符之间', trigger: ['blur', 'change']},
//   ], email: [
//     {required: true, message: '请输入邮件地址', trigger: 'blur'},
//     {type: 'email', message: '请输入合法的电子邮件地址', trigger: ['blur', 'change']}
//   ]
// }
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { validator: validateUsername, trigger: ['blur', 'change'] },
    { min: 2, max: 10, message: '用户名的长度必须在2-10个字符之间', trigger: ['blur', 'change'] }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的中国大陆手机号', trigger: 'blur' }
  ],
  qq: [
    { pattern: /^[1-9][0-9]{4,}$/, message: '请输入有效的QQ号', trigger: 'blur' }
  ],
  wx: [
    { min: 5, max: 20, message: '微信号长度5-20位', trigger: 'blur' }
  ],
  desc: [
    { max: 200, message: '个人简介最多200字', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮件地址', trigger: 'blur' },
    { type: 'email', message: '请输入合法的电子邮件地址', trigger: ['blur', 'change'] }
  ]
}

function saveDetails() {
  baseFormRef.value.validate((isValid, invalidFields) => {
    if (isValid) {
      loading.base = true
      post('/api/user/save-details', baseForm, () => {
        ElMessage.success('用户信息保存成功')
        store.user.username = baseForm.username // 修正写法
        desc.value = baseForm.desc
        loading.base = false
      }, (message) => {
        ElMessage.warning(message)
        loading.base = false
      })
    } else {
      // 获取第一个未通过验证的字段并弹窗提示
      const firstField = Object.keys(invalidFields)[0]
      const firstError = invalidFields[firstField][0].message
      ElMessage.error(firstError)
      // 滚动到第一个错误字段（Element Plus 提供的方法）
      baseFormRef.value.scrollToField(firstField)
    }
  })
}

// function saveDetails() {
//   baseFormRef.value.validate(isValid => {
//     if (isValid) {
//       loading.base = true
//       post('/api/user/save-details', baseForm, () => {
//         ElMessage.success('用户信息保存成功')
//         store.user.usernamew = baseForm.username
//         desc.value = baseForm.desc
//         loading.base = false
//       }, (message) => {
//         ElMessage.warning(message)
//         loading.base = false
//       })
//     }
//   })
// }

get('/api/user/details', data => {
  baseForm.username = store.user.username
  baseForm.gender = data.gender
  baseForm.phone = data.phone
  baseForm.wx = data.wx
  baseForm.qq = data.qq
  baseForm.desc = desc.value = data.desc
  emailForm.email = store.user.email
  loading.form = false
})

const coldTime = ref(0)
const isEmailValid = ref(true)
const onValidate = (prop, isValid) => {
  if (prop === 'email')
    isEmailValid.value = isValid
}
function sendEmailCode() {
  emailFormRef.value.validate(isValid => {
    if (isValid) {
      coldTime.value = 60
      get(`/api/auth/ask-code?email=${emailForm.email}&type=modify`, () => {
        ElMessage.success(`验证码已成功发送到邮箱：${emailForm.email}，请注意查收`)
        const handle = setInterval(() => {
          coldTime.value--
          if (coldTime.value === 0) {
            clearInterval(handle)
          }
        }, 1000)
      }, (message) => {
        ElMessage.warning(message)
        coldTime.value = 0
      })
    }
  })
}
function modifyEmail() {
  emailFormRef.value.validate(isValid => {
    if (isValid) {
      post('/api/user/modify-email', emailForm, () => {
        ElMessage.success('邮件修改成功')
        store.user.email = emailForm.email
        emailForm.code = ''
      })
    }
  })
}





</script>

<template>
  <div style="display: flex;max-width: 950px;margin: auto">

    <div class="settings-left">
      <Card :icon="User" title="账号信息设置" desc="在这里编辑您的个人信息，您可以在隐私设置中选择是否展示这些信息"
          v-loading="loading.form"  >
        <el-form :model="baseForm"  ref="baseFormRef" :rules="rules" label-position="top" style="margin: 0 10px 10px 10px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="baseForm.username" maxlength="10"/>
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="baseForm.gender">
              <el-radio :value="`0`">男</el-radio>
              <el-radio :value="`1`">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="baseForm.phone" maxlength="11"/>
          </el-form-item>
          <el-form-item label="QQ号" prop="qq" >
            <el-input v-model="baseForm.qq" maxlength="13"/>
          </el-form-item>
          <el-form-item label="微信号" prop="wx">
            <el-input v-model="baseForm.wx" maxlength="20"/>
          </el-form-item>
          <el-form-item label="个人简介" prop="desc">
            <el-input type="textarea" :rows="6" v-model="baseForm.desc" maxlength="200"/>
          </el-form-item>
          <el-button :icon="Select" @click="saveDetails" :loading="loading.base" type="success">保存用户信息</el-button>
        </el-form>
      </Card>
      <Card style="margin-top: 10px" :icon="Message" title="电子邮件设置"
            desc="您可以在这里修改默认绑定等等电子邮件地址">

        <el-form ref="emailFormRef" :rules="rules" @validate="onValidate" :model="emailForm" label-position="top" style="margin: 0 10px 10px 10px">
          <el-form-item label="电子邮件" prop="email" >
            <el-input v-model="emailForm.email"/>
          </el-form-item>
          <el-form-item prop="code">
            <el-row style="width: 100%" :gutter="10">
              <el-col :span="18">
                <el-input placeholder="请获取验证码" v-model="emailForm.code"/>
              </el-col>
              <el-col :span="6">
                <el-button type="success" style="width: 100%" :disabled="!isEmailValid || coldTime > 0"
                           @click="sendEmailCode"
                           plain>{{ coldTime > 0 ? `请稍后 ${coldTime} 秒` : `获取验证码` }}
                </el-button>
<!--                {{coldTime >0 `请稍后 ${coldTime} 秒:` `获取验证码`}}-->
              </el-col>
            </el-row>
          </el-form-item>
          <el-button :icon="Refresh" type="success" @click="modifyEmail">更新电子邮件</el-button>

        </el-form>
      </Card>

    </div>
    <div class="settings-right">
      <div style="position: sticky;top: 20px">
        <Card>
          <div style="text-align: center;padding: 5px 15px 0 30px">
            <div>
              <el-avatar :size="70" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"/>
              <div style="font-weight: bold">你好，{{store.user.username}}</div>
            </div>
          </div>
          <el-divider style="margin: 10px 0"/>
          <div style="font-size: 14px;color: grey;padding: 10px">
            {{  desc || '这个用户很懒没有填写个人简介~'}}
          </div>
        </Card>
        <card style="margin-top: 10px;font-size: 14px">
          <div>账号注册时间：{{registerTime}}</div>
          <div style="color: grey">欢迎加入我们的学习论坛!</div>
        </card>
      </div>

    </div>


  </div>
</template>

<style scoped>
.settings-left {
  flex: 1;
  margin: 20px;
}

.settings-right {
  width: 300px;
  margin: 20px 30px 20px 0;
}
</style>
