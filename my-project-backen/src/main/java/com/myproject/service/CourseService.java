package com.myproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.entity.dto.Course;
import com.myproject.entity.vo.request.CourseSaveVO;
import com.myproject.entity.vo.response.CourseVO;
import com.myproject.entity.vo.response.PageVO;

import java.util.List;

public interface CourseService extends IService<Course> {
    List<CourseVO> listMine(int uid);

    String saveCourse(int uid, CourseSaveVO vo);

    String deleteCourse(int uid, int id);

    PageVO<CourseVO> listAdmin(int page, int size, String keyword, Integer uid);

    String adminDelete(int id);
}
