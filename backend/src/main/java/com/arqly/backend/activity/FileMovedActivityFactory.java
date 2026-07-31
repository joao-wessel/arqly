package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class FileMovedActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.FILE_MOVED; }
    protected String defaultTitle() { return "Arquivo movido"; }
}
