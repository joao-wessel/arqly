alter table project_stage_templates alter column project_template_id drop not null;
alter table project_stages alter column project_id drop not null;
