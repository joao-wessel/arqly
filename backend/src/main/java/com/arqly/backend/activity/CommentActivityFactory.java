package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class CommentActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.COMMENT; }
    protected String defaultTitle() { return "Comentário"; }
}
