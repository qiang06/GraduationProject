package com.myproject.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 帖子正文 → 列表页所需的摘要与封面。
 *
 * <p>正文存在两种历史格式，都要兼容：
 * <ul>
 *   <li><b>HTML</b>：当前 {@code TopicEditor} 提交的格式（{@code quill.root.innerHTML}）</li>
 *   <li><b>Quill Delta JSON</b>：早期版本遗留，形如 {@code {"ops":[{"insert":"..."}]}}。
 *       详情页已在前端用 deltaToHtml 兼容，列表页同样不能把整段 JSON 当正文展示出来。</li>
 * </ul>
 *
 * <p>列表页只展示摘要与一张封面，不整段下发正文：正文是富文本，
 * 一页 10 条帖子若全部渲染会连带加载几十张图片，首屏明显变慢且排版难以对齐。
 */
@Component
public class ContentPreview {

    /** 默认摘要长度，超出截断并追加省略号 */
    public static final int DEFAULT_MAX_LENGTH = 100;

    /** Delta 格式的起始特征，用于区分两种正文格式 */
    private static final String DELTA_PREFIX = "{\"ops\":";

    public String preview(String html) {
        return preview(html, DEFAULT_MAX_LENGTH);
    }

    /**
     * 把正文压成一行纯文本摘要。
     *
     * @param html      帖子正文，允许为 null 或空白
     * @param maxLength 最大字符数，需为正数
     * @return 纯文本摘要；输入为空时返回空串（调用方无需再做 null 判断）
     */
    public String preview(String html, int maxLength) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String text = isDelta(html) ? deltaPlainText(html) : htmlPlainText(html);
        return truncate(text, maxLength);
    }

    /**
     * 取出正文中的第一张图片地址，用作列表页封面缩略图。
     *
     * <p>只取第一张：每张卡片配一张缩略图即可。若把正文里的图片全部渲染到列表，
     * 一页 10 条帖子会触发几十个图片请求。
     *
     * @param html 帖子正文，允许为 null 或空白
     * @return 图片地址；正文无图、或地址协议不安全时返回 null
     */
    public String coverImage(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        if (isDelta(html)) {
            return deltaCoverImage(html);
        }
        Element img = Jsoup.parseBodyFragment(html).selectFirst("img[src]");
        if (img == null) {
            return null;
        }
        String src = img.attr("src").trim();
        return isSafeImageUrl(src) ? src : null;
    }

    // ------------------------------------------------------------------
    // HTML 分支
    // ------------------------------------------------------------------

    private String htmlPlainText(String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        // 摘要不展示图片：去掉元素本身，避免把 alt 文本混进摘要
        doc.select("img").remove();
        // <br> 本身不产生文本，换成空格，避免 "第一行第二行" 粘连
        doc.select("br").after(" ");
        return doc.body().text();
    }

    // ------------------------------------------------------------------
    // Quill Delta JSON 分支
    // ------------------------------------------------------------------

    private boolean isDelta(String content) {
        return content.stripLeading().startsWith(DELTA_PREFIX);
    }

    private JSONArray deltaOps(String deltaJson) {
        try {
            JSONObject root = JSON.parseObject(deltaJson);
            return root == null ? null : root.getJSONArray("ops");
        } catch (Exception e) {
            // 历史脏数据解析失败时降级为空，不让列表页整体报错
            return null;
        }
    }

    private String deltaPlainText(String deltaJson) {
        JSONArray ops = deltaOps(deltaJson);
        if (ops == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < ops.size(); i++) {
            JSONObject op = ops.getJSONObject(i);
            Object insert = op == null ? null : op.get("insert");
            // insert 为字符串才是文本；为对象时是嵌入内容（图片等），摘要里跳过
            if (insert instanceof String value) {
                text.append(value);
            }
        }
        return text.toString();
    }

    private String deltaCoverImage(String deltaJson) {
        JSONArray ops = deltaOps(deltaJson);
        if (ops == null) {
            return null;
        }
        for (int i = 0; i < ops.size(); i++) {
            JSONObject op = ops.getJSONObject(i);
            Object insert = op == null ? null : op.get("insert");
            // Delta 的图片形如 {"insert":{"image":"http://..."}}
            if (insert instanceof JSONObject embed) {
                String url = embed.getString("image");
                if (isSafeImageUrl(url)) {
                    return url.trim();
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 公共处理
    // ------------------------------------------------------------------

    /**
     * 历史数据可能绕过入库过滤，这里再挡一次 javascript:、data: 等伪协议。
     */
    private boolean isSafeImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("/");
    }

    private String truncate(String text, int maxLength) {
        String normalized = normalize(text);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "…";
    }

    /** 富文本常见 &nbsp;（U+00A0）与连续空白，统一压成单个空格 */
    private String normalize(String text) {
        return text.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }
}
