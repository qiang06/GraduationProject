package com.myproject.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.entity.dto.Account;
import com.myproject.entity.dto.AccountDetails;
import com.myproject.entity.dto.AccountPrivacy;
import com.myproject.entity.dto.Topic;
import com.myproject.entity.dto.TopicInteractCollect;
import com.myproject.entity.dto.TopicInteractLike;
import com.myproject.entity.dto.TopicType;
import com.myproject.entity.vo.request.CreateTopicVO;
import com.myproject.entity.vo.request.TopicTypeCreateVO;
import com.myproject.entity.vo.request.TopicTypeUpdateVO;
import com.myproject.entity.vo.request.UpdateTopicVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.entity.vo.response.TopicAuthorVO;
import com.myproject.entity.vo.response.TopicDetailVO;
import com.myproject.entity.vo.response.TopicPreviewVO;
import com.myproject.entity.vo.response.TopicTopVO;
import com.myproject.mapper.AccountMapper;
import com.myproject.mapper.CommentMapper;
import com.myproject.mapper.TopicInteractCollectMapper;
import com.myproject.mapper.TopicInteractLikeMapper;
import com.myproject.mapper.TopicMapper;
import com.myproject.mapper.TopicTypeMapper;
import com.myproject.service.AccountDetailsService;
import com.myproject.service.AccountPrivacyService;
import com.myproject.service.TopicPostService;
import com.myproject.utils.Const;
import com.myproject.utils.ContentPreview;
import com.myproject.utils.ContentSanitizer;
import com.myproject.utils.FlowUtils;
import com.myproject.utils.RedisCacheUtils;
import jakarta.annotation.Resource;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TopicPostServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicPostService {

    /**
     * 帖子列表缓存时长（分钟）。
     * 取值偏短是因为列表项里含点赞/收藏数，这两个数变化频繁，
     * 缓存太久会出现「刚点赞但列表数字没变」的困惑。
     */
    private static final long PREVIEW_CACHE_MINUTES = 2;

    /**
     * 各分类允许的额外字段白名单。
     * 表白墙不需要结构化字段，匿名由 db_topic_type.anonymous 控制。
     */
    private static final Map<String, Set<String>> EXTRA_KEYS = Map.of(
            "lost", Set.of("item", "place", "happenedAt", "contact", "status"),
            "activity", Set.of("startAt", "endAt", "place", "organizer")
    );

    @Resource
    AccountMapper accountMapper;

    @Resource
    AccountDetailsService accountDetailsService;

    @Resource
    AccountPrivacyService accountPrivacyService;

    @Resource
    TopicTypeMapper topicTypeMapper;

    @Resource
    CommentMapper commentMapper;

    @Resource
    ContentSanitizer contentSanitizer;

    @Resource
    ContentPreview contentPreview;

    @Resource
    RedisCacheUtils cacheUtils;

    @Resource
    TopicInteractLikeMapper likeMapper;
    @Resource
    TopicInteractCollectMapper collectMapper;

    @Resource
    FlowUtils flow;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public String createTopic(int uid, CreateTopicVO vo) {
        TopicType type = topicTypeMapper.selectById(vo.getType());
        if (type == null) {
            return "帖子分类不存在";
        }
        Account actor = accountMapper.selectById(uid);
        if (actor != null && actor.isBanned()) {
            return "您的账号已被封禁，请联系管理员";
        }
        if (actor != null && actor.isMute()) {
            return "您已被禁言，无法发布帖子";
        }
        String key = Const.FORUM_TOPIC_CREATE_COUNTER + uid;
        if (!flow.limitOnceCheck(key, 10)) {
            return "发帖过于频繁，请稍后再试";
        }
        // 入库前过滤：正文在前端以 v-html 渲染，不过滤等于开放存储型 XSS
        Topic topic = new Topic(null, vo.getTitle(),
                contentSanitizer.sanitize(vo.getContent()),
                uid, vo.getType(), new Date(), 0, 0, 0,
                this.normalizeExtra(type, vo.getExtra(), null));
        if (!this.save(topic)) {
            return "发帖失败，请联系管理员";
        }
        // 必须清缓存：否则用户发完帖回到列表看不到自己的帖子，会以为发帖失败
        this.evictPreviewCache();
        return null;
    }

    /**
     * 帖子列表：按分类/关键词筛选并分页，同时补齐作者名与互动计数。
     *
     * <p>每次调用固定发出 6 条 SQL，与每页条数无关：总数、本页数据、作者、点赞计数、
     * 收藏计数、分类元数据（10 行小表，一次取全量）。
     */
    @Override
    public PageVO<TopicPreviewVO> listTopics(int page, int size, Integer type, String typeCode, String keyword) {
        if ((type == null || type <= 0) && typeCode != null && !typeCode.isBlank()) {
            TopicType resolved = this.findTypeByCode(typeCode);
            if (resolved != null) {
                type = resolved.getId();
            }
        }
        // 只缓存默认浏览路径：关键词搜索的结果基数大、命中率低，缓存收益为负
        boolean cacheable = keyword == null || keyword.isBlank();
        // 缓存键含分类与页码，type 为 null 时统一记作 0，避免出现 "topic:preview:null:1:10"
        String cacheKey = Const.FORUM_TOPIC_PREVIEW_CACHE + (type == null ? 0 : type) + ":" + page + ":" + size;
        if (cacheable) {
            PageVO<TopicPreviewVO> cached =
                    cacheUtils.get(cacheKey, new TypeReference<PageVO<TopicPreviewVO>>() {});
            if (cached != null) {
                return cached;
            }
        }

        Page<Topic> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        // 普通列表永远不返回被屏蔽的帖子
        wrapper.eq(Topic::getInvisible, 0);
        if (type != null && type > 0) {
            wrapper.eq(Topic::getType, type);
        }
        // 只搜标题不搜正文：正文是富文本 HTML，LIKE 既慢又容易命中标签本身
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Topic::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(Topic::getTop).orderByDesc(Topic::getTime);
        Page<Topic> result = this.page(pageParam, wrapper);

        List<Topic> records = result.getRecords();
        if (records.isEmpty()) {
            return this.cacheAndReturn(cacheable, cacheKey,
                    PageVO.of(List.of(), result.getTotal(), result.getCurrent(), result.getSize(), result.getPages()));
        }
        List<TopicPreviewVO> items = this.toPreviews(records);
        return this.cacheAndReturn(cacheable, cacheKey,
                PageVO.of(items, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages()));
    }

    @Override
    public TopicDetailVO getTopicDetail(int id, int uid, boolean admin) {
        Topic topic = this.getById(id);
        if (topic == null) {
            return null;
        }
        // 被屏蔽的帖子：只有作者本人和管理员能看
        if (topic.getInvisible() != null && topic.getInvisible() == 1 && topic.getUid() != uid && !admin) {
            return null;
        }
        TopicType type = topicTypeMapper.selectById(topic.getType());
        TopicDetailVO vo = topic.asViewObject(TopicDetailVO.class);
        vo.setTypeName(type != null ? type.getName() : "未分类");
        vo.setTypeCode(type != null ? type.getCode() : null);
        vo.setExtra(this.parseExtra(topic.getExtra()));
        boolean anonymous = type != null && Boolean.TRUE.equals(type.getAnonymous());
        boolean self = topic.getUid() != null && topic.getUid() == uid;
        TopicAuthorVO author = this.buildAuthor(topic.getUid(), anonymous, self);
        vo.setAuthor(author);
        vo.setUsername(author.getUsername());
        vo.setAvatar(author.getAvatar());
        if (anonymous && !self) {
            // 匿名帖：除作者本人外，谁看都是「匿名同学」
            vo.setUid(null);
        }
        vo.setLikeCount(Math.toIntExact(likeMapper.selectCount(new LambdaQueryWrapper<TopicInteractLike>()
                .eq(TopicInteractLike::getTid, id))));
        vo.setCollectCount(Math.toIntExact(collectMapper.selectCount(new LambdaQueryWrapper<TopicInteractCollect>()
                .eq(TopicInteractCollect::getTid, id))));
        return vo;
    }

    @Override
    public String updateTopic(int uid, UpdateTopicVO vo) {
        Account actor = accountMapper.selectById(uid);
        if (actor != null && actor.isBanned()) {
            return "您的账号已被封禁，请联系管理员";
        }
        TopicType type = topicTypeMapper.selectById(vo.getType());
        if (type == null) {
            return "帖子分类不存在";
        }
        Topic topic = this.getById(vo.getId());
        if (topic == null) {
            return "帖子不存在";
        }
        if (!topic.getUid().equals(uid)) {
            return "无权编辑此帖子";
        }
        if (topic.getLocked() != null && topic.getLocked() == 1) {
            return "帖子已被锁定，无法编辑";
        }
        if (actor != null && actor.isMute()) {
            return "您已被禁言，无法编辑帖子";
        }
        boolean updated = this.update()
                .set("title", vo.getTitle())
                .set("content", contentSanitizer.sanitize(vo.getContent()))
                .set("type", vo.getType())
                .set("extra", this.normalizeExtra(type, vo.getExtra(), topic.getExtra()))
                .eq("id", vo.getId())
                .update();
        if (!updated) {
            return "编辑失败，请联系管理员";
        }
        // 标题与分类都可能变了，缓存的列表项会与实际不符
        this.evictPreviewCache();
        return null;
    }

    @Override
    @Transactional
    public String deleteTopic(int uid, int id) {
        Account actor = accountMapper.selectById(uid);
        if (actor != null && actor.isBanned()) {
            return "您的账号已被封禁，请联系管理员";
        }
        Topic topic = this.getById(id);
        if (topic == null) {
            return "帖子不存在";
        }
        if (!topic.getUid().equals(uid)) {
            return "无权删除此帖子";
        }
        commentMapper.delete(new LambdaQueryWrapper<com.myproject.entity.dto.Comment>()
                .eq(com.myproject.entity.dto.Comment::getTid, id));
        likeMapper.delete(new LambdaQueryWrapper<TopicInteractLike>()
                .eq(TopicInteractLike::getTid, id));
        collectMapper.delete(new LambdaQueryWrapper<TopicInteractCollect>()
                .eq(TopicInteractCollect::getTid, id));
        if (!this.removeById(id)) {
            return "删除失败，请联系管理员";
        }
        this.evictPreviewCache();
        return null;
    }

    @Override
    public String updateStatus(int uid, int id, String status) {
        Account actor = accountMapper.selectById(uid);
        if (actor != null && actor.isBanned()) {
            return "您的账号已被封禁，请联系管理员";
        }
        Topic topic = this.getById(id);
        if (topic == null) {
            return "帖子不存在";
        }
        if (!topic.getUid().equals(uid)) {
            return "无权操作此帖子";
        }
        TopicType type = topicTypeMapper.selectById(topic.getType());
        if (type == null || !"lost".equals(type.getCode())) {
            return "只有失物招领支持该操作";
        }
        JSONObject extra = this.parseExtra(topic.getExtra());
        if (extra == null) {
            extra = new JSONObject();
        }
        extra.put("status", "resolved".equals(status) ? "resolved" : "open");
        this.update().set("extra", extra.toJSONString()).eq("id", id).update();
        // 列表上有「已找回」标记，必须清缓存
        this.evictPreviewCache();
        return null;
    }

    @Override
    public List<TopicPreviewVO> listCollects(int uid) {
        List<TopicInteractCollect> collects = collectMapper.selectList(
                new LambdaQueryWrapper<TopicInteractCollect>()
                        .eq(TopicInteractCollect::getUid, uid)
                        .orderByDesc(TopicInteractCollect::getTime));
        if (collects.isEmpty()) {
            return List.of();
        }
        List<Integer> tids = collects.stream().map(TopicInteractCollect::getTid).toList();
        Map<Integer, Topic> topicMap = this.listByIds(tids).stream()
                .collect(Collectors.toMap(Topic::getId, t -> t, (a, b) -> a));
        // 保持「收藏时间倒序」，跳过已被作者删除的帖子
        List<Topic> ordered = collects.stream()
                .map(c -> topicMap.get(c.getTid()))
                .filter(Objects::nonNull)
                .filter(t -> t.getInvisible() == null || t.getInvisible() == 0)
                .toList();
        return this.toPreviews(ordered);
    }

    @Override
    public List<TopicTopVO> listTopTopics() {
        return this.list(new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getTop, 1)
                        .eq(Topic::getInvisible, 0)
                        .orderByDesc(Topic::getTime)
                        .last("LIMIT 10"))
                .stream()
                .map(topic -> {
                    TopicTopVO vo = new TopicTopVO();
                    vo.setId(topic.getId());
                    vo.setTitle(topic.getTitle());
                    vo.setTime(topic.getTime());
                    return vo;
                })
                .toList();
    }

    /**
     * 列表 / 收藏共用的预览构建：作者、分类、互动计数都批量查，避免 N+1。
     */
    private List<TopicPreviewVO> toPreviews(List<Topic> records) {
        return this.toPreviews(records, false);
    }

    private List<TopicPreviewVO> toPreviews(List<Topic> records, boolean revealAnonymous) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<Integer> tids = records.stream().map(Topic::getId).toList();
        List<Integer> uids = records.stream().map(Topic::getUid).distinct().toList();
        Map<Integer, Account> authors = accountMapper.selectBatchIds(uids).stream()
                .collect(Collectors.toMap(Account::getId, a -> a, (a, b) -> a));
        Map<Integer, TopicType> types = topicTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(TopicType::getId, t -> t, (a, b) -> a));
        Map<Integer, Integer> likeCounts = this.countLikesByTopic(tids);
        Map<Integer, Integer> collectCounts = this.countCollectsByTopic(tids);

        return records.stream().map(topic -> {
            TopicPreviewVO vo = topic.asViewObject(TopicPreviewVO.class);
            TopicType typeInfo = types.get(topic.getType());
            vo.setTypeName(typeInfo != null ? typeInfo.getName() : "未分类");
            vo.setTypeCode(typeInfo != null ? typeInfo.getCode() : null);
            vo.setExtra(this.parseExtra(topic.getExtra()));
            if (typeInfo != null && Boolean.TRUE.equals(typeInfo.getAnonymous()) && !revealAnonymous) {
                // 列表页没有作者操作按钮，匿名帖直接不暴露 uid
                vo.setUid(null);
                vo.setUsername("匿名同学");
                vo.setAvatar(null);
            } else {
                // 作者可能已注销
                Account author = authors.get(topic.getUid());
                vo.setUsername(author != null ? author.getUsername() : "未知用户");
                vo.setAvatar(author == null ? null : author.getAvatar());
            }
            vo.setPreview(contentPreview.preview(topic.getContent()));
            vo.setCover(contentPreview.coverImage(topic.getContent()));
            vo.setLikeCount(likeCounts.getOrDefault(topic.getId(), 0));
            vo.setCollectCount(collectCounts.getOrDefault(topic.getId(), 0));
            return vo;
        }).toList();
    }

    /**
     * 详情页作者信息：匿名帖对非作者隐藏，其余字段按隐私开关过滤。
     */
    private TopicAuthorVO buildAuthor(int authorUid, boolean anonymous, boolean self) {
        TopicAuthorVO vo = new TopicAuthorVO();
        if (anonymous && !self) {
            vo.setUsername("匿名同学");
            return vo;
        }
        Account account = accountMapper.selectById(authorUid);
        if (account == null) {
            vo.setUsername("未知用户");
            return vo;
        }
        vo.setId(account.getId());
        vo.setUsername(account.getUsername());
        vo.setAvatar(account.getAvatar());

        AccountPrivacy privacy = accountPrivacyService.accountPrivacy(authorUid);
        Set<String> hidden = Set.of(privacy.hiddenFields());
        vo.setEmail(hidden.contains("email") ? null : account.getEmail());

        AccountDetails details = accountDetailsService.findAccountDetailsById(authorUid);
        if (details != null) {
            vo.setGender(hidden.contains("gender") ? null : details.getGender());
            vo.setPhone(hidden.contains("phone") ? null : details.getPhone());
            vo.setQq(hidden.contains("qq") ? null : details.getQq());
            vo.setWx(hidden.contains("wx") ? null : details.getWx());
            vo.setDesc(details.getDesc());
        }
        return vo;
    }

    @Override
    public PageVO<TopicPreviewVO> listAllTopics(int page, int size, String keyword) {
        Page<Topic> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Topic::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(Topic::getTime);
        Page<Topic> result = this.page(pageParam, wrapper);
        return PageVO.of(this.toPreviews(result.getRecords(), true),
                result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    @Transactional
    public void adminDeleteTopic(int id) {
        commentMapper.delete(new LambdaQueryWrapper<com.myproject.entity.dto.Comment>()
                .eq(com.myproject.entity.dto.Comment::getTid, id));
        likeMapper.delete(new LambdaQueryWrapper<TopicInteractLike>()
                .eq(TopicInteractLike::getTid, id));
        collectMapper.delete(new LambdaQueryWrapper<TopicInteractCollect>()
                .eq(TopicInteractCollect::getTid, id));
        this.removeById(id);
        this.evictPreviewCache();
    }

    @Override
    public void setTopicTop(int id, boolean top) {
        this.update().set("top", top).eq("id", id).update();
        this.evictPreviewCache();
    }

    @Override
    public void setTopicLocked(int id, boolean locked) {
        this.update().set("locked", locked).eq("id", id).update();
        this.evictPreviewCache();
    }

    @Override
    public void setTopicInvisible(int id, boolean invisible) {
        this.update().set("invisible", invisible).eq("id", id).update();
        this.evictPreviewCache();
    }

    @Override
    public String changeTopicType(int id, int type) {
        if (topicTypeMapper.selectById(type) == null) {
            return "分类不存在";
        }
        this.update().set("type", type).eq("id", id).update();
        this.evictPreviewCache();
        return null;
    }

    @Override
    public String createTopicType(TopicTypeCreateVO vo) {
        String code = vo.getCode().trim();
        Long exists = topicTypeMapper.selectCount(new LambdaQueryWrapper<TopicType>()
                .eq(TopicType::getCode, code));
        if (exists != null && exists > 0) {
            return "code 已存在，请换一个";
        }
        TopicType type = new TopicType();
        type.setCode(code);
        type.setName(vo.getName().trim());
        type.setDesc(vo.getDesc());
        type.setColor(vo.getColor());
        type.setAnonymous(Boolean.TRUE.equals(vo.getAnonymous()));
        topicTypeMapper.insert(type);
        return null;
    }

    @Override
    public String updateTopicType(TopicTypeUpdateVO vo) {
        TopicType type = topicTypeMapper.selectById(vo.getId());
        if (type == null) {
            return "分类不存在";
        }
        // code 是稳定标识，创建后不允许改，避免前端路由和历史数据对不上
        type.setName(vo.getName().trim());
        type.setDesc(vo.getDesc());
        type.setColor(vo.getColor());
        type.setAnonymous(Boolean.TRUE.equals(vo.getAnonymous()));
        topicTypeMapper.updateById(type);
        return null;
    }

    @Override
    public String deleteTopicType(int id) {
        Long used = this.count(new LambdaQueryWrapper<Topic>().eq(Topic::getType, id));
        if (used != null && used > 0) {
            return "该分类下还有帖子，不能删除";
        }
        topicTypeMapper.deleteById(id);
        return null;
    }

    private TopicType findTypeByCode(String code) {
        return topicTypeMapper.selectOne(new LambdaQueryWrapper<TopicType>()
                .eq(TopicType::getCode, code)
                .last("LIMIT 1"));
    }

    /**
     * 按分类白名单过滤 extra，存成 JSON 文本。
     * 只接受纯文本：jsoup 去标签 + 长度截断，避免把 HTML 塞进结构化字段。
     */
    private String normalizeExtra(TopicType type, Map<String, Object> extra, String existingExtra) {
        if (type == null) {
            return null;
        }
        Set<String> allowed = EXTRA_KEYS.getOrDefault(type.getCode(), Set.of());
        JSONObject result = new JSONObject();
        if (extra != null) {
            for (String key : allowed) {
                if ("status".equals(key)) {
                    continue;
                }
                Object raw = extra.get(key);
                if (raw == null) {
                    continue;
                }
                String value = String.valueOf(raw).trim();
                if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
                    continue;
                }
                value = Jsoup.clean(value, Safelist.none()).trim();
                if (value.isEmpty()) {
                    continue;
                }
                if (value.length() > 120) {
                    value = value.substring(0, 120);
                }
                result.put(key, value);
            }
        }
        // 失物招领的 status 只能通过专用接口改，编辑时保留原状态
        if ("lost".equals(type.getCode())) {
            JSONObject old = this.parseExtra(existingExtra);
            String status = old == null ? null : old.getString("status");
            result.put("status", "resolved".equals(status) ? "resolved" : "open");
        }
        return result.isEmpty() ? null : result.toJSONString();
    }

    private JSONObject parseExtra(String extra) {
        if (extra == null || extra.isBlank()) {
            return null;
        }
        try {
            return JSONObject.parseObject(extra);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 一次查询统计多个帖子的点赞数，避免逐条统计
     */
    private Map<Integer, Integer> countLikesByTopic(List<Integer> tids) {
        QueryWrapper<TopicInteractLike> wrapper = new QueryWrapper<TopicInteractLike>()
                .select("tid", "count(*) as cnt")
                .in("tid", tids)
                .groupBy("tid");
        return this.toCountMap(likeMapper.selectMaps(wrapper));
    }

    /**
     * 一次查询统计多个帖子的收藏数，避免逐条统计
     */
    private Map<Integer, Integer> countCollectsByTopic(List<Integer> tids) {
        QueryWrapper<TopicInteractCollect> wrapper = new QueryWrapper<TopicInteractCollect>()
                .select("tid", "count(*) as cnt")
                .in("tid", tids)
                .groupBy("tid");
        return this.toCountMap(collectMapper.selectMaps(wrapper));
    }

    private Map<Integer, Integer> toCountMap(List<Map<String, Object>> rows) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object tid = row.get("tid");
            Object cnt = row.get("cnt");
            if (tid instanceof Number t && cnt instanceof Number c) {
                map.put(t.intValue(), c.intValue());
            }
        }
        return map;
    }

    /**
     * 条件写入列表缓存并原样返回，避免在多个 return 点重复写缓存逻辑。
     */
    private PageVO<TopicPreviewVO> cacheAndReturn(boolean cacheable, String cacheKey, PageVO<TopicPreviewVO> vo) {
        if (cacheable) {
            cacheUtils.set(cacheKey, vo, PREVIEW_CACHE_MINUTES, TimeUnit.MINUTES);
        }
        return vo;
    }

    /**
     * 帖子内容发生变化（发布 / 编辑 / 删除 / 状态变化）后清空列表缓存。
     *
     * <p>这里整类清除而不做精确失效：一个分类下最多只有几页，
     * 而要精确算出「这条帖子影响了哪些分页」需要额外维护映射，复杂度远高于收益。
     */
    private void evictPreviewCache() {
        cacheUtils.deleteByPrefix(Const.FORUM_TOPIC_PREVIEW_CACHE);
    }
}
