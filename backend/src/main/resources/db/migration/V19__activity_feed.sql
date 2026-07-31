create table activities (
    id uuid primary key,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    phase_id uuid references project_phases(id),
    stage_id uuid references project_stages(id),
    author_id uuid references tenant_users(id),
    author_name varchar(255) not null,
    type varchar(60) not null,
    visibility varchar(40) not null default 'INTERNAL',
    deleted boolean not null default false,
    deleted_at timestamptz
);

create table activity_contents (
    activity_id uuid primary key references activities(id) on delete cascade,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    title varchar(255) not null,
    description text not null,
    metadata jsonb,
    edited boolean not null default false,
    edited_at timestamptz
);

create index ix_activities_tenant_stage_created on activities(tenant_id, stage_id, created_at desc) where deleted = false;
create index ix_activities_tenant_project_created on activities(tenant_id, project_id, created_at desc) where deleted = false;
create index ix_activities_tenant_type_created on activities(tenant_id, type, created_at desc) where deleted = false;

insert into activities (id, created_at, updated_at, tenant_id, project_id, phase_id, stage_id, author_name, type, visibility, deleted)
select gen_random_uuid(), t.created_at, t.updated_at, t.tenant_id, ph.project_id, s.project_phase_id, t.stage_id,
       coalesce(nullif(t.actor, ''), 'Sistema'),
       case
           when t.type = 'COMMENT' then 'COMMENT'
           when t.type = 'STATUS' then 'STATUS_CHANGED'
           when t.type = 'CHECKLIST' then 'CHECKLIST_UPDATED'
           when t.type = 'FILE' then 'FILE_UPLOADED'
           else 'SYSTEM'
       end,
       'INTERNAL',
       false
from project_stage_timelines t
join project_stages s on s.id = t.stage_id
join project_phases ph on ph.id = s.project_phase_id
where not exists (
    select 1 from activities a
    where a.stage_id = t.stage_id
      and a.created_at = t.created_at
      and a.author_name = coalesce(nullif(t.actor, ''), 'Sistema')
);

insert into activity_contents (activity_id, created_at, updated_at, title, description, metadata, edited)
select a.id, a.created_at, a.updated_at,
       case
           when a.type = 'COMMENT' then 'Comentário'
           when a.type = 'STATUS_CHANGED' then 'Status alterado'
           when a.type = 'CHECKLIST_UPDATED' then 'Checklist atualizado'
           when a.type = 'FILE_UPLOADED' then 'Arquivo enviado'
           else 'Atividade'
       end,
       t.description,
       nullif(t.metadata, '')::jsonb,
       false
from activities a
join project_stage_timelines t on t.stage_id = a.stage_id
    and t.created_at = a.created_at
    and coalesce(nullif(t.actor, ''), 'Sistema') = a.author_name
where not exists (select 1 from activity_contents c where c.activity_id = a.id);

insert into activities (id, created_at, updated_at, tenant_id, project_id, phase_id, stage_id, author_name, type, visibility, deleted, deleted_at)
select gen_random_uuid(), c.created_at, c.updated_at, c.tenant_id, ph.project_id, s.project_phase_id, c.stage_id,
       c.author_name, 'COMMENT', 'INTERNAL', c.deleted, c.deleted_at
from project_stage_comments c
join project_stages s on s.id = c.stage_id
join project_phases ph on ph.id = s.project_phase_id
where not exists (
    select 1 from activities a
    where a.stage_id = c.stage_id
      and a.created_at = c.created_at
      and a.author_name = c.author_name
      and a.type = 'COMMENT'
);

insert into activity_contents (activity_id, created_at, updated_at, title, description, metadata, edited, edited_at)
select a.id, a.created_at, a.updated_at, 'Comentário',
       case when a.deleted then 'Comentário removido.' else c.comment end,
       null,
       c.edited,
       case when c.edited then c.updated_at else null end
from activities a
join project_stage_comments c on c.stage_id = a.stage_id
    and c.created_at = a.created_at
    and c.author_name = a.author_name
where a.type = 'COMMENT'
  and not exists (select 1 from activity_contents content where content.activity_id = a.id);
