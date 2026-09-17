package com.arqly.backend.activity;
import com.arqly.backend.entity.ActivityType; import org.springframework.stereotype.Component;
@Component public class DiaryPublishedActivityFactory extends AbstractActivityFactory { public ActivityType type(){return ActivityType.DIARY_PUBLISHED;} protected String defaultTitle(){return "Registro publicado no Diário de Obra";} }
