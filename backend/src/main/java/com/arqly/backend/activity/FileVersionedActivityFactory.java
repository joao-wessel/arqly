package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class FileVersionedActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.FILE_VERSIONED; }
    protected String defaultTitle() { return "Nova versão de arquivo"; }
}
