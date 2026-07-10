create table platform_user_tokens (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    platform_user_id uuid not null references platform_users(id) on delete cascade,
    token_hash varchar(255) not null unique,
    type varchar(40) not null,
    expires_at timestamptz not null,
    used_at timestamptz
);
