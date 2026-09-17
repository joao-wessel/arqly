create table construction_diary_entries (
    id uuid primary key,
    created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
    tenant_id uuid not null references tenants(id), project_id uuid not null references projects(id),
    created_by_id uuid references tenant_users(id), responsible_user_id uuid references tenant_users(id),
    entry_type varchar(30) not null, status varchar(30) not null default 'DRAFT', title varchar(255) not null,
    entry_date date not null, start_time time, end_time time, location varchar(255), summary text,
    weather_condition varchar(120), temperature numeric(5,2), visibility varchar(30) not null default 'INTERNAL',
    next_visit_date date, next_visit_notes text, published_at timestamptz, published_by_id uuid references tenant_users(id),
    revision_number integer not null default 1, last_revision_at timestamptz, last_revision_by_id uuid references tenant_users(id),
    deleted boolean not null default false, deleted_at timestamptz
);
create index ix_diary_entry_tenant_project_date on construction_diary_entries(tenant_id, project_id, entry_date desc) where deleted = false;
create index ix_diary_entry_tenant_status on construction_diary_entries(tenant_id, status, entry_date desc) where deleted = false;
create index ix_diary_entry_next_visit on construction_diary_entries(tenant_id, next_visit_date) where deleted = false;

create table construction_diary_entry_stages (
    diary_entry_id uuid not null references construction_diary_entries(id), stage_id uuid not null references project_stages(id),
    primary key (diary_entry_id, stage_id)
);
create index ix_diary_entry_stages_stage on construction_diary_entry_stages(stage_id);

create table construction_diary_participants (
    id uuid primary key, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
    diary_entry_id uuid not null references construction_diary_entries(id) on delete cascade, participant_type varchar(30) not null,
    tenant_user_id uuid references tenant_users(id), name varchar(255), company varchar(255), role varchar(255), phone varchar(60), email varchar(255)
);
create table construction_diary_observations (
    id uuid primary key, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
    diary_entry_id uuid not null references construction_diary_entries(id) on delete cascade, title varchar(255) not null, description text,
    category varchar(30) not null, status varchar(30) not null, display_order integer not null, visibility varchar(30) not null default 'INTERNAL'
);
create table construction_diary_occurrences (
    id uuid primary key, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
    diary_entry_id uuid not null references construction_diary_entries(id) on delete cascade, title varchar(255) not null, description text,
    severity varchar(30) not null, responsible_user_id uuid references tenant_users(id), due_date date, resolved boolean not null default false,
    resolved_at timestamptz, resolution text, visibility varchar(30) not null default 'INTERNAL'
);
create index ix_diary_occurrence_open on construction_diary_occurrences(diary_entry_id, resolved, severity);
create table construction_diary_decisions (
    id uuid primary key, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
    diary_entry_id uuid not null references construction_diary_entries(id) on delete cascade, description text not null,
    decided_by varchar(255), decision_date date, visibility varchar(30) not null default 'INTERNAL'
);
create table construction_diary_instructions (
    id uuid primary key, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
    diary_entry_id uuid not null references construction_diary_entries(id) on delete cascade, description text not null,
    responsible varchar(255), deadline date, completed boolean not null default false, visibility varchar(30) not null default 'INTERNAL'
);
create table construction_diary_photos (
    id uuid primary key, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
    diary_entry_id uuid not null references construction_diary_entries(id) on delete cascade, file_resource_id uuid not null references file_resources(id),
    caption varchar(255), description text, display_order integer not null, visibility varchar(30) not null default 'INTERNAL',
    constraint uk_diary_photo_file unique(diary_entry_id, file_resource_id)
);
