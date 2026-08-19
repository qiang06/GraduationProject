package com.myproject.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.entity.dto.TopicType;
import com.myproject.mapper.TopicTypeMapper;
import com.myproject.service.TopicService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TopicServiceImpl extends ServiceImpl<TopicTypeMapper, TopicType> implements TopicService {

    @Override
    public List<TopicType> listTypes() {
        return this.list();
    }
}