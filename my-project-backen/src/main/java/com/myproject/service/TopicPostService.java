package com.myproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.entity.dto.Topic;
import com.myproject.entity.vo.request.CreateTopicVO;
import com.myproject.entity.vo.request.UpdateTopicVO;
import com.myproject.entity.vo.response.TopicDetailVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.entity.vo.response.TopicPreviewVO;

public interface TopicPostService extends IService<Topic> {

    /**
     * 创建新帖子
     * @param uid 发帖人ID
     * @param vo 帖子内容
     * @return null表示成功，否则为错误原因
     */
    String createTopic(int uid, CreateTopicVO vo);

    /**
     * 分页查询帖子列表
     * @param page 页码
     * @param size 每页数量
     * @param type 分类ID（可选，为null则查全部）
     * @return 帖子预览列表
     */
    PageVO<TopicPreviewVO> listTopics(int page, int size, Integer type, String keyword);

    /**
     * 查询帖子详情
     * @param id 帖子ID
     * @return 帖子详情，不存在则返回null
     */
    TopicDetailVO getTopicDetail(int id);

    /**
     * 编辑帖子（仅帖主本人）
     * @param uid 操作用户ID
     * @param vo 编辑内容
     * @return null表示成功，否则为错误原因
     */
    String updateTopic(int uid, UpdateTopicVO vo);

    /**
     * 删除帖子（仅帖主本人）
     * @param uid 操作用户ID
     * @param id 帖子ID
     * @return null表示成功，否则为错误原因
     */
    String deleteTopic(int uid, int id);
}
