insert into project_services (
    id,
    created_at,
    updated_at,
    project_id,
    name,
    description,
    quantity,
    unit,
    contracted_value
)
select
    gen_random_uuid(),
    now(),
    now(),
    p.id,
    pi.service_name,
    coalesce(nullif(pi.custom_description, ''), pi.service_description),
    pi.quantity,
    pi.unit,
    pi.total
from projects p
join proposal_items pi on pi.proposal_id = p.proposal_id
where p.deleted = false
  and not exists (
      select 1
      from project_services ps
      where ps.project_id = p.id
  );
