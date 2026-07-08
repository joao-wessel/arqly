create table tenants (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    trade_name varchar(255) not null,
    legal_name varchar(255) not null,
    cnpj varchar(20) not null unique,
    primary_email varchar(255) not null,
    phone varchar(255),
    address varchar(255),
    city varchar(255),
    state varchar(255),
    zip_code varchar(255),
    status varchar(40) not null
);

create table platform_users (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    name varchar(255) not null,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    active boolean not null,
    last_access_at timestamptz
);

create table platform_user_roles (
    platform_user_id uuid not null references platform_users(id) on delete cascade,
    role varchar(60) not null,
    primary key (platform_user_id, role)
);

create table tenant_users (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    name varchar(255) not null,
    email varchar(255) not null,
    password_hash varchar(255) not null,
    active boolean not null,
    last_access_at timestamptz
);

create unique index ux_tenant_users_tenant_email on tenant_users(tenant_id, lower(email));

create table tenant_user_roles (
    tenant_user_id uuid not null references tenant_users(id) on delete cascade,
    role varchar(60) not null,
    primary key (tenant_user_id, role)
);

create table tenant_user_tokens (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_user_id uuid not null references tenant_users(id) on delete cascade,
    token_hash varchar(255) not null unique,
    type varchar(40) not null,
    expires_at timestamptz not null,
    used_at timestamptz
);

create table platform_settings (
    key varchar(255) primary key,
    value text not null
);
