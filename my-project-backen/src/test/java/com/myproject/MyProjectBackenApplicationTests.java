package com.myproject;

import com.myproject.entity.vo.request.CreateCommentVO;
import com.myproject.entity.vo.request.CreateTopicVO;
import com.myproject.entity.vo.request.PrivacySaveVO;
import com.myproject.entity.vo.response.PageVO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MyProjectBackenApplicationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void pageResponseCalculatesHasNext() {
        PageVO<String> page = PageVO.of(List.of("topic"), 21, 2, 10, 3);
        assertEquals(21, page.total());
        assertTrue(page.hasNext());
        assertFalse(PageVO.of(List.of(), 21, 3, 10, 3).hasNext());
    }

    @Test
    void blankTopicAndCommentAreRejected() {
        CreateTopicVO topic = new CreateTopicVO();
        topic.setTitle("   ");
        topic.setType(1);
        topic.setContent("   ");
        assertFalse(validator.validate(topic).isEmpty());

        CreateCommentVO comment = new CreateCommentVO();
        comment.setTid(1);
        comment.setContent("   ");
        assertFalse(validator.validate(comment).isEmpty());
    }

    @Test
    void emailPrivacyKeyIsAcceptedWithLegacyAlias() {
        PrivacySaveVO current = new PrivacySaveVO();
        current.setType("email");
        assertTrue(validator.validate(current).isEmpty());

        PrivacySaveVO legacy = new PrivacySaveVO();
        legacy.setType("emial");
        assertTrue(validator.validate(legacy).isEmpty());
    }
}
