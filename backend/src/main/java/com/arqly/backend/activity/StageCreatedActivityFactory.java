package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class StageCreatedActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.STAGE_CREATED; }
    protected String defaultTitle() { return "Etapa criada"; }
}
