package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class ProjectCreatedActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.PROJECT_CREATED; }
    protected String defaultTitle() { return "Projeto criado"; }
}
