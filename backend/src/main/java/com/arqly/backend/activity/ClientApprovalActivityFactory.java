package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class ClientApprovalActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.CLIENT_APPROVAL; }
    protected String defaultTitle() { return "Aprovação do cliente"; }
}
