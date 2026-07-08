alter table platform_users
    add column theme_mode varchar(20) not null default 'light',
    add column color_palette varchar(40) not null default 'arqly';

alter table tenant_users
    add column theme_mode varchar(20) not null default 'light',
    add column color_palette varchar(40) not null default 'arqly';
