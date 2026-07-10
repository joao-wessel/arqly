package com.arqly.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients")
public class Client extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientPersonType personType;

    private String name;
    private String cpf;
    private String rg;
    private LocalDate birthDate;
    private String legalName;
    private String tradeName;
    private String cnpj;
    private String stateRegistration;

    @Column(nullable = false)
    private String email;
    private String phone;
    private String whatsapp;

    private String zipCode;
    private String street;
    private String number;
    private String complement;
    private String district;
    private String city;
    @Column(length = 2)
    private String state;

    @Column(columnDefinition = "text")
    private String notes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientStatus status = ClientStatus.ACTIVE;
    private boolean deleted = false;
    private Instant deletedAt;

    @OneToMany(mappedBy = "client")
    private List<ClientPortalAccess> portalAccesses = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        touch();
    }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public ClientPersonType getPersonType() { return personType; }
    public void setPersonType(ClientPersonType personType) { this.personType = personType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getStateRegistration() { return stateRegistration; }
    public void setStateRegistration(String stateRegistration) { this.stateRegistration = stateRegistration; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getComplement() { return complement; }
    public void setComplement(String complement) { this.complement = complement; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public ClientStatus getStatus() { return status; }
    public void setStatus(ClientStatus status) { this.status = status; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public List<ClientPortalAccess> getPortalAccesses() { return portalAccesses; }
    public void setPortalAccesses(List<ClientPortalAccess> portalAccesses) { this.portalAccesses = portalAccesses; }
}
