package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class FileUploadedActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.FILE_UPLOADED; }
    protected String defaultTitle() { return "Arquivo enviado"; }
}
