alter table tenants
    add column person_type varchar(40) not null default 'LEGAL_ENTITY';
