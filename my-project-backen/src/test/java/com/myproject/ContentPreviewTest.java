package com.myproject;

import com.myproject.utils.ContentPreview;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 摘要提取的行为约定。用测试锁定 jsoup 的真实行为，避免凭印象写实现。
 */
class ContentPreviewTest {

    private final ContentPreview preview = new ContentPreview();

    @Test
    void stripsAllTags() {
        String result = preview.preview("<p>第一段</p><p>第二段</p>");
        assertFalse(result.contains("<"), "不应残留任何标签: " + result);
        assertTrue(result.contains("第一段") && result.contains("第二段"), result);
        assertTrue(result.contains("第一段 第二段"), "块级标签之间应有空格，实际: " + result);
    }

    @Test
    void removesImagesEntirely() {
        String result = preview.preview("<p>看这张图<img src=\"http://minio/x.png\" alt=\"风景\"></p>");
        assertFalse(result.contains("img"), "不应残留 img 标签: " + result);
        assertFalse(result.contains("http"), "不应带出图片地址: " + result);
        assertTrue(result.contains("看这张图"), result);
    }

    @Test
    void replacesLineBreakWithSpace() {
        String result = preview.preview("第一行<br>第二行");
        assertEquals("第一行 第二行", result);
    }

    @Test
    void normalizesNbspAndWhitespace() {
        String result = preview.preview("<p>甲&nbsp;&nbsp;乙</p>");
        assertEquals("甲 乙", result);
    }

    @Test
    void truncatesWithEllipsis() {
        String longText = "字".repeat(150);
        String result = preview.preview(longText, 100);
        assertEquals(101, result.length(), "100 个字符加一个省略号");
        assertTrue(result.endsWith("…"));
    }

    @Test
    void shortTextIsNotTruncated() {
        assertEquals("短内容", preview.preview("短内容", 100));
        assertFalse(preview.preview("短内容", 100).endsWith("…"));
    }

    @Test
    void handlesNullAndBlank() {
        assertEquals("", preview.preview(null));
        assertEquals("", preview.preview(""));
        assertEquals("", preview.preview("   "));
    }

    @Test
    void imageOnlyPostYieldsEmptyPreview() {
        assertEquals("", preview.preview("<p><img src=\"http://minio/a.png\"></p>"));
    }

    @Test
    void coverImageReturnsFirstImage() {
        String html = "<p>正文</p><img src=\"http://localhost:8080/images/a.png\"><img src=\"http://localhost:8080/images/b.png\">";
        assertEquals("http://localhost:8080/images/a.png", preview.coverImage(html));
    }

    @Test
    void coverImageAcceptsRelativePath() {
        assertEquals("/images/a.png", preview.coverImage("<img src=\"/images/a.png\">"));
    }

    @Test
    void coverImageRejectsPseudoProtocol() {
        assertNull(preview.coverImage("<img src=\"javascript:alert(1)\">"));
        assertNull(preview.coverImage("<img src=\"data:image/png;base64,AAAA\">"));
    }

    @Test
    void coverImageReturnsNullWhenAbsent() {
        assertNull(preview.coverImage(null));
        assertNull(preview.coverImage(""));
        assertNull(preview.coverImage("<p>没有图片的帖子</p>"));
        assertNull(preview.coverImage("<img alt=\"没有src\">"));
    }

    // ---------- Quill Delta JSON（历史遗留格式） ----------

    @Test
    void previewExtractsTextFromDelta() {
        String delta = "{\"ops\":[{\"insert\":\"千万别用了，小心数据库被勒索！\\n\"}]}";
        assertEquals("千万别用了，小心数据库被勒索！", preview.preview(delta));
    }

    @Test
    void previewSkipsEmbeddedImageInDelta() {
        String delta = "{\"ops\":[{\"insert\":\"看图\"},{\"insert\":{\"image\":\"http://localhost:8080/images/a.png\"}},{\"insert\":\"结束\"}]}";
        String result = preview.preview(delta);
        assertTrue(result.contains("看图") && result.contains("结束"), result);
        assertFalse(result.contains("http"), "摘要不应带出图片地址: " + result);
    }

    @Test
    void previewDoesNotLeakDeltaJson() {
        String delta = "{\"ops\":[{\"insert\":\"正文内容\"}]}";
        String result = preview.preview(delta);
        assertFalse(result.contains("ops"), "不应把 Delta JSON 原样展示: " + result);
        assertFalse(result.contains("{"), result);
    }

    @Test
    void coverImageExtractsFromDelta() {
        String delta = "{\"ops\":[{\"insert\":\"文字\"},{\"insert\":{\"image\":\"http://localhost:8080/images/a.png\"}}]}";
        assertEquals("http://localhost:8080/images/a.png", preview.coverImage(delta));
    }

    @Test
    void malformedDeltaDegradesGracefully() {
        assertEquals("", preview.preview("{\"ops\":[}"));
        assertNull(preview.coverImage("{\"ops\":[}"));
    }
}


