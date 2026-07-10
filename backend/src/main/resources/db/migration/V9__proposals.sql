create table proposals (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    client_id uuid not null references clients(id),
    number varchar(40) not null,
    title varchar(255) not null,
    description text,
    valid_until date,
    subtotal numeric(15,2) not null default 0,
    discount numeric(15,2) not null default 0,
    addition numeric(15,2) not null default 0,
    total numeric(15,2) not null default 0,
    status varchar(40) not null,
    scope text,
    exclusions text,
    internal_notes text,
    client_notes text,
    created_by varchar(255),
    updated_by varchar(255),
    sent_at timestamptz,
    viewed_at timestamptz,
    accepted_at timestamptz,
    rejected_at timestamptz,
    expired_at timestamptz,
    cancelled_at timestamptz,
    accepted_ip varchar(80),
    accepted_user_agent varchar(500),
    rejected_ip varchar(80),
    rejected_user_agent varchar(500),
    project_created boolean not null default false,
    deleted boolean not null default false,
    deleted_at timestamptz
);

create unique index ux_proposals_tenant_number_active
    on proposals(tenant_id, number)
    where deleted = false;

create index ix_proposals_tenant_deleted on proposals(tenant_id, deleted);
create index ix_proposals_tenant_client on proposals(tenant_id, client_id);
create index ix_proposals_tenant_status on proposals(tenant_id, status);
create index ix_proposals_total on proposals(total);
create index ix_proposals_created_at on proposals(created_at);

create table proposal_items (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    proposal_id uuid not null references proposals(id) on delete cascade,
    service_id uuid references services(id),
    service_name varchar(255) not null,
    service_description text,
    custom_description text,
    quantity numeric(15,2) not null default 1,
    unit varchar(40) not null,
    unit_value numeric(15,2) not null default 0,
    discount numeric(15,2) not null default 0,
    total numeric(15,2) not null default 0
);

create index ix_proposal_items_proposal on proposal_items(proposal_id);
create index ix_proposal_items_service on proposal_items(service_id);

create table proposal_payment_conditions (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    proposal_id uuid not null references proposals(id) on delete cascade,
    description varchar(255) not null,
    percentage numeric(8,2),
    value numeric(15,2) not null default 0,
    due_date date
);

create index ix_proposal_payment_conditions_proposal on proposal_payment_conditions(proposal_id);

create table projects (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    client_id uuid not null references clients(id),
    proposal_id uuid not null references proposals(id),
    title varchar(255) not null,
    description text,
    commercial_notes text,
    reference_value numeric(15,2) not null default 0,
    status varchar(40) not null,
    created_from_proposal_at timestamptz not null,
    deleted boolean not null default false
);

create unique index ux_projects_proposal_active
    on projects(proposal_id)
    where deleted = false;

create index ix_projects_tenant_deleted on projects(tenant_id, deleted);
create index ix_projects_client on projects(client_id);
