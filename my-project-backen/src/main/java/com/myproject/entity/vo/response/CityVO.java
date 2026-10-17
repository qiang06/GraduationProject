package com.myproject.entity.vo.response;

import lombok.Data;

/**
 * 和风天气城市检索结果，供前端「切换城市」使用。
 * id 是字符串而不是 int：和风对境外城市使用 F3E5 这类非数字 ID，转 int 会炸。
 */
@Data
public class CityVO {
    private String id;
    private String name;
    private String adm1;
    private String adm2;
    private String country;
    private String lat;
    private String lon;
}
