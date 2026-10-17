package com.myproject.controller;

import com.myproject.entity.RestBean;
import com.myproject.entity.vo.request.CreateCommentVO;
import com.myproject.entity.vo.request.CreateTopicVO;
import com.myproject.entity.vo.request.UpdateTopicVO;
import com.myproject.entity.vo.response.CityVO;
import com.myproject.entity.vo.response.CommentPageVO;
import com.myproject.entity.vo.response.CommentVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.entity.vo.response.TopicDetailVO;
import com.myproject.entity.vo.response.TopicPreviewVO;
import com.myproject.entity.vo.response.TopicTopVO;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
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

	/**
	 * 按和风城市 ID 取天气：浏览器定位不可用或定位到错误城市时，前端可手动选城市
	 */
	@GetMapping("/weather/city")
	public RestBean<WeatherVO> weatherByCity(@RequestParam String id){
		WeatherVO vo = service.fetchWeatherById(id);
		return vo == null ?
				RestBean.failure(400, "获取该城市天气失败，请稍后重试") : RestBean.success(vo);
	}

	/**
	 * 按城市名/拼音检索城市，供前端「切换城市」下拉使用
	 */
	@GetMapping("/weather/search")
	public RestBean<List<CityVO>> searchCity(@RequestParam String keyword){
		return RestBean.success(service.searchCities(keyword));
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
	                                                  @RequestParam(required = false) String typeCode,
	                                                  @RequestParam(required = false) String keyword) {
		return RestBean.success(topicPostService.listTopics(page, size, type, typeCode, keyword));
	}

	/**
	 * 我的收藏：按收藏时间倒序
	 */
	@GetMapping("/collects")
	public RestBean<List<TopicPreviewVO>> collects(@RequestAttribute(Const.ATTR_USER_ID) int uid) {
		return RestBean.success(topicPostService.listCollects(uid));
	}

	/**
	 * 置顶帖子，最多 10 条
	 */
	@GetMapping("/top")
	public RestBean<List<TopicTopVO>> topTopics() {
		return RestBean.success(topicPostService.listTopTopics());
	}

	@GetMapping("/detail")
	public RestBean<TopicDetailVO> detail(@RequestParam @Min(1) int id,
	                                       @RequestAttribute(Const.ATTR_USER_ID) int uid,
	                                       Authentication authentication) {
		boolean admin = authentication != null && authentication.getAuthorities().stream()
				.anyMatch(a -> ("ROLE_" + Const.ROLE_ADMIN).equals(a.getAuthority()));
		TopicDetailVO vo = topicPostService.getTopicDetail(id, uid, admin);
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
	public RestBean<Void> deleteTopic(@RequestBody @Valid IdRequest body,
	                                   HttpServletRequest request) {
		int uid = (int) request.getAttribute(Const.ATTR_USER_ID);
		return utils.messageHandle(() -> topicPostService.deleteTopic(uid, body.id()));
	}

	/**
	 * 失物招领专用：作者标记「已找回」或撤销
	 */
	@PostMapping("/status")
	public RestBean<Void> updateStatus(@RequestBody @Valid StatusRequest body,
	                                   @RequestAttribute(Const.ATTR_USER_ID) int uid) {
		return utils.messageHandle(() -> topicPostService.updateStatus(uid, body.id(), body.status()));
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
	public RestBean<CommentPageVO<CommentVO>> listComments(@RequestParam @Min(1) int tid,
	                                               @RequestParam(defaultValue = "1") @Min(1) int page,
	                                               @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
	                                               @RequestAttribute(Const.ATTR_USER_ID) int uid) {
		return RestBean.success(commentService.listComments(tid, page, size, uid));
	}

	@PostMapping("/comment-delete")
	public RestBean<Void> deleteComment(@RequestBody @Valid IdRequest body,
	                                     HttpServletRequest request) {
		int uid = (int) request.getAttribute(Const.ATTR_USER_ID);
		return utils.messageHandle(() -> commentService.deleteComment(uid, body.id()));
	}

	/**
	 * 删除类接口的请求体，统一校验 id 合法性
	 */
	public record IdRequest(@NotNull @Min(1) Integer id) {
	}

	public record StatusRequest(@NotNull @Min(1) Integer id,
	                            @NotNull @Pattern(regexp = "(open|resolved)") String status) {
	}
}
