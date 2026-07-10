drop index if exists ix_service_categories_tenant_order;

alter table service_categories
    drop column if exists display_order;

alter table services
    drop column if exists display_order;
