package com.arqly.backend.notification;
import com.arqly.backend.entity.*;import java.util.*;
public record NotificationRequest(UUID tenantId,UUID recipientUserId,NotificationType type,NotificationCategory category,NotificationPriority priority,String title,String message,NotificationSourceType sourceType,UUID sourceId,String actionUrl,String deduplicationKey,boolean mandatory,String messageKey,String metadata){}
