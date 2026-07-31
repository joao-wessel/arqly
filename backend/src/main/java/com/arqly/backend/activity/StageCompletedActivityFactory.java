package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class StageCompletedActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.STAGE_COMPLETED; }
    protected String defaultTitle() { return "Etapa concluída"; }
}
