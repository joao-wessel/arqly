create table service_categories (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    name varchar(255) not null,
    description text,
    color varchar(40),
    icon varchar(80),
    display_order integer not null default 0,
    active boolean not null default true,
    deleted boolean not null default false,
    deleted_at timestamptz
);

create unique index ux_service_categories_tenant_name_active
    on service_categories(tenant_id, lower(name))
    where deleted = false;

create index ix_service_categories_tenant_deleted on service_categories(tenant_id, deleted);
create index ix_service_categories_tenant_order on service_categories(tenant_id, display_order);

create table services (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    category_id uuid references service_categories(id),
    name varchar(255) not null,
    short_description varchar(500),
    full_description text,
    base_value numeric(14,2),
    currency varchar(8) not null default 'BRL',
    billing_unit varchar(40) not null,
    active boolean not null default true,
    featured boolean not null default false,
    display_order integer not null default 0,
    deleted boolean not null default false,
    deleted_at timestamptz
);

create unique index ux_services_tenant_name_active
    on services(tenant_id, lower(name))
    where deleted = false;

create index ix_services_tenant_deleted on services(tenant_id, deleted);
create index ix_services_tenant_active on services(tenant_id, active);
create index ix_services_tenant_featured on services(tenant_id, featured);
create index ix_services_tenant_category on services(tenant_id, category_id);
create index ix_services_base_value on services(base_value);
