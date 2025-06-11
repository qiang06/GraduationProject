package com.myproject.entity.vo.response;

import lombok.Data;

@Data
public class AuthorizeVO {
    String username;
    String role;
    String token;
    String expire;
}
