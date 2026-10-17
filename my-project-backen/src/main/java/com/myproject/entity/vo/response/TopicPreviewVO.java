package com.myproject.entity.vo.response;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.Date;

/**
 * 帖子广场列表项。
 *
 * <p>这里刻意不包含正文（content）本身。列表页只需要一段纯文本摘要：
 * 正文是富文本 HTML，若整段下发给前端渲染，一页 10 条帖子会连带加载几十张图片，
 * 首屏变慢且排版难以对齐。摘要由服务端提取，保证天然不含标签与图片。
 */
@Data
public class TopicPreviewVO {
    Integer id;
    String title;
    /** 纯文本摘要，由 ContentPreview 从正文提取，不含任何 HTML 标签与图片 */
    String preview;
    Integer uid;
    String username;
    /** 作者头像地址；作者已注销时为 null */
    String avatar;
    /** 正文第一张图片，用作列表封面缩略图；正文无图时为 null */
    String cover;
    Integer type;
    String typeName;
    String typeCode;
    /** 类型专属字段；无则为 null */
    JSONObject extra;
    Date time;
    Integer top;
    Integer locked;
    Integer invisible;
    Integer likeCount;
    Integer collectCount;
}
