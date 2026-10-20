package com.myproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.entity.dto.Account;
import com.myproject.entity.dto.Course;
import com.myproject.entity.vo.request.CourseSaveVO;
import com.myproject.entity.vo.response.CourseVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.mapper.AccountMapper;
import com.myproject.mapper.CourseMapper;
import com.myproject.service.CourseService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private static final String DEFAULT_COLOR = "#409EFF";

    @Resource
    AccountMapper accountMapper;

    @Override
    public List<CourseVO> listMine(int uid) {
        return this.list(new LambdaQueryWrapper<Course>()
                .eq(Course::getUid, uid)
                .orderByAsc(Course::getWeekday)
                .orderByAsc(Course::getStartSection)).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public String saveCourse(int uid, CourseSaveVO vo) {
        if (vo.getEndSection() < vo.getStartSection()) {
            return "结束节次不能早于开始节次";
        }
        Course course;
        if (vo.getId() != null) {
            course = this.getById(vo.getId());
            if (course == null) return "课程不存在";
            if (!course.getUid().equals(uid)) return "无权修改别人的课程";
        } else {
            course = new Course();
            course.setUid(uid);
        }
        course.setName(vo.getName().trim());
        course.setTeacher(trim(vo.getTeacher()));
        course.setLocation(trim(vo.getLocation()));
        course.setWeekday(vo.getWeekday());
        course.setStartSection(vo.getStartSection());
        course.setEndSection(vo.getEndSection());
        course.setWeeks(trim(vo.getWeeks()));
        course.setColor(validColor(vo.getColor()) ? vo.getColor() : DEFAULT_COLOR);
        course.setNote(trim(vo.getNote()));
        boolean ok = vo.getId() == null ? this.save(course) : this.updateById(course);
        return ok ? null : "保存失败，请联系管理员";
    }

    @Override
    public String deleteCourse(int uid, int id) {
        Course course = this.getById(id);
        if (course == null) return "课程不存在";
        if (!course.getUid().equals(uid)) return "无权删除别人的课程";
        return this.removeById(id) ? null : "删除失败，请联系管理员";
    }

    @Override
    public PageVO<CourseVO> listAdmin(int page, int size, String keyword, Integer uid) {
        Page<Course> result = this.page(new Page<>(page, size), new LambdaQueryWrapper<Course>()
                .eq(uid != null, Course::getUid, uid)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Course::getName, keyword)
                        .or().like(Course::getTeacher, keyword)
                        .or().like(Course::getLocation, keyword))
                .orderByAsc(Course::getUid)
                .orderByAsc(Course::getWeekday)
                .orderByAsc(Course::getStartSection));
        Map<Integer, Account> users = new HashMap<>();
        if (!result.getRecords().isEmpty()) {
            accountMapper.selectBatchIds(result.getRecords().stream().map(Course::getUid).distinct().toList())
                    .forEach(a -> users.put(a.getId(), a));
        }
        List<CourseVO> items = result.getRecords().stream().map(course -> {
            CourseVO vo = toVO(course);
            Account account = users.get(course.getUid());
            vo.setUsername(account != null ? account.getUsername() : "未知用户");
            return vo;
        }).toList();
        return PageVO.of(items, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    public String adminDelete(int id) {
        return this.removeById(id) ? null : "课程不存在";
    }

    private CourseVO toVO(Course course) {
        return course.asViewObject(CourseVO.class);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean validColor(String color) {
        return color != null && color.matches("^#[0-9a-fA-F]{6}$");
    }
}
