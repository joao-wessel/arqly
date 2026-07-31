package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class DocumentGeneratedActivityFactory extends AbstractActivityFactory {
    public ActivityType type() { return ActivityType.DOCUMENT_GENERATED; }
    protected String defaultTitle() { return "Documento gerado"; }
}
