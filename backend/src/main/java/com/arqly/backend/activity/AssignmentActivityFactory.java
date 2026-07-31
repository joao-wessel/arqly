package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class AssignmentActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.ASSIGNED; }
    protected String defaultTitle() { return "Responsável alterado"; }
}
