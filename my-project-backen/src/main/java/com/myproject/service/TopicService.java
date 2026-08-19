package com.myproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.entity.dto.TopicType;

import java.util.List;

public interface TopicService extends IService<TopicType> {

    List<TopicType> listTypes();
}