create table notifications (
 id uuid primary key, created_at timestamptz not null, updated_at timestamptz not null,
 tenant_id uuid not null references tenants(id), recipient_user_id uuid not null references tenant_users(id),
 type varchar(60) not null, category varchar(40) not null, title varchar(255) not null, message text not null,
 priority varchar(20) not null, status varchar(20) not null, source_type varchar(60) not null, source_id uuid not null,
 action_url varchar(500), message_key varchar(160), metadata text, mandatory boolean not null default false,
 deduplication_key varchar(300) not null, read_at timestamptz, archived_at timestamptz,
 constraint uk_notification_dedup unique(tenant_id,recipient_user_id,deduplication_key)
);
create index idx_notifications_recipient_status_created on notifications(tenant_id,recipient_user_id,status,created_at desc);
create index idx_notifications_recipient_category on notifications(recipient_user_id,category);
create table notification_preferences (
 id uuid primary key, created_at timestamptz not null, updated_at timestamptz not null,
 tenant_user_id uuid not null references tenant_users(id), category varchar(40) not null,
 in_app_enabled boolean not null default true, email_enabled boolean not null default false,
 constraint uk_notification_preference unique(tenant_user_id,category)
);
