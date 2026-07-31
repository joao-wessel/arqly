package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class VisitRegisteredActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.VISIT_REGISTERED; }
    protected String defaultTitle() { return "Visita registrada"; }
}
