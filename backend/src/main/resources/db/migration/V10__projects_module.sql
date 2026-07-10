create table project_templates (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    name varchar(255) not null,
    description text,
    active boolean not null default true,
    deleted boolean not null default false,
    deleted_at timestamptz
);

create unique index ux_project_templates_tenant_name_active
    on project_templates(tenant_id, lower(name))
    where deleted = false;

create index ix_project_templates_tenant_deleted on project_templates(tenant_id, deleted);

alter table projects add column if not exists code varchar(40);
alter table projects add column if not exists name varchar(255);
alter table projects add column if not exists project_template_id uuid references project_templates(id);
alter table projects add column if not exists responsible_architect varchar(255);
alter table projects add column if not exists contracted_value numeric(15,2);
alter table projects add column if not exists start_date date;
alter table projects add column if not exists expected_end_date date;
alter table projects add column if not exists completed_at date;
alter table projects add column if not exists internal_notes text;
alter table projects add column if not exists created_by varchar(255);
alter table projects add column if not exists updated_by varchar(255);
alter table projects add column if not exists deleted_at timestamptz;

update projects
set name = coalesce(name, title),
    code = coalesce(code, 'PRJ-' || extract(year from created_at)::int || '-' || lpad(row_number_text.seq::text, 6, '0')),
    contracted_value = coalesce(contracted_value, reference_value),
    status = case when status = 'OPEN' then 'PLANNING' else status end
from (
    select id, row_number() over (partition by tenant_id, extract(year from created_at) order by created_at, id) as seq
    from projects
) row_number_text
where projects.id = row_number_text.id;

alter table projects alter column code set not null;
alter table projects alter column name set not null;
alter table projects alter column contracted_value set not null;
alter table projects alter column status set not null;

create unique index ux_projects_tenant_code_active
    on projects(tenant_id, code)
    where deleted = false;

create index ix_projects_tenant_status on projects(tenant_id, status);
create index ix_projects_template on projects(project_template_id);
create index ix_projects_expected_end on projects(expected_end_date);

create table project_services (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    project_id uuid not null references projects(id) on delete cascade,
    name varchar(255) not null,
    description text,
    quantity numeric(15,2) not null default 1,
    unit varchar(40) not null,
    contracted_value numeric(15,2) not null default 0
);

create index ix_project_services_project on project_services(project_id);
