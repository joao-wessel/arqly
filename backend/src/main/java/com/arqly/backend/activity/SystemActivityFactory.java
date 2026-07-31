package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class SystemActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.SYSTEM; }
    protected String defaultTitle() { return "Atividade"; }
}
