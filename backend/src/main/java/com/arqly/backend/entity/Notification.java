package com.arqly.backend.entity;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="notifications", uniqueConstraints=@UniqueConstraint(name="uk_notification_dedup",columnNames={"tenant_id","recipient_user_id","deduplication_key"}))
public class Notification extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id",nullable=false) private Tenant tenant;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="recipient_user_id",nullable=false) private TenantUser recipientUser;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private NotificationType type;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private NotificationCategory category;
 @Column(nullable=false) private String title;
 @Column(nullable=false,columnDefinition="text") private String message;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private NotificationPriority priority=NotificationPriority.NORMAL;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private NotificationStatus status=NotificationStatus.UNREAD;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private NotificationSourceType sourceType;
 @Column(nullable=false) private java.util.UUID sourceId;
 private String actionUrl; private String messageKey; @Column(columnDefinition="text") private String metadata;
 @Column(nullable=false) private boolean mandatory=false; @Column(nullable=false) private String deduplicationKey;
 private Instant readAt; private Instant archivedAt;
 @PreUpdate void onUpdate(){touch();}
 public Tenant getTenant(){return tenant;}public void setTenant(Tenant v){tenant=v;} public TenantUser getRecipientUser(){return recipientUser;}public void setRecipientUser(TenantUser v){recipientUser=v;} public NotificationType getType(){return type;}public void setType(NotificationType v){type=v;} public NotificationCategory getCategory(){return category;}public void setCategory(NotificationCategory v){category=v;} public String getTitle(){return title;}public void setTitle(String v){title=v;} public String getMessage(){return message;}public void setMessage(String v){message=v;} public NotificationPriority getPriority(){return priority;}public void setPriority(NotificationPriority v){priority=v;} public NotificationStatus getStatus(){return status;}public void setStatus(NotificationStatus v){status=v;} public NotificationSourceType getSourceType(){return sourceType;}public void setSourceType(NotificationSourceType v){sourceType=v;} public java.util.UUID getSourceId(){return sourceId;}public void setSourceId(java.util.UUID v){sourceId=v;} public String getActionUrl(){return actionUrl;}public void setActionUrl(String v){actionUrl=v;} public String getMessageKey(){return messageKey;}public void setMessageKey(String v){messageKey=v;} public String getMetadata(){return metadata;}public void setMetadata(String v){metadata=v;} public boolean isMandatory(){return mandatory;}public void setMandatory(boolean v){mandatory=v;} public String getDeduplicationKey(){return deduplicationKey;}public void setDeduplicationKey(String v){deduplicationKey=v;} public Instant getReadAt(){return readAt;}public void setReadAt(Instant v){readAt=v;} public Instant getArchivedAt(){return archivedAt;}public void setArchivedAt(Instant v){archivedAt=v;}
}
