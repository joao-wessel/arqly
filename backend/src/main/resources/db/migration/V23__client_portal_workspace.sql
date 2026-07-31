alter table generated_documents
    add column if not exists client_visible boolean not null default false;

create table if not exists approvals (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    stage_id uuid references project_stages(id),
    document_id uuid references generated_documents(id),
    client_id uuid not null references clients(id),
    status varchar(30) not null,
    description text not null,
    deadline date,
    approved_at timestamptz,
    approved_by varchar(255),
    created_by_id uuid references tenant_users(id),
    client_comment text,
    deleted boolean not null default false,
    deleted_at timestamptz
);

create index if not exists ix_approvals_tenant_project on approvals(tenant_id, project_id);
create index if not exists ix_approvals_tenant_client on approvals(tenant_id, client_id);
create index if not exists ix_approvals_tenant_status on approvals(tenant_id, status);
