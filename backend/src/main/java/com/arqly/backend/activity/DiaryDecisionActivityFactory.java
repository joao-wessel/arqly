package com.arqly.backend.activity;
import com.arqly.backend.entity.ActivityType; import org.springframework.stereotype.Component;
@Component public class DiaryDecisionActivityFactory extends AbstractActivityFactory { public ActivityType type(){return ActivityType.DIARY_DECISION_REGISTERED;} protected String defaultTitle(){return "Decisão registrada";} }
