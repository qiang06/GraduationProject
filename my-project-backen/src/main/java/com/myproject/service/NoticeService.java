package com.myproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.entity.dto.Notice;
import com.myproject.entity.vo.request.NoticeSaveVO;
import com.myproject.entity.vo.response.NoticePreviewVO;
import com.myproject.entity.vo.response.NoticeVO;
import com.myproject.entity.vo.response.PageVO;

public interface NoticeService extends IService<Notice> {
    PageVO<NoticePreviewVO> listPublished(int page, int size, String category, String keyword);

    NoticeVO getDetail(int id);

    PageVO<NoticeVO> listAdmin(int page, int size, String category, String keyword);

    String saveNotice(NoticeSaveVO vo);

    String deleteNotice(int id);

    String setTop(int id, boolean top);
}
