package com.arqly.backend.activity;

import com.arqly.backend.entity.Activity;
import com.arqly.backend.entity.ActivityContent;
import com.arqly.backend.entity.ActivityVisibility;

public abstract class AbstractActivityFactory implements ActivityFactory {
    @Override
    public Activity build(ActivityBuildRequest request) {
        var activity = new Activity();
        activity.setTenant(request.tenant());
        activity.setProject(request.project());
        activity.setClient(request.client());
        activity.setProposal(request.proposal());
        activity.setGeneratedDocument(request.generatedDocument());
        activity.setPhase(request.phase());
        activity.setStage(request.stage());
        activity.setAuthor(request.author());
        activity.setAuthorName(request.author() != null
                ? request.author().getName()
                : request.authorName() == null || request.authorName().isBlank() ? "Sistema" : request.authorName());
        activity.setType(type());
        activity.setVisibility(request.visibility() == null ? ActivityVisibility.INTERNAL : request.visibility());

        var content = new ActivityContent();
        content.setTitle(request.title() == null || request.title().isBlank() ? defaultTitle() : request.title());
        content.setDescription(request.description() == null ? "" : request.description());
        content.setMetadata(request.metadata());
        activity.setContent(content);
        return activity;
    }

    protected abstract String defaultTitle();
}
