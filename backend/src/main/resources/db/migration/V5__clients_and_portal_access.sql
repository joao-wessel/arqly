create table clients (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    person_type varchar(40) not null,
    name varchar(255),
    cpf varchar(20),
    rg varchar(40),
    birth_date date,
    legal_name varchar(255),
    trade_name varchar(255),
    cnpj varchar(20),
    state_registration varchar(60),
    email varchar(255) not null,
    phone varchar(40) not null,
    whatsapp varchar(40),
    zip_code varchar(20) not null,
    street varchar(255) not null,
    number varchar(40) not null,
    complement varchar(255),
    district varchar(255) not null,
    city varchar(255) not null,
    state varchar(2) not null,
    notes text,
    status varchar(40) not null,
    deleted boolean not null default false,
    deleted_at timestamptz
);

create index ix_clients_tenant_deleted on clients(tenant_id, deleted);
create index ix_clients_tenant_status on clients(tenant_id, status);
create index ix_clients_tenant_city on clients(tenant_id, city);
create index ix_clients_tenant_person_type on clients(tenant_id, person_type);

create table client_portal_accesses (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    client_id uuid not null references clients(id) on delete cascade,
    token uuid not null unique,
    expires_at timestamptz not null,
    revoked boolean not null default false,
    last_access_at timestamptz,
    active boolean not null default true
);

create index ix_client_portal_client on client_portal_accesses(client_id);
create index ix_client_portal_token on client_portal_accesses(token);
