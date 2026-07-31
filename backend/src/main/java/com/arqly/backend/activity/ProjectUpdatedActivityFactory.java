package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class ProjectUpdatedActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.PROJECT_UPDATED; }
    protected String defaultTitle() { return "Projeto atualizado"; }
}
