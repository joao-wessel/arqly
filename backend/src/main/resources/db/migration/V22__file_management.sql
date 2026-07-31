create table file_folders (
    id uuid primary key,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    tenant_id uuid not null references tenants(id),
    owner_type varchar(40) not null,
    owner_id uuid not null,
    parent_id uuid references file_folders(id),
    name varchar(255) not null,
    deleted boolean not null default false,
    deleted_at timestamptz
);

create index ix_file_folders_owner on file_folders(tenant_id, owner_type, owner_id, parent_id) where deleted = false;
create unique index uk_file_folder_name
    on file_folders(tenant_id, owner_type, owner_id, parent_id, lower(name)) nulls not distinct
    where deleted = false;

create table file_tags (
    id uuid primary key,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    tenant_id uuid not null references tenants(id),
    name varchar(255) not null,
    constraint uk_file_tag_name unique (tenant_id, name)
);

create table file_resources (
    id uuid primary key,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    tenant_id uuid not null references tenants(id),
    owner_type varchar(40) not null,
    owner_id uuid not null,
    folder_id uuid references file_folders(id),
    name varchar(255) not null,
    original_name varchar(255) not null,
    extension varchar(30) not null,
    mime_type varchar(255) not null,
    size bigint not null,
    storage_key varchar(700) not null unique,
    checksum varchar(64) not null,
    version integer not null default 1,
    visibility varchar(30) not null default 'INTERNAL',
    status varchar(30) not null default 'ACTIVE',
    uploaded_by_id uuid not null references tenant_users(id),
    deleted_at timestamptz
);

create index ix_file_resources_owner on file_resources(tenant_id, owner_type, owner_id, folder_id, created_at desc);
create index ix_file_resources_name on file_resources(tenant_id, lower(name));
create index ix_file_resources_checksum on file_resources(tenant_id, checksum);

create table file_versions (
    id uuid primary key,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    file_resource_id uuid not null references file_resources(id),
    version_number integer not null,
    storage_key varchar(700) not null unique,
    checksum varchar(64) not null,
    size bigint not null,
    author_id uuid not null references tenant_users(id),
    revision_comment text,
    constraint uk_file_version_number unique (file_resource_id, version_number)
);

create index ix_file_versions_file on file_versions(file_resource_id, version_number desc);

create table file_resource_tags (
    file_resource_id uuid not null references file_resources(id),
    tag_id uuid not null references file_tags(id),
    primary key (file_resource_id, tag_id)
);
