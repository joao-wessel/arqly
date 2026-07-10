create table project_stage_templates (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    project_template_id uuid not null references project_templates(id),
    name varchar(255) not null,
    description text,
    display_order integer not null,
    color varchar(40),
    icon varchar(80),
    weight_percentage numeric(5,2) not null default 0,
    active boolean not null default true,
    deleted boolean not null default false,
    deleted_at timestamptz
);

create index ix_project_stage_templates_template on project_stage_templates(project_template_id, deleted, display_order);
create index ix_project_stage_templates_tenant on project_stage_templates(tenant_id, deleted);

create table project_stage_template_checklists (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    stage_template_id uuid not null references project_stage_templates(id) on delete cascade,
    title varchar(255) not null,
    display_order integer not null,
    deleted boolean not null default false,
    deleted_at timestamptz
);

create index ix_project_stage_template_checklists_stage on project_stage_template_checklists(stage_template_id, deleted, display_order);

create table project_stages (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id) on delete cascade,
    stage_template_id uuid references project_stage_templates(id),
    depends_on_stage_id uuid references project_stages(id),
    name varchar(255) not null,
    description text,
    display_order integer not null,
    color varchar(40),
    icon varchar(80),
    status varchar(40) not null default 'NOT_STARTED',
    planned_start date,
    planned_end date,
    actual_start date,
    actual_end date,
    completion_percentage numeric(5,2) not null default 0,
    weight_percentage numeric(5,2) not null default 0,
    notes text,
    responsible varchar(255),
    created_by varchar(255),
    updated_by varchar(255),
    deleted boolean not null default false,
    deleted_at timestamptz
);

create index ix_project_stages_project on project_stages(project_id, deleted, display_order);
create index ix_project_stages_tenant_status on project_stages(tenant_id, status, deleted);
create index ix_project_stages_planned_end on project_stages(planned_end);

create table project_stage_checklists (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    stage_id uuid not null references project_stages(id) on delete cascade,
    title varchar(255) not null,
    display_order integer not null,
    completed boolean not null default false,
    completed_at timestamptz,
    deleted boolean not null default false,
    deleted_at timestamptz
);

create index ix_project_stage_checklists_stage on project_stage_checklists(stage_id, deleted, display_order);
