create table project_stage_checklist_items (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    stage_id uuid not null references project_stages(id) on delete cascade,
    description varchar(255) not null,
    completed boolean not null default false,
    display_order integer not null,
    completed_at timestamptz,
    completed_by varchar(255),
    notes text,
    deleted boolean not null default false,
    deleted_at timestamptz
);

insert into project_stage_checklist_items (
    id, created_at, updated_at, tenant_id, stage_id, description, completed,
    display_order, completed_at, deleted, deleted_at
)
select id, created_at, updated_at, tenant_id, stage_id, title, completed,
       display_order, completed_at, deleted, deleted_at
from project_stage_checklists
where not exists (
    select 1
    from project_stage_checklist_items item
    where item.id = project_stage_checklists.id
);

create table project_stage_timelines (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    stage_id uuid not null references project_stages(id) on delete cascade,
    type varchar(40) not null,
    description text not null,
    actor varchar(255),
    metadata varchar(500)
);

create table project_stage_comments (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    stage_id uuid not null references project_stages(id) on delete cascade,
    author_id varchar(80) not null,
    author_name varchar(255) not null,
    comment text not null,
    edited boolean not null default false,
    deleted boolean not null default false,
    deleted_at timestamptz
);

insert into project_stage_timelines (
    id, created_at, updated_at, tenant_id, stage_id, type, description, actor
)
select gen_random_uuid(), ps.created_at, ps.created_at, ps.tenant_id, ps.id,
       'SYSTEM', 'Etapa criada', ps.created_by
from project_stages ps
where ps.deleted = false
  and not exists (
      select 1
      from project_stage_timelines pst
      where pst.stage_id = ps.id
        and pst.type = 'SYSTEM'
        and pst.description = 'Etapa criada'
  );

create index ix_stage_checklist_items_stage on project_stage_checklist_items(stage_id, deleted, display_order);
create index ix_stage_checklist_items_tenant_pending on project_stage_checklist_items(tenant_id, completed, deleted);
create index ix_stage_timelines_stage on project_stage_timelines(stage_id, created_at desc);
create index ix_stage_comments_stage on project_stage_comments(stage_id, deleted, created_at desc);
