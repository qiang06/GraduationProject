package com.myproject.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.entity.dto.Account;
import com.myproject.entity.dto.Comment;
import com.myproject.entity.dto.Topic;
import com.myproject.entity.dto.TopicType;
import com.myproject.entity.vo.request.CreateCommentVO;
import com.myproject.entity.vo.response.CommentPageVO;
import com.myproject.entity.vo.response.CommentVO;
import com.myproject.mapper.AccountMapper;
import com.myproject.mapper.CommentMapper;
import com.myproject.mapper.TopicMapper;
import com.myproject.mapper.TopicTypeMapper;
import com.myproject.service.CommentService;
import com.myproject.service.NotificationService;
import com.myproject.utils.Const;
import com.myproject.utils.ContentPreview;
import com.myproject.utils.ContentSanitizer;
import com.myproject.utils.FlowUtils;
import jakarta.annotation.Resource;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private static final int MAX_TEXT_LENGTH = 2000;
    private static final int MAX_HTML_LENGTH = 20000;
    private static final int MAX_DEPTH = 10;

    @Resource
    AccountMapper accountMapper;

    @Resource
    TopicMapper topicMapper;

    @Resource
    TopicTypeMapper topicTypeMapper;

    @Resource
    FlowUtils flow;

    @Resource
    NotificationService notificationService;

    @Resource
    ContentSanitizer contentSanitizer;

    @Resource
    ContentPreview contentPreview;

    @Override
    @Transactional
    public Map<String, Object> createComment(int uid, CreateCommentVO vo) {
        Topic topic = topicMapper.selectById(vo.getTid());
        if (topic == null) return Map.of("error", "帖子不存在");
        if (topic.getInvisible() != null && topic.getInvisible() == 1 && topic.getUid() != uid) {
            return Map.of("error", "帖子不存在");
        }
        if (topic.getLocked() != null && topic.getLocked() == 1) {
            return Map.of("error", "帖子已被锁定，无法评论");
        }
        Account actor = accountMapper.selectById(uid);
        if (actor != null && actor.isBanned()) {
            return Map.of("error", "您的账号已被封禁，请联系管理员");
        }
        if (actor != null && actor.isMute()) {
            return Map.of("error", "您已被禁言，无法评论");
        }

        String key = Const.FORUM_TOPIC_COMMENT_COUNTER + uid;
        if (!flow.limitOnceCheck(key, 3)) {
            return Map.of("error", "评论过于频繁，请稍后再试");
        }

        // 统一成 HTML：新编辑器提交 HTML，旧客户端/旧数据可能是 Delta JSON 或纯文本
        String html = normalizeContent(vo.getContent());
        String plain = contentPreview.preview(html, Integer.MAX_VALUE);
        boolean hasImage = !Jsoup.parse(html).select("img").isEmpty();
        if (plain.isBlank() && !hasImage) {
            return Map.of("error", "评论内容不能为空");
        }
        if (plain.length() > MAX_TEXT_LENGTH) {
            return Map.of("error", "评论最多 " + MAX_TEXT_LENGTH + " 字，当前 " + plain.length() + " 字");
        }
        if (html.length() > MAX_HTML_LENGTH) {
            return Map.of("error", "评论内容过长（含格式与图片），请删减后重试");
        }

        Comment parent = null;
        Integer rootId = null;
        if (vo.getQuote() != null) {
            parent = this.getById(vo.getQuote());
            if (parent == null) return Map.of("error", "引用的评论不存在");
            if (!Objects.equals(parent.getTid(), vo.getTid())) {
                return Map.of("error", "评论与帖子不匹配");
            }
            int depth = 1;
            Comment cursor = parent;
            while (cursor.getQuote() != null && depth < MAX_DEPTH) {
                depth++;
                cursor = this.getById(cursor.getQuote());
                if (cursor == null) break;
            }
            if (depth >= MAX_DEPTH) {
                return Map.of("error", "回复层级最多 " + MAX_DEPTH + " 层");
            }
            rootId = parent.getRootId() != null ? parent.getRootId() : resolveRootId(parent);
        }

        Comment comment = new Comment(null, uid, vo.getTid(), html, new Date(), vo.getQuote(), rootId);
        if (!this.save(comment)) {
            return Map.of("error", "评论失败，请联系管理员");
        }
        // 顶层评论插入后才知道自己的 id，补一次 root_id
        if (rootId == null) {
            this.update(new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId, comment.getId())
                    .set(Comment::getRootId, comment.getId()));
            comment.setRootId(comment.getId());
        }

        String preview = contentPreview.preview(html, 60);
        if (preview.isBlank()) preview = "[图片]";
        String url = "/index/topic/" + vo.getTid();
        if (parent == null) {
            if (topic.getUid() != null && !topic.getUid().equals(uid)) {
                notificationService.create(topic.getUid(), "帖子收到新评论", preview, "comment", url);
            }
        } else if (parent.getUid() != null && !parent.getUid().equals(uid)) {
            // 多级回复只通知直接父评论作者，避免每层都去打扰楼主
            notificationService.create(parent.getUid(), "评论收到新回复", preview, "reply", url);
        }

        return Map.of("id", comment.getId(), "rootId", comment.getRootId());
    }

    @Override
    public CommentPageVO<CommentVO> listComments(int tid, int page, int size, int requesterUid) {
        // 匿名帖：楼主自己的评论也要对别人匿名，否则一点开评论就暴露了
        Topic topic = topicMapper.selectById(tid);
        if (topic == null) {
            return CommentPageVO.of(List.of(), 0, 0, page, size, 0);
        }
        if (topic.getInvisible() != null && topic.getInvisible() == 1 && topic.getUid() != requesterUid) {
            return CommentPageVO.of(List.of(), 0, 0, page, size, 0);
        }
        final Integer topicUid = topic.getUid();
        final boolean anonymousTopic = this.isAnonymousTopic(topic);

        Page<Comment> result = this.page(new Page<>(page, size), new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTid, tid)
                .isNull(Comment::getQuote)
                .orderByAsc(Comment::getTime));
        long allTotal = this.count(new LambdaQueryWrapper<Comment>().eq(Comment::getTid, tid));

        List<Comment> roots = result.getRecords();
        if (roots.isEmpty()) {
            return CommentPageVO.of(List.of(), result.getTotal(), allTotal,
                    result.getCurrent(), result.getSize(), result.getPages());
        }

        // 顶层评论分页，但一次把本页所有顶层评论的后代取出来，再在内存里建树。
        // 校园论坛单帖评论量有限，这样比子级分页简单，也不会出现“回复翻页”的割裂感。
        List<Integer> rootIds = roots.stream().map(Comment::getId).toList();
        List<Comment> all = this.list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTid, tid)
                .and(w -> w.in(Comment::getRootId, rootIds).or().in(Comment::getId, rootIds))
                .orderByAsc(Comment::getTime));

        Map<Integer, Comment> byId = all.stream()
                .collect(Collectors.toMap(Comment::getId, c -> c, (a, b) -> a));
        Map<Integer, List<Comment>> children = all.stream()
                .filter(c -> c.getQuote() != null)
                .collect(Collectors.groupingBy(Comment::getQuote));

        Set<Integer> uids = all.stream().map(Comment::getUid).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Account> userCache = new HashMap<>();
        if (!uids.isEmpty()) {
            accountMapper.selectBatchIds(uids).forEach(a -> userCache.put(a.getId(), a));
        }

        List<CommentVO> items = roots.stream()
                .map(c -> buildVO(c, byId, children, userCache, anonymousTopic, topicUid, requesterUid))
                .toList();
        return CommentPageVO.of(items, result.getTotal(), allTotal,
                result.getCurrent(), result.getSize(), result.getPages());
    }

    private CommentVO buildVO(Comment c, Map<Integer, Comment> byId, Map<Integer, List<Comment>> children,
                              Map<Integer, Account> userCache, boolean anonymousTopic,
                              Integer topicUid, int requesterUid) {
        CommentVO vo = convert(c, userCache, anonymousTopic, topicUid, requesterUid);
        if (c.getQuote() != null) {
            Comment parent = byId.get(c.getQuote());
            if (parent != null) {
                vo.setReplyToUsername(displayName(parent, userCache, anonymousTopic, topicUid, requesterUid));
            }
        }
        vo.setReplies(children.getOrDefault(c.getId(), List.of()).stream()
                .map(child -> buildVO(child, byId, children, userCache, anonymousTopic, topicUid, requesterUid))
                .collect(Collectors.toList()));
        return vo;
    }

    private CommentVO convert(Comment c, Map<Integer, Account> userCache, boolean anonymousTopic,
                              Integer topicUid, int requesterUid) {
        CommentVO vo = c.asViewObject(CommentVO.class);
        // 历史评论可能是 Delta JSON / 纯文本，统一转成安全 HTML 再下发
        vo.setContent(normalizeContent(c.getContent()));
        vo.setRootId(c.getRootId());

        // 匿名帖楼主发的评论：除楼主自己外，其他人看到「匿名同学」
        if (anonymousTopic && topicUid != null && topicUid.equals(c.getUid()) && requesterUid != c.getUid()) {
            vo.setUid(null);
            vo.setUsername("匿名同学");
            vo.setAvatar(null);
            return vo;
        }
        Account account = userCache.get(c.getUid());
        if (account != null) {
            vo.setUsername(account.getUsername());
            vo.setAvatar(account.getAvatar());
        } else {
            vo.setUsername("未知用户");
        }
        return vo;
    }

    private String displayName(Comment c, Map<Integer, Account> userCache, boolean anonymousTopic,
                               Integer topicUid, int requesterUid) {
        if (anonymousTopic && topicUid != null && topicUid.equals(c.getUid()) && requesterUid != c.getUid()) {
            return "匿名同学";
        }
        Account account = userCache.get(c.getUid());
        return account != null ? account.getUsername() : "未知用户";
    }

    private boolean isAnonymousTopic(Topic topic) {
        if (topic == null) {
            return false;
        }
        TopicType type = topicTypeMapper.selectById(topic.getType());
        return type != null && Boolean.TRUE.equals(type.getAnonymous());
    }

    private Integer resolveRootId(Comment c) {
        Comment cursor = c;
        int guard = 0;
        while (cursor.getQuote() != null && guard++ < MAX_DEPTH + 5) {
            Comment parent = this.getById(cursor.getQuote());
            if (parent == null) break;
            cursor = parent;
        }
        return cursor.getRootId() != null ? cursor.getRootId() : cursor.getId();
    }

    /**
     * 把三种历史格式统一成 HTML：
     * 1. 新编辑器提交的 HTML；
     * 2. 早期 Quill Delta JSON（jwt-1.0 迁移过来的数据）；
     * 3. 纯文本（含换行）。
     */
    private String normalizeContent(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.stripLeading();
        if (trimmed.startsWith("{\"ops\":")) {
            return contentSanitizer.sanitize(deltaToHtml(raw));
        }
        if (trimmed.matches("(?s).*<[a-zA-Z][^>]*>.*")) {
            return contentSanitizer.sanitize(raw);
        }
        String escaped = HtmlUtils.htmlEscape(raw)
                .replace("\r\n", "\n")
                .replace("\n", "<br>");
        return "<p>" + escaped + "</p>";
    }

    /**
     * 旧评论是 Delta JSON。这里只还原文本、粗体/斜体/下划线/删除线/代码/链接/图片，
     * 其他块级格式（列表、标题）按文本处理，保证旧数据能读且不丢内容。
     */
    private String deltaToHtml(String deltaJson) {
        try {
            JSONObject root = JSON.parseObject(deltaJson);
            JSONArray ops = root == null ? null : root.getJSONArray("ops");
            if (ops == null) return "";
            StringBuilder html = new StringBuilder();
            for (int i = 0; i < ops.size(); i++) {
                JSONObject op = ops.getJSONObject(i);
                if (op == null) continue;
                Object insert = op.get("insert");
                if (insert instanceof String text) {
                    String value = HtmlUtils.htmlEscape(text)
                            .replace("\r\n", "\n")
                            .replace("\n", "<br>");
                    JSONObject attrs = op.getJSONObject("attributes");
                    if (attrs != null) {
                        if (Boolean.TRUE.equals(attrs.getBoolean("bold"))) value = "<strong>" + value + "</strong>";
                        if (Boolean.TRUE.equals(attrs.getBoolean("italic"))) value = "<em>" + value + "</em>";
                        if (Boolean.TRUE.equals(attrs.getBoolean("underline"))) value = "<u>" + value + "</u>";
                        if (Boolean.TRUE.equals(attrs.getBoolean("strike"))) value = "<s>" + value + "</s>";
                        if (Boolean.TRUE.equals(attrs.getBoolean("code"))) value = "<code>" + value + "</code>";
                        String link = attrs.getString("link");
                        if (link != null && (link.startsWith("http://") || link.startsWith("https://"))) {
                            value = "<a href=\"" + HtmlUtils.htmlEscape(link) + "\" target=\"_blank\" rel=\"noopener\">"
                                    + value + "</a>";
                        }
                    }
                    html.append(value);
                } else if (insert instanceof JSONObject embed) {
                    String image = embed.getString("image");
                    if (image != null && (image.startsWith("http://") || image.startsWith("https://"))) {
                        html.append("<img src=\"").append(HtmlUtils.htmlEscape(image)).append("\">");
                    }
                }
            }
            return "<p>" + html + "</p>";
        } catch (Exception e) {
            // 脏 Delta 不让整个评论列表报错，降级为空
            return "";
        }
    }

    @Override
    @Transactional
    public String deleteComment(int uid, int id) {
        Comment comment = this.getById(id);
        if (comment == null) return "评论不存在";

        boolean isOwner = Objects.equals(comment.getUid(), uid);
        boolean isTopicAuthor = false;
        if (!isOwner) {
            Topic topic = topicMapper.selectById(comment.getTid());
            isTopicAuthor = topic != null && Objects.equals(topic.getUid(), uid);
        }
        if (!isOwner && !isTopicAuthor) return "无权删除此评论";

        // 删除整棵子树：父评论被删后，子回复不能变成孤儿
        List<Comment> all = this.list(new LambdaQueryWrapper<Comment>().eq(Comment::getTid, comment.getTid()));
        Map<Integer, List<Integer>> children = all.stream()
                .filter(c -> c.getQuote() != null)
                .collect(Collectors.groupingBy(Comment::getQuote,
                        Collectors.mapping(Comment::getId, Collectors.toList())));
        Set<Integer> ids = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(id);
        while (!queue.isEmpty()) {
            int current = queue.poll();
            if (!ids.add(current)) continue;
            queue.addAll(children.getOrDefault(current, List.of()));
        }
        return this.removeByIds(ids) ? null : "删除失败，请联系管理员";
    }
}
