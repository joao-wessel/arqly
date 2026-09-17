package com.arqly.backend.activity;
import com.arqly.backend.entity.ActivityType; import org.springframework.stereotype.Component;
@Component public class DiaryCreatedActivityFactory extends AbstractActivityFactory { public ActivityType type(){return ActivityType.DIARY_CREATED;} protected String defaultTitle(){return "Diário de Obra iniciado";} }
