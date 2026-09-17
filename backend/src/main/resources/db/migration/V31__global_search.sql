create extension if not exists unaccent;

create index if not exists idx_clients_search_tenant on clients (tenant_id) where deleted = false;
create index if not exists idx_briefings_search_tenant on briefings (tenant_id) where deleted = false;
create index if not exists idx_proposals_search_tenant on proposals (tenant_id) where deleted = false;
create index if not exists idx_projects_search_tenant on projects (tenant_id) where deleted = false;
create index if not exists idx_project_stages_search_tenant on project_stages (tenant_id) where deleted = false;
create index if not exists idx_generated_documents_search_tenant on generated_documents (tenant_id) where deleted = false;
create index if not exists idx_file_resources_search_tenant on file_resources (tenant_id, status);
