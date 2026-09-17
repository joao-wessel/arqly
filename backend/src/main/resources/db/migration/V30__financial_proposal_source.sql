create unique index ux_financial_entry_proposal on financial_entries(proposal_id) where proposal_id is not null and deleted = false;
