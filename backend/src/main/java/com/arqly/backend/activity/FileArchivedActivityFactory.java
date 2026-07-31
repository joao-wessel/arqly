package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class FileArchivedActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.FILE_ARCHIVED; }
    protected String defaultTitle() { return "Arquivo arquivado"; }
}
