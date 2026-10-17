package com.myproject.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 富文本内容白名单过滤。
 *
 * 帖子正文以 HTML 存储，前端直接用 v-html 渲染，因此必须在入库前过滤：
 * 否则任何登录用户只要绕过编辑器直接调接口，就能发一篇带 script 或 img onerror 的帖子，
 * 其他用户一打开就中招（存储型 XSS）。
 *
 * 注意：这是“入库前过滤”，只能挡住新内容，历史脏数据不会被自动修复。
 */
@Component
public class ContentSanitizer {

    private final Safelist safelist;

    public ContentSanitizer() {
        this.safelist = Safelist.none()
                // Quill 编辑器实际会产出的标签
                .addTags("p", "br", "span", "div", "hr",
                        "strong", "b", "em", "i", "u", "s", "strike", "sub", "sup",
                        "blockquote", "pre", "code",
                        "h1", "h2", "h3", "h4", "h5", "h6",
                        "ol", "ul", "li", "a", "img")
                // 对齐、字号、缩进靠 class（ql-align-*、ql-size-*、ql-indent-*）；
                // style 必须放行，否则 Quill 的字体颜色与背景色会被静默丢弃，
                // 危险写法由 stripDangerousStyles 兜底剔除
                .addAttributes(":all", "class", "style")
                .addAttributes("a", "href", "title", "target", "rel")
                .addAttributes("img", "src", "alt", "width", "height")
                .addAttributes("ol", "start")
                .addProtocols("a", "href", "http", "https", "mailto")
                .addProtocols("img", "src", "http", "https");
    }

    /**
     * 过滤正文 HTML。null 与空白原样返回，交给上层校验处理。
     */
    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        Document dirty = Jsoup.parseBodyFragment(html);
        dirty.outputSettings().prettyPrint(false);

        Document clean = new Cleaner(safelist).clean(dirty);
        clean.outputSettings().prettyPrint(false);

        this.stripDangerousStyles(clean);
        // 去掉被白名单剥掉 src/非法协议后剩下的空 img，避免评论里出现破图
        for (Element img : clean.select("img:not([src])")) {
            img.remove();
        }
        return clean.body().html();
    }

    /**
     * jsoup 的白名单不检查 style 内容，这里额外剔除可执行、可外链的写法。
     * 保留 style 是为了不破坏 Quill 的颜色与对齐，但绝不放行 url()、expression()、@import。
     */
    private void stripDangerousStyles(Document document) {
        for (Element element : document.select("[style]")) {
            String style = element.attr("style").toLowerCase(Locale.ROOT);
            boolean dangerous = style.contains("url(")
                    || style.contains("expression")
                    || style.contains("@import")
                    || style.contains("javascript:")
                    || style.contains("behavior:")
                    || style.contains("-moz-binding");
            if (dangerous) {
                element.removeAttr("style");
            }
        }
    }
}
