package com.myproject.controller.admin;

import com.myproject.entity.RestBean;
import com.myproject.entity.vo.request.CourseSaveVO;
import com.myproject.entity.vo.request.LibrarySaveVO;
import com.myproject.entity.vo.request.NoticeSaveVO;
import com.myproject.entity.vo.response.CourseVO;
import com.myproject.entity.vo.response.LibraryResourceVO;
import com.myproject.entity.vo.response.NoticeVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.service.CourseService;
import com.myproject.service.LibraryService;
import com.myproject.service.NoticeService;
import com.myproject.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/campus")
public class CampusAdminController {

    @Resource
    NoticeService noticeService;

    @Resource
    CourseService courseService;

    @Resource
    LibraryService libraryService;

    @Resource
    ControllerUtils utils;

    @GetMapping("/notices")
    public RestBean<PageVO<NoticeVO>> notices(@RequestParam(defaultValue = "1") @Min(1) int page,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) String keyword) {
        return RestBean.success(noticeService.listAdmin(page, size, category, keyword));
    }

    @PostMapping("/notices/save")
    public RestBean<Void> saveNotice(@RequestBody @Valid NoticeSaveVO vo) {
        return utils.messageHandle(() -> noticeService.saveNotice(vo));
    }

    @PostMapping("/notices/delete")
    public RestBean<Void> deleteNotice(@RequestBody @Valid IdRequest body) {
        return utils.messageHandle(() -> noticeService.deleteNotice(body.id()));
    }

    @PostMapping("/notices/top")
    public RestBean<Void> topNotice(@RequestBody @Valid TopRequest body) {
        return utils.messageHandle(() -> noticeService.setTop(body.id(), body.top()));
    }

    @GetMapping("/courses")
    public RestBean<PageVO<CourseVO>> courses(@RequestParam(defaultValue = "1") @Min(1) int page,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer uid) {
        return RestBean.success(courseService.listAdmin(page, size, keyword, uid));
    }

    @PostMapping("/courses/delete")
    public RestBean<Void> deleteCourse(@RequestBody @Valid IdRequest body) {
        return utils.messageHandle(() -> courseService.adminDelete(body.id()));
    }

    @GetMapping("/library")
    public RestBean<PageVO<LibraryResourceVO>> library(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                       @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
                                                       @RequestParam(required = false) String category,
                                                       @RequestParam(required = false) String keyword) {
        return RestBean.success(libraryService.listAdmin(page, size, category, keyword));
    }

    @PostMapping("/library/save")
    public RestBean<Void> saveLibrary(@RequestBody @Valid LibrarySaveVO vo) {
        return utils.messageHandle(() -> libraryService.saveResource(vo));
    }

    @PostMapping("/library/delete")
    public RestBean<Void> deleteLibrary(@RequestBody @Valid IdRequest body) {
        return utils.messageHandle(() -> libraryService.deleteResource(body.id()));
    }

    public record IdRequest(@NotNull @Min(1) Integer id) {
    }

    public record TopRequest(@NotNull @Min(1) Integer id, @NotNull Boolean top) {
    }
}
