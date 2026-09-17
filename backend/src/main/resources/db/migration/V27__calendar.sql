create table calendar_events (
  id uuid primary key,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  tenant_id uuid not null references tenants(id),
  project_id uuid references projects(id),
  client_id uuid references clients(id),
  stage_id uuid references project_stages(id),
  title varchar(255) not null,
  description text,
  type varchar(40) not null,
  start_date_time timestamptz not null,
  end_date_time timestamptz,
  all_day boolean not null default false,
  location varchar(255),
  responsible_user_id uuid references tenant_users(id),
  created_by_id uuid not null references tenant_users(id),
  visibility varchar(30) not null default 'INTERNAL',
  status varchar(30) not null default 'SCHEDULED',
  deleted boolean not null default false,
  deleted_at timestamptz
);
create index idx_calendar_events_tenant_range on calendar_events(tenant_id, start_date_time, end_date_time);
create index idx_calendar_events_responsible on calendar_events(responsible_user_id);
create index idx_calendar_events_project on calendar_events(project_id);
