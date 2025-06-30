package com.myproject.entity.vo.response;

import com.myproject.entity.BaseData;
import lombok.Data;

@Data
public class AuthorizeVO implements BaseData {
    String username;
    String role;
    String token;
    String expire;
}
