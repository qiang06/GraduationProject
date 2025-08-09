package com.myproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproject.entity.dto.StoreImage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImageServiceMapper extends BaseMapper<StoreImage> {
}
