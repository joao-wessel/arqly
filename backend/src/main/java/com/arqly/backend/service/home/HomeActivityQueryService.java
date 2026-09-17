package com.arqly.backend.service.home;

import com.arqly.backend.dto.HomeDtos.HomeActivityResponse;
import com.arqly.backend.repository.ActivityRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeActivityQueryService {
    private final ActivityRepository activities;
    public HomeActivityQueryService(ActivityRepository activities) { this.activities = activities; }
    @Transactional(readOnly = true)
    public List<HomeActivityResponse> recent(HomeUserContext context) {
        return activities.findTop20ByTenantIdAndDeletedFalseOrderByCreatedAtDesc(context.tenantId()).stream()
                .filter(activity -> activity.getProject() == null || HomeAccess.canSee(context, activity.getProject())).limit(10)
                .map(activity -> new HomeActivityResponse(activity.getId(), activity.getContent().getTitle(), activity.getContent().getDescription(),
                        activity.getAuthorName(), activity.getType().name(), activity.getProject() == null ? null : activity.getProject().getId(),
                        activity.getProject() == null ? null : activity.getProject().getName(), activity.getCreatedAt())).toList();
    }
}
