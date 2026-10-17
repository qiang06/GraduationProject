package com.myproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.entity.dto.Topic;
import com.myproject.entity.vo.request.CreateTopicVO;
import com.myproject.entity.vo.request.TopicTypeCreateVO;
import com.myproject.entity.vo.request.TopicTypeUpdateVO;
import com.myproject.entity.vo.request.UpdateTopicVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.entity.vo.response.TopicDetailVO;
import com.myproject.entity.vo.response.TopicPreviewVO;
import com.myproject.entity.vo.response.TopicTopVO;

import java.util.List;

public interface TopicPostService extends IService<Topic> {

    String createTopic(int uid, CreateTopicVO vo);

    /**
     * 分页查询帖子列表
     * @param type     分类 ID，可为 null
     * @param typeCode 分类 code（lost/activity/...），type 为空时用它解析
     */
    PageVO<TopicPreviewVO> listTopics(int page, int size, Integer type, String typeCode, String keyword);

    /** 查询详情；uid 用于判断匿名帖子的作者本人，admin 用于允许管理员查看被屏蔽帖子 */
    TopicDetailVO getTopicDetail(int id, int uid, boolean admin);

    String updateTopic(int uid, UpdateTopicVO vo);

    String deleteTopic(int uid, int id);

    /** 失物招领：作者标记已找回 / 撤销 */
    String updateStatus(int uid, int id, String status);

    /** 我的收藏：按收藏时间倒序返回帖子预览 */
    List<TopicPreviewVO> listCollects(int uid);

    /** 置顶帖子，最多 10 条 */
    List<TopicTopVO> listTopTopics();

    // ==================== 管理端 ====================

    /** 管理端列表：包含被屏蔽帖子，作者信息不匿名 */
    PageVO<TopicPreviewVO> listAllTopics(int page, int size, String keyword);

    void adminDeleteTopic(int id);

    void setTopicTop(int id, boolean top);

    void setTopicLocked(int id, boolean locked);

    void setTopicInvisible(int id, boolean invisible);

    String changeTopicType(int id, int type);

    String createTopicType(TopicTypeCreateVO vo);

    String updateTopicType(TopicTypeUpdateVO vo);

    String deleteTopicType(int id);
}
