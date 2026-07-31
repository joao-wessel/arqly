create table document_templates (
    id uuid primary key,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    tenant_id uuid not null references tenants(id),
    name varchar(255) not null,
    description text,
    category varchar(40) not null,
    content text not null,
    active boolean not null default true,
    version integer not null default 1,
    current_version boolean not null default true,
    series_id uuid not null,
    previous_version_id uuid references document_templates(id),
    archived boolean not null default false,
    deleted boolean not null default false,
    deleted_at timestamptz,
    constraint uk_document_template_series_version unique (tenant_id, series_id, version)
);

create index ix_document_templates_tenant_category on document_templates(tenant_id, category, active) where deleted = false;
create index ix_document_templates_tenant_name on document_templates(tenant_id, lower(name)) where deleted = false;

create table generated_documents (
    id uuid primary key,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    tenant_id uuid not null references tenants(id),
    template_id uuid not null references document_templates(id),
    project_id uuid references projects(id),
    proposal_id uuid references proposals(id),
    client_id uuid not null references clients(id),
    title varchar(255) not null,
    content text not null,
    version integer not null default 1,
    current_version boolean not null default true,
    series_id uuid not null,
    previous_version_id uuid references generated_documents(id),
    status varchar(30) not null default 'GENERATED',
    generated_by_id uuid references tenant_users(id),
    generated_by_name varchar(255) not null,
    generated_at timestamptz not null default now(),
    deleted boolean not null default false,
    deleted_at timestamptz,
    constraint uk_generated_document_series_version unique (tenant_id, series_id, version)
);

create index ix_generated_documents_tenant_date on generated_documents(tenant_id, generated_at desc) where deleted = false;
create index ix_generated_documents_project on generated_documents(tenant_id, project_id, generated_at desc) where deleted = false;
create index ix_generated_documents_proposal on generated_documents(tenant_id, proposal_id, generated_at desc) where deleted = false;
create index ix_generated_documents_client on generated_documents(tenant_id, client_id, generated_at desc) where deleted = false;

alter table activities alter column project_id drop not null;
alter table activities add column client_id uuid references clients(id);
alter table activities add column proposal_id uuid references proposals(id);
alter table activities add column generated_document_id uuid references generated_documents(id);
create index ix_activities_tenant_client_created on activities(tenant_id, client_id, created_at desc) where deleted = false;
create index ix_activities_tenant_proposal_created on activities(tenant_id, proposal_id, created_at desc) where deleted = false;
