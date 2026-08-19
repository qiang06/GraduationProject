package com.myproject.controller;

import com.myproject.entity.RestBean;
import com.myproject.entity.vo.request.CreateCommentVO;
import com.myproject.entity.vo.request.CreateTopicVO;
import com.myproject.entity.vo.request.UpdateTopicVO;
import com.myproject.entity.vo.response.CommentVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.entity.vo.response.TopicDetailVO;
import com.myproject.entity.vo.response.TopicPreviewVO;
import com.myproject.entity.vo.response.TopicTypeVO;
import com.myproject.entity.vo.response.WeatherVO;
import com.myproject.service.CommentService;
import com.myproject.service.TopicPostService;
import com.myproject.service.TopicService;
import com.myproject.service.WeatherService;
import com.myproject.utils.Const;
import com.myproject.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import java.util.*;


@Validated
@RestController
@RequestMapping("/api/forum")
public class ForumController {
	
	@Resource
	WeatherService service;
	
	@Resource
	TopicService topicService;

	@Resource
	TopicPostService topicPostService;

	@Resource
	CommentService commentService;

	@Resource
	ControllerUtils utils;
	
	
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

	@PostMapping("/create")
	public RestBean<Void> createTopic(@RequestBody @Valid CreateTopicVO vo,
	                                   HttpServletRequest request) {
		int uid = (int) request.getAttribute(Const.ATTR_USER_ID);
		return utils.messageHandle(() -> topicPostService.createTopic(uid, vo));
	}

	@GetMapping("/list")
	public RestBean<PageVO<TopicPreviewVO>> listTopics(@RequestParam(defaultValue = "1") @Min(1) int page,
	                                                  @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
	                                                  @RequestParam(required = false) Integer type,
	                                                  @RequestParam(required = false) String keyword) {
		return RestBean.success(topicPostService.listTopics(page, size, type, keyword));
	}

	@GetMapping("/detail")
	public RestBean<TopicDetailVO> detail(@RequestParam @Min(1) int id) {
		TopicDetailVO vo = topicPostService.getTopicDetail(id);
		return vo == null ?
				RestBean.failure(400, "帖子不存在") : RestBean.success(vo);
	}

	@PostMapping("/update")
	public RestBean<Void> updateTopic(@RequestBody @Valid UpdateTopicVO vo,
	                                   HttpServletRequest request) {
		int uid = (int) request.getAttribute(Const.ATTR_USER_ID);
		return utils.messageHandle(() -> topicPostService.updateTopic(uid, vo));
	}

	@PostMapping("/delete")
	public RestBean<Void> deleteTopic(@RequestBody Map<String, Integer> body,
	                                   HttpServletRequest request) {
		int uid = (int) request.getAttribute(Const.ATTR_USER_ID);
		Integer id = body.get("id");
		if (id == null) {
			return RestBean.failure(400, "参数错误");
		}
		return utils.messageHandle(() -> topicPostService.deleteTopic(uid, id));
	}

	@PostMapping("/comment")
	public RestBean<Map<String, Object>> createComment(@RequestBody @Valid CreateCommentVO vo,
	                                                    HttpServletRequest request) {
		int uid = (int) request.getAttribute(Const.ATTR_USER_ID);
		Map<String, Object> result = commentService.createComment(uid, vo);
		if (result.containsKey("error")) {
			return RestBean.failure(400, (String) result.get("error"));
		}
		return RestBean.success(result);
	}

	@GetMapping("/comments")
	public RestBean<PageVO<CommentVO>> listComments(@RequestParam @Min(1) int tid,
	                                               @RequestParam(defaultValue = "1") @Min(1) int page,
	                                               @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
		return RestBean.success(commentService.listComments(tid, page, size));
	}

	@PostMapping("/comment-delete")
	public RestBean<Void> deleteComment(@RequestBody Map<String, Integer> body,
	                                     HttpServletRequest request) {
		int uid = (int) request.getAttribute(Const.ATTR_USER_ID);
		Integer id = body.get("id");
		if (id == null) {
			return RestBean.failure(400, "参数错误");
		}
		return utils.messageHandle(() -> commentService.deleteComment(uid, id));
	}
}
