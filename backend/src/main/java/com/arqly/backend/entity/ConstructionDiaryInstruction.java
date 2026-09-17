package com.arqly.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "construction_diary_instructions")
public class ConstructionDiaryInstruction extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "diary_entry_id", nullable = false) private ConstructionDiaryEntry diaryEntry;
    @Column(columnDefinition = "text", nullable = false) private String description;
    private String responsible;
    private LocalDate deadline;
    @Column(nullable = false) private boolean completed = false;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ActivityVisibility visibility = ActivityVisibility.INTERNAL;
    public ConstructionDiaryEntry getDiaryEntry(){return diaryEntry;} public void setDiaryEntry(ConstructionDiaryEntry value){diaryEntry=value;}
    public String getDescription(){return description;} public void setDescription(String value){description=value;}
    public String getResponsible(){return responsible;} public void setResponsible(String value){responsible=value;}
    public LocalDate getDeadline(){return deadline;} public void setDeadline(LocalDate value){deadline=value;}
    public boolean isCompleted(){return completed;} public void setCompleted(boolean value){completed=value;}
    public ActivityVisibility getVisibility(){return visibility;} public void setVisibility(ActivityVisibility value){visibility=value;}
}
