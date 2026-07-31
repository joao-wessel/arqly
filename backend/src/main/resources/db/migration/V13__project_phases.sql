create table project_phase_templates (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    project_template_id uuid not null references project_templates(id),
    name varchar(255) not null,
    description text,
    display_order integer not null,
    color varchar(40),
    icon varchar(80),
    active boolean not null default true,
    deleted boolean not null default false,
    deleted_at timestamptz
);

create index ix_project_phase_templates_template on project_phase_templates(project_template_id, deleted, display_order);
create index ix_project_phase_templates_tenant on project_phase_templates(tenant_id, deleted);

create table project_phases (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id) on delete cascade,
    phase_template_id uuid references project_phase_templates(id),
    name varchar(255) not null,
    description text,
    display_order integer not null,
    color varchar(40),
    icon varchar(80),
    status varchar(40) not null default 'NOT_STARTED',
    completion_percentage numeric(5,2) not null default 0,
    notes text,
    created_by varchar(255),
    updated_by varchar(255),
    deleted boolean not null default false,
    deleted_at timestamptz
);

create index ix_project_phases_project on project_phases(project_id, deleted, display_order);
create index ix_project_phases_tenant_status on project_phases(tenant_id, status, deleted);

alter table project_stage_templates add column if not exists project_phase_template_id uuid references project_phase_templates(id);
alter table project_stage_templates add column if not exists progress_calculation_mode varchar(40) not null default 'MANUAL';

alter table project_stages add column if not exists project_phase_id uuid references project_phases(id);
alter table project_stages add column if not exists progress_calculation_mode varchar(40) not null default 'MANUAL';

insert into project_phase_templates (
    id, created_at, updated_at, tenant_id, project_template_id, name, description, display_order, color, icon, active, deleted
)
select gen_random_uuid(), now(), now(), pst.tenant_id, pst.project_template_id, 'Geral',
       'Fase criada automaticamente para preservar etapas existentes.', 1, '#0f766e', 'Layers', true, false
from (
    select distinct tenant_id, project_template_id
    from project_stage_templates
    where deleted = false and project_template_id is not null
) pst
where not exists (
    select 1
    from project_phase_templates ppt
    where ppt.tenant_id = pst.tenant_id
      and ppt.project_template_id = pst.project_template_id
      and ppt.name = 'Geral'
      and ppt.deleted = false
);

update project_stage_templates pst
set project_phase_template_id = ppt.id
from project_phase_templates ppt
where pst.project_phase_template_id is null
  and pst.project_template_id = ppt.project_template_id
  and pst.tenant_id = ppt.tenant_id
  and ppt.name = 'Geral'
  and ppt.deleted = false;

insert into project_phases (
    id, created_at, updated_at, tenant_id, project_id, phase_template_id, name, description, display_order,
    color, icon, status, completion_percentage, created_by, updated_by, deleted
)
select gen_random_uuid(), now(), now(), p.tenant_id, p.id, ppt.id, 'Geral',
       'Fase criada automaticamente para preservar etapas existentes.', 1, '#0f766e', 'Layers',
       'NOT_STARTED', 0, p.created_by, p.updated_by, false
from projects p
left join project_phase_templates ppt
       on ppt.project_template_id = p.project_template_id
      and ppt.tenant_id = p.tenant_id
      and ppt.name = 'Geral'
      and ppt.deleted = false
where exists (
    select 1 from project_stages ps where ps.project_id = p.id and ps.deleted = false
)
and not exists (
    select 1 from project_phases pp where pp.project_id = p.id and pp.deleted = false
);

update project_stages ps
set project_phase_id = pp.id
from project_phases pp
where ps.project_phase_id is null
  and ps.project_id = pp.project_id
  and ps.tenant_id = pp.tenant_id
  and pp.name = 'Geral'
  and pp.deleted = false;

insert into project_templates (id, created_at, updated_at, tenant_id, name, description, active, deleted)
select gen_random_uuid(), now(), now(), t.id, 'Projeto Arquitetônico Residencial',
       'Modelo inicial com fases e etapas padrão para projetos residenciais.', true, false
from tenants t
where not exists (
    select 1
    from project_templates pt
    where pt.tenant_id = t.id
      and lower(pt.name) = lower('Projeto Arquitetônico Residencial')
      and pt.deleted = false
);

insert into project_phase_templates (id, created_at, updated_at, tenant_id, project_template_id, name, description, display_order, color, icon, active, deleted)
select gen_random_uuid(), now(), now(), pt.tenant_id, pt.id, phase.name, phase.description, phase.display_order, phase.color, phase.icon, true, false
from project_templates pt
cross join (
    values
      ('Planejamento', 'Organização inicial e entendimento do projeto.', 1, '#0f766e', 'ClipboardList'),
      ('Desenvolvimento', 'Desenvolvimento técnico e conceitual.', 2, '#2563eb', 'DraftingCompass'),
      ('Aprovações', 'Aprovações legais e validações externas.', 3, '#b45309', 'BadgeCheck'),
      ('Execução', 'Acompanhamento da execução contratada.', 4, '#7c3aed', 'Hammer'),
      ('Encerramento', 'Entrega final e encerramento técnico.', 5, '#475569', 'PackageCheck')
) as phase(name, description, display_order, color, icon)
where lower(pt.name) = lower('Projeto Arquitetônico Residencial')
  and not exists (
      select 1
      from project_phase_templates ppt
      where ppt.project_template_id = pt.id
        and ppt.tenant_id = pt.tenant_id
        and ppt.name = phase.name
        and ppt.deleted = false
  );

insert into project_stage_templates (
    id, created_at, updated_at, tenant_id, project_template_id, project_phase_template_id, name, description, display_order,
    weight_percentage, progress_calculation_mode, active, deleted
)
select gen_random_uuid(), now(), now(), ppt.tenant_id, pt.id, ppt.id, stage.name, stage.description, stage.display_order,
       stage.weight_percentage, 'MANUAL', true, false
from project_phase_templates ppt
join project_templates pt on pt.id = ppt.project_template_id
join (
    values
      ('Planejamento', 'Levantamento', 'Coleta de medidas, fotos e documentação inicial.', 1, 50.00),
      ('Planejamento', 'Programa de Necessidades', 'Definição das necessidades do cliente.', 2, 50.00),
      ('Desenvolvimento', 'Estudo Preliminar', 'Primeiras soluções de projeto.', 1, 30.00),
      ('Desenvolvimento', 'Anteprojeto', 'Desenvolvimento das soluções aprovadas.', 2, 35.00),
      ('Desenvolvimento', 'Projeto Executivo', 'Detalhamento técnico para execução.', 3, 35.00),
      ('Aprovações', 'Projeto Legal', 'Documentação para aprovação legal.', 1, 100.00),
      ('Execução', 'Acompanhamento da Obra', 'Acompanhamento técnico da obra.', 1, 100.00),
      ('Encerramento', 'Entrega', 'Entrega final ao cliente.', 1, 100.00)
) as stage(phase_name, name, description, display_order, weight_percentage)
  on stage.phase_name = ppt.name
where lower(pt.name) = lower('Projeto Arquitetônico Residencial')
  and not exists (
      select 1
      from project_stage_templates pst
      where pst.project_phase_template_id = ppt.id
        and pst.tenant_id = ppt.tenant_id
        and pst.name = stage.name
        and pst.deleted = false
  );

alter table project_stage_templates alter column project_phase_template_id set not null;
alter table project_stages alter column project_phase_id set not null;

create index ix_project_stage_templates_phase on project_stage_templates(project_phase_template_id, deleted, display_order);
create index ix_project_stages_phase on project_stages(project_phase_id, deleted, display_order);
