package com.arqly.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "construction_diary_photos")
public class ConstructionDiaryPhoto extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "diary_entry_id", nullable = false) private ConstructionDiaryEntry diaryEntry;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "file_resource_id", nullable = false) private FileResource file;
    private String caption;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "display_order", nullable = false) private int order;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ActivityVisibility visibility = ActivityVisibility.INTERNAL;
    public ConstructionDiaryEntry getDiaryEntry(){return diaryEntry;} public void setDiaryEntry(ConstructionDiaryEntry value){diaryEntry=value;}
    public FileResource getFile(){return file;} public void setFile(FileResource value){file=value;}
    public String getCaption(){return caption;} public void setCaption(String value){caption=value;}
    public String getDescription(){return description;} public void setDescription(String value){description=value;}
    public int getOrder(){return order;} public void setOrder(int value){order=value;}
    public ActivityVisibility getVisibility(){return visibility;} public void setVisibility(ActivityVisibility value){visibility=value;}
}
