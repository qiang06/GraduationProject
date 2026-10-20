import { createRouter, createWebHistory } from 'vue-router'
import { isRoleAdmin, unauthorized } from "@/net";

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'welcome',
            component: () => import('@/views/WelcomeView.vue'),
            children: [
                {
                    path: '',
                    name: 'welcome-login',
                    component: () => import('@/views/welcome/LoginPage.vue'),
                    meta: {title: '登录'}
                },
                {
                    path: 'register',
                    name: 'welcome-register',
                    component: () => import('@/views/welcome/RegisterPage.vue'),
                    meta: {title: '注册'}
                },
                {
                    path: 'forget',
                    name: 'welcome-forget',
                    component: () => import('@/views/welcome/ForgetPage.vue'),
                    meta: {title: '找回密码'}
                }
            ]
        },
        {
            path: '/index',
            name: 'index',
            component: () => import('@/views/IndexView.vue'),
            children: [
                {
                    path: '',
                    name: 'topic-list',
                    component: () => import('@/views/forum/TopicList.vue'),
                    meta: {title: '帖子广场'}
                },
                {
                    path: 'topic/:id',
                    name: 'topic-detail',
                    component: () => import('@/views/forum/TopicDetail.vue'),
                    meta: {title: '帖子详情'}
                },
                {
                    path: 'lost',
                    name: 'topic-lost',
                    component: () => import('@/views/forum/TopicList.vue'),
                    meta: {typeCode: 'lost', title: '失物招领', hint: '丢了或捡到东西都可以发，找到后由作者标记「已找回」'}
                },
                {
                    path: 'activity',
                    name: 'topic-activity',
                    component: () => import('@/views/forum/TopicList.vue'),
                    meta: {typeCode: 'activity', title: '校园活动', hint: '社团、讲座、比赛等校园活动信息'}
                },
                {
                    path: 'confession',
                    name: 'topic-confession',
                    component: () => import('@/views/forum/TopicList.vue'),
                    meta: {typeCode: 'confession', title: '表白墙', hint: '匿名表达心意，其他人看不到你的用户名'}
                },
                {
                    path: 'grade',
                    name: 'grade',
                    component: () => import('@/views/grade/GradeView.vue'),
                    meta: {title: '成绩查询'}
                },
                {
                    path: 'notices',
                    name: 'campus-notices',
                    component: () => import('@/views/campus/NoticeList.vue'),
                    meta: {title: '教务通知'}
                },
                {
                    path: 'notices/:id',
                    name: 'campus-notice-detail',
                    component: () => import('@/views/campus/NoticeDetail.vue'),
                    meta: {title: '通知详情'}
                },
                {
                    path: 'schedule',
                    name: 'campus-schedule',
                    component: () => import('@/views/campus/ScheduleView.vue'),
                    meta: {title: '我的课程表'}
                },
                {
                    path: 'library',
                    name: 'campus-library',
                    component: () => import('@/views/campus/LibraryList.vue'),
                    meta: {title: '在线图书馆'}
                },
                {
                    path: 'library/:id',
                    name: 'campus-library-detail',
                    component: () => import('@/views/campus/LibraryDetail.vue'),
                    meta: {title: '资源详情'}
                },
                {
                    path: 'collects',
                    name: 'collects',
                    component: () => import('@/views/forum/CollectList.vue'),
                    meta: {title: '我的收藏'}
                },
                {
                    path: 'notifications',
                    name: 'notifications',
                    component: () => import('@/views/NotificationView.vue'),
                    meta: {title: '消息列表'}
                },
                {
                    path: 'user-setting',
                    name: 'user-setting',
                    component: () => import('@/views/settings/UserSetting.vue'),
                    meta: {title: '个人信息'}
                },
                {
                    path: 'privacy-setting',
                    name: 'privacy-setting',
                    component: () => import('@/views/settings/PrivacySetting.vue'),
                    meta: {title: '账号安全'}
                }
            ]
        },
        {
            path: '/admin',
            name: 'admin',
            component: () => import('@/views/AdminView.vue'),
            meta: {title: '管理后台'},
            children: [
                {
                    path: '',
                    redirect: '/admin/forum'
                },
                {
                    path: 'forum',
                    name: 'admin-forum',
                    component: () => import('@/views/admin/AdminForum.vue'),
                    meta: {title: '帖子管理'}
                },
                {
                    path: 'user',
                    name: 'admin-user',
                    component: () => import('@/views/admin/AdminUser.vue'),
                    meta: {title: '用户管理'}
                },
                {
                    path: 'campus',
                    name: 'admin-campus',
                    component: () => import('@/views/admin/AdminCampus.vue'),
                    meta: {title: '校园服务管理'}
                }
            ]
        }
    ]
})

router.beforeEach((to, from, next) => {
    const isUnauthorized = unauthorized()
    if (to.name?.startsWith('welcome') && !isUnauthorized) {
        next('/index')
    } else if (to.path.startsWith('/admin') && !isRoleAdmin()) {
        next('/index')
    } else if (to.fullPath.startsWith('/index') && isUnauthorized) {
        next('/')
    } else {
        next()
    }
})

router.afterEach((to) => {
    const title = to.meta && to.meta.title
    document.title = title ? `${title} · 校园论坛` : '校园论坛'
})

export default router
