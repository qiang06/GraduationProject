package com.myproject.controller;

import com.myproject.entity.RestBean;
import com.myproject.entity.vo.request.CourseSaveVO;
import com.myproject.entity.vo.response.CourseVO;
import com.myproject.entity.vo.response.LibraryResourceVO;
import com.myproject.entity.vo.response.NoticePreviewVO;
import com.myproject.entity.vo.response.NoticeVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.service.CourseService;
import com.myproject.service.LibraryService;
import com.myproject.service.NoticeService;
import com.myproject.utils.Const;
import com.myproject.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/campus")
public class CampusController {

    @Resource
    NoticeService noticeService;

    @Resource
    CourseService courseService;

    @Resource
    LibraryService libraryService;

    @Resource
    ControllerUtils utils;

    @GetMapping("/notices")
    public RestBean<PageVO<NoticePreviewVO>> notices(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                     @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
                                                     @RequestParam(required = false) String category,
                                                     @RequestParam(required = false) String keyword) {
        return RestBean.success(noticeService.listPublished(page, size, category, keyword));
    }

    @GetMapping("/notices/{id}")
    public RestBean<NoticeVO> noticeDetail(@PathVariable @Min(1) int id) {
        NoticeVO vo = noticeService.getDetail(id);
        return vo == null ? RestBean.failure(400, "通知不存在") : RestBean.success(vo);
    }

    @GetMapping("/courses")
    public RestBean<List<CourseVO>> myCourses(@RequestAttribute(Const.ATTR_USER_ID) int uid) {
        return RestBean.success(courseService.listMine(uid));
    }

    @PostMapping("/courses/save")
    public RestBean<Void> saveCourse(@RequestBody @Valid CourseSaveVO vo,
                                     @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        return utils.messageHandle(() -> courseService.saveCourse(uid, vo));
    }

    @PostMapping("/courses/delete")
    public RestBean<Void> deleteCourse(@RequestBody @Valid IdRequest body,
                                       @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        return utils.messageHandle(() -> courseService.deleteCourse(uid, body.id()));
    }

    @GetMapping("/library")
    public RestBean<PageVO<LibraryResourceVO>> library(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                       @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size,
                                                       @RequestParam(required = false) String category,
                                                       @RequestParam(required = false) String keyword) {
        return RestBean.success(libraryService.list(page, size, category, keyword));
    }

    @GetMapping("/library/{id}")
    public RestBean<LibraryResourceVO> libraryDetail(@PathVariable @Min(1) int id) {
        LibraryResourceVO vo = libraryService.getDetail(id);
        return vo == null ? RestBean.failure(400, "资源不存在") : RestBean.success(vo);
    }

    public record IdRequest(@NotNull @Min(1) Integer id) {
    }
}
