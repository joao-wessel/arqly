alter table clients
    alter column phone drop not null,
    alter column zip_code drop not null,
    alter column street drop not null,
    alter column number drop not null,
    alter column district drop not null,
    alter column city drop not null,
    alter column state drop not null;
