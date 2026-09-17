package com.arqly.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "construction_diary_decisions")
public class ConstructionDiaryDecision extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "diary_entry_id", nullable = false) private ConstructionDiaryEntry diaryEntry;
    @Column(columnDefinition = "text", nullable = false) private String description;
    private String decidedBy;
    private LocalDate decisionDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ActivityVisibility visibility = ActivityVisibility.INTERNAL;
    public ConstructionDiaryEntry getDiaryEntry(){return diaryEntry;} public void setDiaryEntry(ConstructionDiaryEntry value){diaryEntry=value;}
    public String getDescription(){return description;} public void setDescription(String value){description=value;}
    public String getDecidedBy(){return decidedBy;} public void setDecidedBy(String value){decidedBy=value;}
    public LocalDate getDecisionDate(){return decisionDate;} public void setDecisionDate(LocalDate value){decisionDate=value;}
    public ActivityVisibility getVisibility(){return visibility;} public void setVisibility(ActivityVisibility value){visibility=value;}
}
