package com.myproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.entity.dto.LibraryResource;
import com.myproject.entity.vo.request.LibrarySaveVO;
import com.myproject.entity.vo.response.LibraryResourceVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.mapper.LibraryResourceMapper;
import com.myproject.service.LibraryService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class LibraryServiceImpl extends ServiceImpl<LibraryResourceMapper, LibraryResource> implements LibraryService {

    @Override
    public PageVO<LibraryResourceVO> list(int page, int size, String category, String keyword) {
        return pageResult(page, size, category, keyword);
    }

    @Override
    public LibraryResourceVO getDetail(int id) {
        LibraryResource resource = this.getById(id);
        return resource == null ? null : resource.asViewObject(LibraryResourceVO.class);
    }

    @Override
    public PageVO<LibraryResourceVO> listAdmin(int page, int size, String category, String keyword) {
        return pageResult(page, size, category, keyword);
    }

    @Override
    public String saveResource(LibrarySaveVO vo) {
        String url = trim(vo.getUrl());
        if (!url.isEmpty() && !safeUrl(url)) {
            return "阅读链接必须以 http:// 或 https:// 开头";
        }
        LibraryResource resource;
        if (vo.getId() != null) {
            resource = this.getById(vo.getId());
            if (resource == null) return "资源不存在";
        } else {
            resource = new LibraryResource();
        }
        resource.setTitle(vo.getTitle().trim());
        resource.setAuthor(trim(vo.getAuthor()));
        resource.setCategory(vo.getCategory().trim());
        resource.setCover(trim(vo.getCover()));
        resource.setDescription(trim(vo.getDescription()));
        resource.setUrl(url);
        boolean ok = vo.getId() == null ? this.save(resource) : this.updateById(resource);
        return ok ? null : "保存失败，请联系管理员";
    }

    @Override
    public String deleteResource(int id) {
        return this.removeById(id) ? null : "资源不存在";
    }

    private PageVO<LibraryResourceVO> pageResult(int page, int size, String category, String keyword) {
        Page<LibraryResource> result = this.page(new Page<>(page, size), new LambdaQueryWrapper<LibraryResource>()
                .eq(StringUtils.hasText(category), LibraryResource::getCategory, category)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(LibraryResource::getTitle, keyword)
                        .or().like(LibraryResource::getAuthor, keyword))
                .orderByDesc(LibraryResource::getUpdateTime)
                .orderByDesc(LibraryResource::getId));
        List<LibraryResourceVO> items = result.getRecords().stream()
                .map(resource -> resource.asViewObject(LibraryResourceVO.class))
                .toList();
        return PageVO.of(items, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean safeUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
}
