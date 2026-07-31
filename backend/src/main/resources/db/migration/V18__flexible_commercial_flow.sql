alter table proposals add column if not exists origin_type varchar(30) not null default 'MANUAL';
update proposals set origin_type = 'BRIEFING' where briefing_id is not null;
update proposals set origin_type = 'MANUAL' where briefing_id is null;

alter table projects add column if not exists origin_type varchar(30) not null default 'PROPOSAL';
update projects set origin_type = 'PROPOSAL' where proposal_id is not null;
update projects set origin_type = 'MANUAL' where proposal_id is null;

alter table projects alter column proposal_id drop not null;

create index if not exists ix_proposals_tenant_origin on proposals(tenant_id, origin_type, deleted);
create index if not exists ix_projects_tenant_origin on projects(tenant_id, origin_type, deleted);
