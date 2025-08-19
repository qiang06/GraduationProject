package com.myproject.controller;

import com.myproject.entity.RestBean;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RequestMapping("/api/ip")
@RestController
@CrossOrigin(origins = "*") // 允许所有跨域请求
public class IpController {
	
	@GetMapping(produces = "application/json")
	public RestBean<Map<String, String>> getClientIpAddress(HttpServletRequest request) {
		// 使用LinkedHashMap保持顺序
		Map<String, String> response = new LinkedHashMap<>();
		
		// 获取IP地址
		String ip = getClientIp(request);
		
		// 处理IPv6本地地址
		if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
			ip = "127.0.0.1";
		}
		
		response.put("ip", ip);
		response.put("message", "Successfully retrieved client IP address");
		
		return  RestBean.success(response);
	}
	
	private String getClientIp(HttpServletRequest request) {
		String[] headers = {
				"X-Forwarded-For",
				"Proxy-Client-IP",
				"WL-Proxy-Client-IP",
				"HTTP_CLIENT_IP",
				"HTTP_X_FORWARDED_FOR"
		};
		
		String ip = null;
		
		// 检查所有可能的代理头部
		for (String header : headers) {
			ip = request.getHeader(header);
			if (isValidIp(ip)) {
				break;
			}
		}
		
		// 如果所有代理头部都无效，使用remoteAddr
		if (!isValidIp(ip)) {
			ip = request.getRemoteAddr();
		}
		
		// 处理多个IP的情况（如X-Forwarded-For: client, proxy1, proxy2）
		if (ip != null && ip.contains(",")) {
			ip = Arrays.stream(ip.split(","))
					.map(String::trim)
					.filter(this::isValidIp)
					.findFirst()
					.orElse(request.getRemoteAddr());
		}
		
		return ip;
	}
	
	private boolean isValidIp(String ip) {
		return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
	}
	
}
