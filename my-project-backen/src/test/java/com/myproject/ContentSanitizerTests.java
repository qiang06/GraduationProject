package com.myproject;

import com.myproject.utils.ContentSanitizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 正文白名单过滤：既要不放过攻击载荷，也不能误伤正常格式 */
class ContentSanitizerTests {

    private final ContentSanitizer sanitizer = new ContentSanitizer();

    @Test
    void removesScriptAndEventHandlers() {
        String attack = "<p>正常文字</p><script>alert(1)</script>"
                + "<img src=\"http://localhost:8080/images/cache/a.png\" onerror=\"alert(2)\">"
                + "<iframe src=\"http://evil.com\"></iframe>"
                + "<svg onload=\"alert(3)\"></svg>";
        String clean = sanitizer.sanitize(attack);
        System.out.println("攻击载荷过滤后 -> " + clean);

        assertFalse(clean.contains("<script"), "script 未被移除");
        assertFalse(clean.contains("onerror"), "onerror 未被移除");
        assertFalse(clean.contains("<iframe"), "iframe 未被移除");
        assertFalse(clean.contains("<svg"), "svg 未被移除");
        assertFalse(clean.contains("alert("), "残留了可执行代码");
        assertTrue(clean.contains("正常文字"), "误删了正常文字");
    }

    @Test
    void stripsJavascriptUrls() {
        String clean = sanitizer.sanitize("<a href=\"javascript:alert(1)\">点我</a>");
        System.out.println("javascript 链接过滤后 -> " + clean);
        assertFalse(clean.contains("javascript:"), "javascript: 协议未被移除");
        assertTrue(clean.contains("点我"), "误删了链接文字");
    }

    @Test
    void stripsDangerousInlineStylesButKeepsColors() {
        String dirty = "<span style=\"background:url(javascript:alert(1))\">x</span>"
                + "<span style=\"color: rgb(230, 0, 0);\">红字</span>";
        String clean = sanitizer.sanitize(dirty);
        System.out.println("样式过滤后 -> " + clean);

        assertFalse(clean.contains("url("), "危险 url() 未被移除");
        assertTrue(clean.contains("红字"), "误删了正文");
        assertTrue(clean.contains("color"), "误删了正常的颜色样式（Quill 会用到）");
    }

    @Test
    void keepsRealQuillOutputIntact() {
        // 取自本项目编辑器真实会产出的结构，含绝对图片地址
        String quill = "<p class=\"ql-align-center\"><strong>标题</strong>"
                + "<span style=\"color: rgb(230, 0, 0);\">红色</span></p>"
                + "<p><img src=\"http://localhost:8080/images/cache/20260918/abc.png\" "
                + "style=\"max-width:100%\"></p>"
                + "<ol><li>第一项</li></ol>";
        String clean = sanitizer.sanitize(quill);
        System.out.println("正常内容过滤后 -> " + clean);

        assertTrue(clean.contains("http://localhost:8080/images/cache/20260918/abc.png"),
                "把正常图片地址删掉了，这会导致所有帖子图片失效");
        assertTrue(clean.contains("ql-align-center"), "丢失了对齐 class");
        assertTrue(clean.contains("<strong>"), "丢失了加粗");
        assertTrue(clean.contains("<ol>"), "丢失了有序列表");
        assertTrue(clean.contains("color"), "丢失了颜色");
    }

    @Test
    void blocksDataUriImages() {
        String clean = sanitizer.sanitize(
                "<img src=\"data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=\">");
        System.out.println("data: 图片过滤后 -> [" + clean + "]");
        assertFalse(clean.contains("data:"), "data: 协议应被拒绝（可承载 script）");
    }

    @Test
    void blankInputPassesThrough() {
        assertNull(sanitizer.sanitize(null));
        assertEquals("   ", sanitizer.sanitize("   "));
    }
}
