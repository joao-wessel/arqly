package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class StatusChangedActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.STATUS_CHANGED; }
    protected String defaultTitle() { return "Status alterado"; }
}
