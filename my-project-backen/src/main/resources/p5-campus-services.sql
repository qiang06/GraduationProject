-- p5：校园服务（教务通知 / 我的课程表 / 在线图书馆）
-- 只执行一次。三个模块的数据都由管理员在后台维护，不依赖学校接口。

CREATE TABLE IF NOT EXISTS db_notice (
  id int NOT NULL AUTO_INCREMENT,
  title varchar(120) NOT NULL,
  category varchar(20) NOT NULL DEFAULT '教务',
  content text NOT NULL,
  top tinyint(1) NOT NULL DEFAULT 0,
  publisher varchar(50) NOT NULL DEFAULT '教务处',
  publish_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notice_list (top, publish_time),
  KEY idx_notice_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS db_course (
  id int NOT NULL AUTO_INCREMENT,
  uid int NOT NULL,
  name varchar(80) NOT NULL,
  teacher varchar(50) DEFAULT NULL,
  location varchar(80) DEFAULT NULL,
  weekday tinyint NOT NULL,
  start_section tinyint NOT NULL,
  end_section tinyint NOT NULL,
  weeks varchar(40) DEFAULT NULL,
  color varchar(20) DEFAULT '#409EFF',
  note varchar(200) DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_uid (uid),
  KEY idx_course_weekday (weekday)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS db_library_resource (
  id int NOT NULL AUTO_INCREMENT,
  title varchar(120) NOT NULL,
  author varchar(80) DEFAULT NULL,
  category varchar(20) NOT NULL DEFAULT '其他',
  cover varchar(255) DEFAULT NULL,
  description text,
  url varchar(500) DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_library_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 演示通知
INSERT INTO db_notice (title, category, content, top, publisher, publish_time) VALUES
('关于2026-2027学年第一学期选课的通知', '选课', '<p>本学期选课将于 <strong>10月20日 9:00</strong> 开放，请同学们登录教务系统完成选课。</p><p>注意事项：</p><ul><li>先修课程未通过的同学不能选后续课程；</li><li>选课结束后不再受理补选；</li><li>如有问题请联系学院教务办。</li></ul>', 1, '教务处', '2026-10-15 09:00:00'),
('期中考试安排及考场纪律提醒', '考试', '<p>本学期期中考试安排在 <strong>11月2日-11月6日</strong>，具体场次请查看附件或教务系统。</p><p>请携带学生证、身份证提前 15 分钟入场，严禁携带手机等电子设备。</p>', 1, '教务处', '2026-10-12 14:30:00'),
('图书馆开放时间调整通知', '其他', '<p>因设备维护，图书馆三楼自习区将于本周六（10月18日）8:00-12:00 暂停开放，其他区域正常开放。</p>', 0, '图书馆', '2026-10-11 10:00:00'),
('关于开展学籍信息核对的通知', '教务', '<p>请全体在校生登录教务系统核对本人学籍信息，重点核对姓名、学号、专业、班级。</p><p>如有错误，请于10月25日前到学院教务办登记修改。</p>', 0, '教务处', '2026-10-09 08:30:00'),
('创新创业大赛校内选拔报名开始', '其他', '<p>2026年大学生创新创业大赛校内选拔开始报名，截止时间10月30日。</p><p>团队负责人请将项目计划书发送至创新创业学院邮箱。</p>', 0, '创新创业学院', '2026-10-08 16:00:00'),
('国庆假期返校安全提示', '教务', '<p>请同学们合理安排返校时间，注意交通安全；返校后及时向辅导员报备。</p>', 0, '学生处', '2026-10-06 18:00:00');

-- 演示书目 / 资源
INSERT INTO db_library_resource (title, author, category, description, url) VALUES
('Java核心技术 卷I', '凯·S.霍斯特曼', '专业', 'Java 基础语法、集合、并发与常用类库，适合作为 Java 入门到进阶的参考书。', 'https://book.douban.com/'),
('深入理解计算机系统', 'Randal E. Bryant', '专业', '从程序员视角理解计算机系统，涵盖数据表示、汇编、存储器层次与链接。', 'https://book.douban.com/'),
('算法导论', 'Thomas H. Cormen', '专业', '算法设计与分析的经典教材，适合系统学习数据结构与算法。', 'https://book.douban.com/'),
('活着', '余华', '文学', '一个人和他命运之间的友情，讲述普通人在时代中的坚韧。', 'https://book.douban.com/'),
('百年孤独', '加西亚·马尔克斯', '文学', '魔幻现实主义代表作，布恩迪亚家族七代人的传奇故事。', 'https://book.douban.com/'),
('人类简史', '尤瓦尔·赫拉利', '其他', '从认知革命到科学革命，重新理解人类的过去与未来。', 'https://book.douban.com/'),
('大学英语四级真题详解', '星火英语', '考试', '历年四级真题与解析，适合考前集中训练。', 'https://www.icourse163.org/'),
('考研数学基础过关660题', '李永乐', '考试', '考研数学基础阶段习题集，覆盖高数、线代与概率。', 'https://www.icourse163.org/'),
('中国大学MOOC', '高等教育出版社', '其他', '国内高校优质在线课程平台，可按学校或专业检索课程。', 'https://www.icourse163.org/'),
('超星数字图书馆', '超星', '其他', '电子图书与学术资源检索平台，校内访问效果更佳。', 'https://www.chaoxing.com/');

-- 演示课表（uid 1=Admin，uid 2=user；密码/账号不变，仅预置课程）
INSERT INTO db_course (uid, name, teacher, location, weekday, start_section, end_section, weeks, color, note) VALUES
(2, '高等数学A', '张老师', '教1-101', 1, 1, 2, '1-16周', '#409EFF', '带教材和习题册'),
(2, '大学英语II', '李老师', '外语楼203', 2, 3, 4, '1-16周', '#67C23A', ''),
(2, '数据结构', '王老师', '计算机楼305', 3, 5, 6, '1-16周', '#E6A23C', '实验课在周四晚上'),
(2, '体育II', '赵老师', '田径场', 5, 7, 8, '1-16周', '#F56C6C', '穿运动鞋'),
(1, '计算机网络', '刘老师', '计算机楼201', 1, 3, 4, '1-16周', '#909399', ''),
(1, '操作系统', '陈老师', '计算机楼302', 3, 1, 2, '1-16周', '#9B59B6', ''),
(1, '形势与政策', '孙老师', '教2-201', 4, 9, 10, '1-8周', '#1ABC9C', '');
