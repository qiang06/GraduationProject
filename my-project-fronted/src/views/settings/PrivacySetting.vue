<script setup>

import Card from "@/components/Card.vue";
import {Setting, Switch, Lock} from "@element-plus/icons-vue";
import {reactive, ref} from "vue";
import {get, post} from "@/net";
import {ElMessage} from "element-plus";

const form = reactive({
    password: '',
    new_password: '',
    new_password_repeat: ''
})
const validatePassword = (rule, value, callback) => {
    if (value === '') {
        callback(new Error('请再次输入密码'))
    } else if (value !== form.new_password) {
        callback(new Error("两次输入的密码不一致"))
    } else {
        callback()
    }
}
const rules = {
    password: [
        {required: true, message: '请输入原来的密码', trigger: 'blur'}
    ],
    new_password: [
        {required: true, message: '请输入新的密码', trigger: 'blur'},
        {min: 6, max: 16, message: '密码的长度必须在6-16个字符之间', trigger: ['blur']}
    ],
    new_password_repeat: [
        {required: true, message: '请再次输入新的密码', trigger: 'blur'},
        {validator: validatePassword, trigger: ['blur', 'change']},
    ]
}
const formRef = ref()
const valid = ref(false)
const onValidate = (prop, isValid) => valid.value = isValid

function resetPassword() {
    formRef.value.validate(valid => {
        if (valid) {
            post('/api/user/change-password', form, () => {
                ElMessage.success('修改密码成功！')
                formRef.value.resetFields();
            })
        }
    })
}

const saving = ref(true)
const privacy = reactive({
    phone: false,
    wx: false,
    qq: false,
    email: false,
    gender: false
})
get('/api/user/privacy', data => {
    privacy.phone = data.phone
    privacy.email = data.email
    privacy.wx = data.wx
    privacy.qq = data.qq
    privacy.gender = data.gender
    saving.value = false
})

function savePrivacy(type, status) {
    saving.value = true
    post('/api/user/save-privacy', {
        type: type,
        status: status
    }, () => {
        ElMessage.success('隐私设置修改成功！')
        saving.value = false
    })
}
</script>

<template>
    <div style="margin: auto;max-width: 600px">
        <div style="margin-top: 20px">
            <card v-loading="saving" :icon="Setting"
                  desc="在这里设置哪些内容可以被其他人看到，请各位小伙伴注重自己的隐私"
                  title="隐私设置">
                <div class="checkbox-list">
                    <el-checkbox v-model="privacy.phone"
                                 @change="savePrivacy('phone', privacy.phone)">公开展示我的手机号
                    </el-checkbox>
                    <el-checkbox v-model="privacy.email"
                                 @change="savePrivacy('email', privacy.email)">公开展示我的电子邮件地址
                    </el-checkbox>
                    <el-checkbox v-model="privacy.wx"
                                 @change="savePrivacy('wx', privacy.wx)">公开展示我的微信号
                    </el-checkbox>
                    <el-checkbox v-model="privacy.qq"
                                 @change="savePrivacy('qq', privacy.qq)">公开展示我的QQ号
                    </el-checkbox>
                    <el-checkbox v-model="privacy.gender"
                                 @change="savePrivacy('gender', privacy.gender)">公开展示我的性别
                    </el-checkbox>
                </div>
            </card>
            <card :icon="Setting" desc="修改密码请在这里进行操作，请务必牢记您的密码"
                  style="margin: 20px 0" title="修改密码">
                <el-form ref="formRef" :model="form" :rules="rules" label-width="100" style="margin: 20px"
                         @validate="onValidate">
                    <el-form-item label="当前密码" prop="password">
                        <el-input v-model="form.password" :prefix-icon="Lock" maxlength="16"
                                  placeholder="当前密码" type="password"/>
                    </el-form-item>
                    <el-form-item label="新密码" prop="new_password">
                        <el-input v-model="form.new_password" :prefix-icon="Lock" maxlength="16"
                                  placeholder="新密码" type="password"/>
                    </el-form-item>
                    <el-form-item label="重复新密码" prop="new_password_repeat">
                        <el-input v-model="form.new_password_repeat" :prefix-icon="Lock" maxlength="16"
                                  placeholder="重新输入新密码" type="password"/>
                    </el-form-item>
                    <div style="text-align: center">
                        <el-button :icon="Switch" type="success" @click="resetPassword">立即重置密码</el-button>
                    </div>
                </el-form>
            </card>
        </div>
    </div>
</template>

<style scoped>
.checkbox-list {
    margin: 10px 0 0 10px;
    display: flex;
    flex-direction: column;
}
</style>
