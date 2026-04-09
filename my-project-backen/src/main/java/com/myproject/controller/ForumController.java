package com.myproject.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myproject.entity.RestBean;
import com.myproject.entity.vo.response.TopicTypeVO;
import com.myproject.entity.vo.response.WeatherVO;
import com.myproject.service.TopicService;
import com.myproject.service.WeatherService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;


@RestController
@RequestMapping("/api/forum")
public class ForumController {
	
	@Resource
	WeatherService service;
	
	@Resource
	TopicService topicService;
	
	
	@GetMapping("/weather")
	public RestBean<WeatherVO> weather(double longitude, double latitude){
		WeatherVO vo = service.fetchWeather(longitude, latitude);
		return vo == null ?
				RestBean.failure(400, "获取地理位置信息与天气失败，请联系管理员！") : RestBean.success(vo);
	}
	
	@GetMapping("/types")
	public RestBean<List<TopicTypeVO>> listTypes(){
		return RestBean.success(topicService
				.listTypes()
				.stream()
				.map(type -> type.asViewObject(TopicTypeVO.class))
				.toList());
	}
	
	
	
}
