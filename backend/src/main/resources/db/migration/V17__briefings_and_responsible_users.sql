create table briefings (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    client_id uuid not null references clients(id),
    project_template_id uuid references project_templates(id),
    responsible_user_id uuid references tenant_users(id),
    title varchar(255) not null,
    description text,
    status varchar(40) not null default 'DRAFT',
    approximate_area numeric(12,2),
    work_address varchar(255),
    city varchar(120),
    state varchar(2),
    desired_deadline date,
    expected_budget numeric(15,2),
    architectural_style varchar(255),
    color_palette varchar(255),
    desired_materials text,
    preference_notes text,
    legal_restrictions text,
    technical_restrictions text,
    client_restrictions text,
    restriction_notes text,
    created_by varchar(255),
    updated_by varchar(255),
    deleted boolean not null default false,
    deleted_at timestamptz
);

create table briefing_requirements (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    briefing_id uuid not null references briefings(id) on delete cascade,
    description varchar(255) not null,
    display_order integer not null,
    deleted boolean not null default false,
    deleted_at timestamptz
);

create table briefing_attachments (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    briefing_id uuid not null references briefings(id) on delete cascade,
    file_name varchar(255) not null,
    storage_key varchar(500),
    content_type varchar(120),
    file_size bigint,
    deleted boolean not null default false,
    deleted_at timestamptz
);

alter table proposals add column if not exists briefing_id uuid references briefings(id);
create unique index if not exists ux_proposals_briefing_active on proposals(briefing_id) where briefing_id is not null and deleted = false;

alter table projects add column if not exists responsible_user_id uuid references tenant_users(id);
alter table projects add column if not exists project_manager_id uuid references tenant_users(id);
alter table projects add column if not exists legacy_responsible_name varchar(255);

update projects
set legacy_responsible_name = responsible_architect
where legacy_responsible_name is null
  and responsible_architect is not null;

update projects p
set responsible_user_id = tu.id
from tenant_users tu
where p.responsible_user_id is null
  and tu.tenant_id = p.tenant_id
  and p.responsible_architect is not null
  and (lower(tu.email) = lower(p.responsible_architect) or lower(tu.name) = lower(p.responsible_architect));

alter table project_stages add column if not exists responsible_user_id uuid references tenant_users(id);
alter table project_stages add column if not exists legacy_responsible_name varchar(255);

update project_stages
set legacy_responsible_name = responsible
where legacy_responsible_name is null
  and responsible is not null;

update project_stages ps
set responsible_user_id = tu.id
from tenant_users tu
where ps.responsible_user_id is null
  and tu.tenant_id = ps.tenant_id
  and ps.responsible is not null
  and (lower(tu.email) = lower(ps.responsible) or lower(tu.name) = lower(ps.responsible));

create index ix_briefings_tenant_status on briefings(tenant_id, status, deleted);
create index ix_briefings_client on briefings(client_id, deleted, created_at desc);
create index ix_briefings_responsible on briefings(responsible_user_id, deleted);
create index ix_briefing_requirements_briefing on briefing_requirements(briefing_id, deleted, display_order);
create index ix_projects_responsible_user on projects(responsible_user_id, deleted);
create index ix_project_stages_responsible_user on project_stages(responsible_user_id, deleted);
