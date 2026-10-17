package com.myproject.entity.vo.response;

import lombok.Data;

import java.util.Date;

/** 管理端用户列表项；不包含密码 */
@Data
public class AdminUserVO {
    Integer id;
    String username;
    String email;
    String role;
    String avatar;
    Date registerTime;
    boolean mute;
    boolean banned;
}
