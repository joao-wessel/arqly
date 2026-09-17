-- Archived files remain part of the diary history. Link legacy image uploads so
-- they reappear in the gallery as soon as the file is restored.
with candidates as (
    select
        file_resource.id as file_resource_id,
        file_resource.owner_id as diary_entry_id,
        file_resource.visibility,
        row_number() over (
            partition by file_resource.owner_id
            order by file_resource.created_at, file_resource.id
        ) as row_position
    from file_resources file_resource
    join construction_diary_entries entry on entry.id = file_resource.owner_id
    where file_resource.owner_type = 'CONSTRUCTION_DIARY_ENTRY'
      and file_resource.status <> 'DELETED'
      and file_resource.deleted_at is null
      and entry.deleted = false
      and lower(file_resource.extension) in ('png', 'jpg', 'jpeg', 'webp', 'svg')
      and not exists (
          select 1
          from construction_diary_photos photo
          where photo.diary_entry_id = file_resource.owner_id
            and photo.file_resource_id = file_resource.id
      )
), existing_orders as (
    select diary_entry_id, coalesce(max(display_order), 0) as max_display_order
    from construction_diary_photos
    group by diary_entry_id
)
insert into construction_diary_photos (
    id, created_at, updated_at, diary_entry_id, file_resource_id, display_order, visibility
)
select
    gen_random_uuid(), now(), now(), candidates.diary_entry_id, candidates.file_resource_id,
    coalesce(existing_orders.max_display_order, 0) + candidates.row_position, candidates.visibility
from candidates
left join existing_orders on existing_orders.diary_entry_id = candidates.diary_entry_id;
