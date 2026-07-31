package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class FileDownloadedActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.FILE_DOWNLOADED; }
    protected String defaultTitle() { return "Arquivo baixado"; }
}
