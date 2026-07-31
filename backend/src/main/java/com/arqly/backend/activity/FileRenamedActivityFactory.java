package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class FileRenamedActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.FILE_RENAMED; }
    protected String defaultTitle() { return "Arquivo renomeado"; }
}
