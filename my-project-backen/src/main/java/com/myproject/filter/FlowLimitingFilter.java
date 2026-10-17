package com.myproject.filter;

import com.myproject.entity.RestBean;
import com.myproject.utils.Const;
import com.myproject.utils.FlowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 限流控制过滤器
 * 防止用户高频请求接口，借助Redis进行限流
 */
@Slf4j
@Component
@Order(Const.ORDER_FLOW_LIMIT)
public class FlowLimitingFilter extends HttpFilter {

    @Resource
    StringRedisTemplate template;
    //指定时间内最大请求次数限制
    @Value("${spring.web.flow.limit}")
    int limit;
    //计数时间周期
    @Value("${spring.web.flow.period}")
    int period;
    //超出请求限制封禁时间
    @Value("${spring.web.flow.block}")
    int block;
    //是否信任反向代理传递的真实IP请求头，仅在服务位于可信代理之后时开启
    @Value("${spring.web.flow.trust-proxy:false}")
    boolean trustProxy;

    @Resource
    FlowUtils utils;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String address = this.resolveAddress(request);
        if ("OPTIONS".equals(request.getMethod())) {
            if (!tryCount(address)) {
                this.writeBlockMessage(response);
                return;
            }
        } else if (template.hasKey(Const.FLOW_LIMIT_BLOCK + address) || !tryCount(address)) {
            this.writeBlockMessage(response);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * 解析客户端地址
     * 默认使用连接来源地址；只有在配置为信任代理时，才采用代理转发的真实IP，
     * 否则客户端可以伪造 X-Forwarded-For 绕过限流。
     * @param request 请求
     * @return 用于限流的客户端地址
     */
    private String resolveAddress(HttpServletRequest request) {
        if (trustProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 尝试对指定IP地址请求计数，如果被限制则无法继续访问
     * 计数与封禁均通过 Redis 原子操作完成，无需本地锁
     * @param address 请求IP地址
     * @return 是否操作成功
     */
    private boolean tryCount(String address) {
        if (template.hasKey(Const.FLOW_LIMIT_BLOCK + address))
            return false;
        String counterKey = Const.FLOW_LIMIT_COUNTER + address;
        String blockKey = Const.FLOW_LIMIT_BLOCK + address;
        return utils.limitPeriodCheck(counterKey, blockKey, block, limit, period);
    }

    /**
     * 为响应编写拦截内容，提示用户操作频繁
     * @param response 响应
     * @throws IOException 可能的异常
     */
    private void writeBlockMessage(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.write(RestBean.failure(429, "请求频率过快，请稍后再试").asJsonString());
    }
}
