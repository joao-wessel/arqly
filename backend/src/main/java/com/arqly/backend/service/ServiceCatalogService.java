package com.arqly.backend.service;

import com.arqly.backend.dto.ServiceCatalogDtos.ServiceCatalogStatsResponse;
import com.arqly.backend.dto.ServiceCatalogDtos.ServiceCategoryRequest;
import com.arqly.backend.dto.ServiceCatalogDtos.ServiceCategoryResponse;
import com.arqly.backend.dto.ServiceCatalogDtos.ServiceRequest;
import com.arqly.backend.dto.ServiceCatalogDtos.ServiceResponse;
import com.arqly.backend.dto.ServiceCatalogDtos.ServiceSummaryResponse;
import com.arqly.backend.entity.BillingUnit;
import com.arqly.backend.entity.Service;
import com.arqly.backend.entity.ServiceCategory;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.mapper.ServiceCategoryMapper;
import com.arqly.backend.mapper.ServiceMapper;
import com.arqly.backend.repository.ServiceCategoryRepository;
import com.arqly.backend.repository.ServiceRepository;
import com.arqly.backend.repository.TenantRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@org.springframework.stereotype.Service
public class ServiceCatalogService {
    private static final Logger log = LoggerFactory.getLogger(ServiceCatalogService.class);

    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;
    private final ServiceMapper serviceMapper;
    private final ServiceCategoryMapper categoryMapper;

    public ServiceCatalogService(ServiceRepository serviceRepository, ServiceCategoryRepository categoryRepository,
                                 TenantRepository tenantRepository, ServiceMapper serviceMapper,
                                 ServiceCategoryMapper categoryMapper) {
        this.serviceRepository = serviceRepository;
        this.categoryRepository = categoryRepository;
        this.tenantRepository = tenantRepository;
        this.serviceMapper = serviceMapper;
        this.categoryMapper = categoryMapper;
    }

    public Page<ServiceSummaryResponse> listServices(UUID tenantId, String name, UUID categoryId, Boolean active,
                                                     BigDecimal minValue, BigDecimal maxValue, Boolean featured,
                                                     Pageable pageable) {
        return serviceRepository.findAll(serviceSpecification(tenantId, name, categoryId, active, minValue, maxValue, featured), pageable)
                .map(this::toSummary);
    }

    public ServiceResponse getService(UUID tenantId, UUID id) {
        return toResponse(findTenantService(tenantId, id));
    }

    @org.springframework.transaction.annotation.Transactional
    public ServiceResponse createService(UUID tenantId, ServiceRequest request) {
        validateServiceName(tenantId, null, request.name());
        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant não encontrado."));
        var service = serviceMapper.toEntity(request);
        service.setTenant(tenant);
        service.setCategory(resolveCategory(tenantId, request.categoryId()));
        log.info("Creating service '{}' for tenant {}", request.name(), tenantId);
        return toResponse(serviceRepository.save(service));
    }

    @org.springframework.transaction.annotation.Transactional
    public ServiceResponse updateService(UUID tenantId, UUID id, ServiceRequest request) {
        var service = findTenantService(tenantId, id);
        validateServiceName(tenantId, id, request.name());
        serviceMapper.update(request, service);
        service.setCategory(resolveCategory(tenantId, request.categoryId()));
        log.info("Updating service {} for tenant {}", id, tenantId);
        return toResponse(service);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteService(UUID tenantId, UUID id) {
        var service = findTenantService(tenantId, id);
        service.setActive(false);
        service.setDeleted(true);
        service.setDeletedAt(Instant.now());
        log.info("Soft deleting service {} for tenant {}", id, tenantId);
    }

    @org.springframework.transaction.annotation.Transactional
    public ServiceResponse activateService(UUID tenantId, UUID id) {
        var service = findTenantService(tenantId, id);
        service.setActive(true);
        return toResponse(service);
    }

    @org.springframework.transaction.annotation.Transactional
    public ServiceResponse deactivateService(UUID tenantId, UUID id) {
        var service = findTenantService(tenantId, id);
        service.setActive(false);
        return toResponse(service);
    }

    public Page<ServiceCategoryResponse> listCategories(UUID tenantId, String name, Boolean active, Pageable pageable) {
        return categoryRepository.findAll(categorySpecification(tenantId, name, active), pageable).map(this::toCategoryResponse);
    }

    public List<ServiceCategoryResponse> categoryOptions(UUID tenantId) {
        return categoryRepository.findAllByTenantIdAndDeletedFalseOrderByNameAsc(tenantId).stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    public ServiceCategoryResponse getCategory(UUID tenantId, UUID id) {
        return toCategoryResponse(findTenantCategory(tenantId, id));
    }

    @org.springframework.transaction.annotation.Transactional
    public ServiceCategoryResponse createCategory(UUID tenantId, ServiceCategoryRequest request) {
        validateCategoryName(tenantId, null, request.name());
        var tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant não encontrado."));
        var category = categoryMapper.toEntity(request);
        category.setTenant(tenant);
        log.info("Creating service category '{}' for tenant {}", request.name(), tenantId);
        return toCategoryResponse(categoryRepository.save(category));
    }

    @org.springframework.transaction.annotation.Transactional
    public ServiceCategoryResponse updateCategory(UUID tenantId, UUID id, ServiceCategoryRequest request) {
        var category = findTenantCategory(tenantId, id);
        validateCategoryName(tenantId, id, request.name());
        categoryMapper.update(request, category);
        log.info("Updating service category {} for tenant {}", id, tenantId);
        return toCategoryResponse(category);
    }

    @org.springframework.transaction.annotation.Transactional
    public ServiceCategoryResponse activateCategory(UUID tenantId, UUID id) {
        var category = findTenantCategory(tenantId, id);
        category.setActive(true);
        return toCategoryResponse(category);
    }

    @org.springframework.transaction.annotation.Transactional
    public ServiceCategoryResponse deactivateCategory(UUID tenantId, UUID id) {
        var category = findTenantCategory(tenantId, id);
        category.setActive(false);
        return toCategoryResponse(category);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteCategory(UUID tenantId, UUID id) {
        var category = findTenantCategory(tenantId, id);
        if (serviceRepository.countByCategoryIdAndDeletedFalse(id) > 0) {
            throw new BusinessException("Categorias com serviços vinculados devem ser apenas inativadas.");
        }
        category.setActive(false);
        category.setDeleted(true);
        category.setDeletedAt(Instant.now());
        log.info("Soft deleting service category {} for tenant {}", id, tenantId);
    }

    public ServiceCatalogStatsResponse stats(UUID tenantId) {
        return new ServiceCatalogStatsResponse(
                serviceRepository.countByTenantIdAndDeletedFalse(tenantId),
                categoryRepository.countByTenantIdAndDeletedFalse(tenantId),
                serviceRepository.countByTenantIdAndActiveTrueAndDeletedFalse(tenantId),
                serviceRepository.countByTenantIdAndActiveFalseAndDeletedFalse(tenantId)
        );
    }

    private Specification<Service> serviceSpecification(UUID tenantId, String name, UUID categoryId, Boolean active,
                                                        BigDecimal minValue, BigDecimal maxValue, Boolean featured) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenant").get("id"), tenantId));
            predicates.add(builder.isFalse(root.get("deleted")));
            if (name != null && !name.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (categoryId != null) {
                predicates.add(builder.equal(root.get("category").get("id"), categoryId));
            }
            if (active != null) {
                predicates.add(builder.equal(root.get("active"), active));
            }
            if (featured != null) {
                predicates.add(builder.equal(root.get("featured"), featured));
            }
            if (minValue != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("baseValue"), minValue));
            }
            if (maxValue != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("baseValue"), maxValue));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<ServiceCategory> categorySpecification(UUID tenantId, String name, Boolean active) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenant").get("id"), tenantId));
            predicates.add(builder.isFalse(root.get("deleted")));
            if (name != null && !name.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (active != null) {
                predicates.add(builder.equal(root.get("active"), active));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validateServiceName(UUID tenantId, UUID currentId, String name) {
        serviceRepository.findAll(serviceSpecification(tenantId, name, null, null, null, null, null)).stream()
                .filter(service -> service.getName().equalsIgnoreCase(name))
                .filter(service -> currentId == null || !service.getId().equals(currentId))
                .findFirst()
                .ifPresent(service -> { throw new BusinessException("Já existe um serviço com este nome."); });
    }

    private void validateCategoryName(UUID tenantId, UUID currentId, String name) {
        categoryRepository.findAll(categorySpecification(tenantId, name, null)).stream()
                .filter(category -> category.getName().equalsIgnoreCase(name))
                .filter(category -> currentId == null || !category.getId().equals(currentId))
                .findFirst()
                .ifPresent(category -> { throw new BusinessException("Já existe uma categoria com este nome."); });
    }

    private ServiceCategory resolveCategory(UUID tenantId, UUID categoryId) {
        if (categoryId == null) return null;
        return findTenantCategory(tenantId, categoryId);
    }

    private Service findTenantService(UUID tenantId, UUID id) {
        return serviceRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado."));
    }

    private ServiceCategory findTenantCategory(UUID tenantId, UUID id) {
        return categoryRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Categoria não encontrada."));
    }

    private ServiceSummaryResponse toSummary(Service service) {
        var category = service.getCategory();
        return new ServiceSummaryResponse(service.getId(), service.getName(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                category == null ? null : category.getColor(),
                service.getBaseValue(), service.getCurrency(), service.getBillingUnit(), service.isActive(),
                service.isFeatured(), service.getCreatedAt(), service.getUpdatedAt());
    }

    private ServiceResponse toResponse(Service service) {
        var category = service.getCategory();
        return new ServiceResponse(service.getId(), service.getName(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                service.getShortDescription(), service.getFullDescription(), service.getBaseValue(),
                service.getCurrency(), service.getBillingUnit(), service.isActive(), service.isFeatured(),
                service.getCreatedAt(), service.getUpdatedAt());
    }

    private ServiceCategoryResponse toCategoryResponse(ServiceCategory category) {
        return new ServiceCategoryResponse(category.getId(), category.getName(), category.getDescription(),
                category.getColor(), category.getIcon(), category.isActive(),
                serviceRepository.countByCategoryIdAndDeletedFalse(category.getId()),
                category.getCreatedAt(), category.getUpdatedAt());
    }
}
