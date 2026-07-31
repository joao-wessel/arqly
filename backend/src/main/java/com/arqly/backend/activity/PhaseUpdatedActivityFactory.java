package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class PhaseUpdatedActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.PHASE_UPDATED; }
    protected String defaultTitle() { return "Fase atualizada"; }
}
