package com.myproject.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 缓存工具类。
 *
 * <p>把「JSON 序列化、设置过期时间、按前缀清理」这些重复动作收在一处，
 * 业务代码只需要关心「缓存什么、缓存多久」，不必关心底层怎么转、怎么扫。
 *
 * <p>序列化统一使用 fastjson2，与项目中已有做法保持一致（参见天气缓存）。
 *
 * <p><b>为什么用 SCAN 而不是 KEYS</b>：KEYS 会一次遍历并阻塞整个 Redis 实例，
 * 在线上几十万 key 的场景足以把服务拖死；SCAN 是游标式渐进遍历，不会长时间阻塞。
 *
 * <p><b>失败策略</b>：所有方法在 Redis 异常时只记日志、不抛异常。
 * 缓存属于「加速手段」，不能因为缓存挂了就让发帖、看帖这些主流程失败。
 */
@Slf4j
@Component
public class RedisCacheUtils {

    @Resource
    StringRedisTemplate template;

    /**
     * 读取并反序列化为指定类型。
     *
     * @return key 不存在、已过期、或内容损坏时返回 null（调用方按未命中处理）
     */
    public <T> T get(String key, Class<T> type) {
        String json = this.read(key);
        if (json == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, type);
        } catch (Exception e) {
            log.warn("缓存反序列化失败，按未命中处理: {} ({})", key, e.getMessage());
            return null;
        }
    }

    /**
     * 读取并反序列化泛型结构，例如 {@code PageVO<TopicPreviewVO>}。
     * 用法：{@code cache.get(key, new TypeReference<PageVO<TopicPreviewVO>>(){})}
     */
    public <T> T get(String key, TypeReference<T> type) {
        String json = this.read(key);
        if (json == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, type);
        } catch (Exception e) {
            log.warn("缓存反序列化失败，按未命中处理: {} ({})", key, e.getMessage());
            return null;
        }
    }

    /** 写入并设置过期时间 */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (value == null) {
            return;
        }
        try {
            template.opsForValue().set(key, JSON.toJSONString(value), timeout, unit);
        } catch (Exception e) {
            log.warn("写入缓存失败，忽略: {} ({})", key, e.getMessage());
        }
    }

    /** 删除单个 key，返回是否真的删掉了 */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(template.delete(key));
        } catch (Exception e) {
            log.warn("删除缓存失败: {} ({})", key, e.getMessage());
            return false;
        }
    }

    /**
     * 按前缀批量删除，返回删除的 key 数量。
     * 用于「数据一变就要让某一类缓存全部失效」的场景，例如帖子列表缓存。
     */
    public long deleteByPrefix(String prefix) {
        long removed = 0;
        List<String> batch = new ArrayList<>();
        try (Cursor<String> cursor = template.scan(
                ScanOptions.scanOptions().match(prefix + "*").count(200).build())) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                // 分批删除，避免一次堆积过多命令
                if (batch.size() >= 200) {
                    removed += template.delete(batch);
                    batch.clear();
                }
            }
        } catch (Exception e) {
            log.warn("按前缀清理缓存失败: {} ({})", prefix, e.getMessage());
        }
        if (!batch.isEmpty()) {
            try {
                removed += template.delete(batch);
            } catch (Exception e) {
                log.warn("按前缀清理缓存失败: {} ({})", prefix, e.getMessage());
            }
        }
        if (removed > 0) {
            log.debug("已清理前缀为 {} 的缓存 {} 个", prefix, removed);
        }
        return removed;
    }

    /**
     * 缓存旁路（cache-aside）：命中直接返回；未命中则执行 loader 并把结果写入缓存。
     *
     * <p>loader 返回 null 时不写缓存 —— 避免把「查不到」这种临时状态缓存成一段时间的空值。
     */
    public <T> T getOrLoad(String key, TypeReference<T> type, long timeout, TimeUnit unit, Supplier<T> loader) {
        T cached = this.get(key, type);
        if (cached != null) {
            return cached;
        }
        T loaded = loader.get();
        this.set(key, loaded, timeout, unit);
        return loaded;
    }

    private String read(String key) {
        try {
            return template.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("读取缓存失败，按未命中处理: {} ({})", key, e.getMessage());
            return null;
        }
    }
}
