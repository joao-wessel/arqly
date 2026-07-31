package com.arqly.backend.activity;

import com.arqly.backend.entity.Activity;
import com.arqly.backend.entity.ActivityType;

public interface ActivityFactory {
    ActivityType type();
    Activity build(ActivityBuildRequest request);
}
