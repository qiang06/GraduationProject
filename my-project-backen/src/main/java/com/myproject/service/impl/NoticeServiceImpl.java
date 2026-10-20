package com.myproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.entity.dto.Notice;
import com.myproject.entity.vo.request.NoticeSaveVO;
import com.myproject.entity.vo.response.NoticePreviewVO;
import com.myproject.entity.vo.response.NoticeVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.mapper.NoticeMapper;
import com.myproject.service.NoticeService;
import com.myproject.utils.ContentPreview;
import com.myproject.utils.ContentSanitizer;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Resource
    ContentSanitizer contentSanitizer;

    @Resource
    ContentPreview contentPreview;

    @Override
    public PageVO<NoticePreviewVO> listPublished(int page, int size, String category, String keyword) {
        Page<Notice> result = this.page(new Page<>(page, size), wrapper(category, keyword));
        List<NoticePreviewVO> items = result.getRecords().stream().map(notice -> {
            NoticePreviewVO vo = notice.asViewObject(NoticePreviewVO.class);
            vo.setSummary(contentPreview.preview(notice.getContent(), 100));
            return vo;
        }).toList();
        return PageVO.of(items, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    public NoticeVO getDetail(int id) {
        Notice notice = this.getById(id);
        if (notice == null) return null;
        NoticeVO vo = notice.asViewObject(NoticeVO.class);
        vo.setContent(contentSanitizer.sanitize(notice.getContent()));
        return vo;
    }

    @Override
    public PageVO<NoticeVO> listAdmin(int page, int size, String category, String keyword) {
        Page<Notice> result = this.page(new Page<>(page, size), wrapper(category, keyword));
        List<NoticeVO> items = result.getRecords().stream().map(notice -> {
            NoticeVO vo = notice.asViewObject(NoticeVO.class);
            vo.setContent(contentSanitizer.sanitize(notice.getContent()));
            return vo;
        }).toList();
        return PageVO.of(items, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    public String saveNotice(NoticeSaveVO vo) {
        String content = contentSanitizer.sanitize(vo.getContent());
        if (content == null || content.isBlank()) {
            return "通知内容不能为空";
        }
        Notice notice;
        if (vo.getId() != null) {
            notice = this.getById(vo.getId());
            if (notice == null) return "通知不存在";
        } else {
            notice = new Notice();
            notice.setPublishTime(new Date());
        }
        notice.setTitle(vo.getTitle().trim());
        notice.setCategory(vo.getCategory().trim());
        notice.setContent(content);
        notice.setTop(Boolean.TRUE.equals(vo.getTop()) ? 1 : 0);
        notice.setPublisher(StringUtils.hasText(vo.getPublisher()) ? vo.getPublisher().trim() : "教务处");
        boolean ok = vo.getId() == null ? this.save(notice) : this.updateById(notice);
        return ok ? null : "保存失败，请联系管理员";
    }

    @Override
    public String deleteNotice(int id) {
        return this.removeById(id) ? null : "通知不存在";
    }

    @Override
    public String setTop(int id, boolean top) {
        boolean ok = this.update(new LambdaUpdateWrapper<Notice>()
                .eq(Notice::getId, id)
                .set(Notice::getTop, top ? 1 : 0));
        return ok ? null : "通知不存在";
    }

    private LambdaQueryWrapper<Notice> wrapper(String category, String keyword) {
        return new LambdaQueryWrapper<Notice>()
                .eq(StringUtils.hasText(category), Notice::getCategory, category)
                .like(StringUtils.hasText(keyword), Notice::getTitle, keyword)
                .orderByDesc(Notice::getTop)
                .orderByDesc(Notice::getPublishTime)
                .orderByDesc(Notice::getId);
    }
}
