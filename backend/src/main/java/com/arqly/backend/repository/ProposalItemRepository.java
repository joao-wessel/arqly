package com.arqly.backend.repository;

import com.arqly.backend.entity.ProposalItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalItemRepository extends JpaRepository<ProposalItem, UUID> {
    List<ProposalItem> findAllByProposalIdOrderByCreatedAtAsc(UUID proposalId);
}
