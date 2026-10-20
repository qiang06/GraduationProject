package com.myproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.entity.dto.LibraryResource;
import com.myproject.entity.vo.request.LibrarySaveVO;
import com.myproject.entity.vo.response.LibraryResourceVO;
import com.myproject.entity.vo.response.PageVO;

public interface LibraryService extends IService<LibraryResource> {
    PageVO<LibraryResourceVO> list(int page, int size, String category, String keyword);

    LibraryResourceVO getDetail(int id);

    PageVO<LibraryResourceVO> listAdmin(int page, int size, String category, String keyword);

    String saveResource(LibrarySaveVO vo);

    String deleteResource(int id);
}
