package com.myproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.entity.dto.Comment;
import com.myproject.entity.vo.request.CreateCommentVO;
import com.myproject.entity.vo.response.CommentVO;
import com.myproject.entity.vo.response.PageVO;
import java.util.Map;

public interface CommentService extends IService<Comment> {

    Map<String, Object> createComment(int uid, CreateCommentVO vo);

    PageVO<CommentVO> listComments(int tid, int page, int size);

    String deleteComment(int uid, int id);
}
