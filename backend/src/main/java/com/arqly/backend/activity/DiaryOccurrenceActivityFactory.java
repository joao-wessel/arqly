package com.arqly.backend.activity;
import com.arqly.backend.entity.ActivityType; import org.springframework.stereotype.Component;
@Component public class DiaryOccurrenceActivityFactory extends AbstractActivityFactory { public ActivityType type(){return ActivityType.DIARY_OCCURRENCE_CREATED;} protected String defaultTitle(){return "Ocorrência registrada";} }
