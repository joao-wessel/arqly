package com.arqly.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "construction_diary_participants")
public class ConstructionDiaryParticipant extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "diary_entry_id", nullable = false) private ConstructionDiaryEntry diaryEntry;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ConstructionDiaryParticipantType participantType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "tenant_user_id") private TenantUser tenantUser;
    private String name; private String company; private String role; private String phone; private String email;
    public ConstructionDiaryEntry getDiaryEntry(){return diaryEntry;} public void setDiaryEntry(ConstructionDiaryEntry value){diaryEntry=value;}
    public ConstructionDiaryParticipantType getParticipantType(){return participantType;} public void setParticipantType(ConstructionDiaryParticipantType value){participantType=value;}
    public TenantUser getTenantUser(){return tenantUser;} public void setTenantUser(TenantUser value){tenantUser=value;}
    public String getName(){return name;} public void setName(String value){name=value;}
    public String getCompany(){return company;} public void setCompany(String value){company=value;}
    public String getRole(){return role;} public void setRole(String value){role=value;}
    public String getPhone(){return phone;} public void setPhone(String value){phone=value;}
    public String getEmail(){return email;} public void setEmail(String value){email=value;}
}
