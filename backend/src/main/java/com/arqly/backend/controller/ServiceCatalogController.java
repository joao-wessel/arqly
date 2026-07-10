package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.ServiceCatalogDtos.ServiceCatalogStatsResponse;
import com.arqly.backend.dto.ServiceCatalogDtos.ServiceCategoryRequest;
import com.arqly.backend.dto.ServiceCatalogDtos.ServiceCategoryResponse;
import com.arqly.backend.dto.ServiceCatalogDtos.ServiceRequest;
import com.arqly.backend.dto.ServiceCatalogDtos.ServiceResponse;
import com.arqly.backend.dto.ServiceCatalogDtos.ServiceSummaryResponse;
import com.arqly.backend.entity.BillingUnit;
import com.arqly.backend.security.AuthenticatedUser;
import com.arqly.backend.service.ServiceCatalogService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/service-catalog")
public class ServiceCatalogController {
    private final ServiceCatalogService service;

    public ServiceCatalogController(ServiceCatalogService service) {
        this.service = service;
    }

    @GetMapping("/services")
    public ApiResponse<Page<ServiceSummaryResponse>> listServices(@AuthenticationPrincipal AuthenticatedUser user,
                                                                  @RequestParam(required = false) String name,
                                                                  @RequestParam(required = false) UUID categoryId,
                                                                  @RequestParam(required = false) Boolean active,
                                                                  @RequestParam(required = false) BigDecimal minValue,
                                                                  @RequestParam(required = false) BigDecimal maxValue,
                                                                  @RequestParam(required = false) Boolean featured,
                                                                  Pageable pageable) {
        return ApiResponse.ok(service.listServices(user.getTenantId(), name, categoryId, active, minValue, maxValue, featured, pageable));
    }

    @GetMapping("/services/{id}")
    public ApiResponse<ServiceResponse> getService(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.getService(user.getTenantId(), id));
    }

    @PostMapping("/services")
    public ApiResponse<ServiceResponse> createService(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @Valid @RequestBody ServiceRequest request) {
        return ApiResponse.ok(service.createService(user.getTenantId(), request));
    }

    @PutMapping("/services/{id}")
    public ApiResponse<ServiceResponse> updateService(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody ServiceRequest request) {
        return ApiResponse.ok(service.updateService(user.getTenantId(), id, request));
    }

    @DeleteMapping("/services/{id}")
    public ApiResponse<Void> deleteService(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        service.deleteService(user.getTenantId(), id);
        return ApiResponse.message("Serviço removido.");
    }

    @PatchMapping("/services/{id}/activate")
    public ApiResponse<ServiceResponse> activateService(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.activateService(user.getTenantId(), id));
    }

    @PatchMapping("/services/{id}/deactivate")
    public ApiResponse<ServiceResponse> deactivateService(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.deactivateService(user.getTenantId(), id));
    }

    @GetMapping("/categories")
    public ApiResponse<Page<ServiceCategoryResponse>> listCategories(@AuthenticationPrincipal AuthenticatedUser user,
                                                                    @RequestParam(required = false) String name,
                                                                    @RequestParam(required = false) Boolean active,
                                                                    Pageable pageable) {
        return ApiResponse.ok(service.listCategories(user.getTenantId(), name, active, pageable));
    }

    @GetMapping("/categories/options")
    public ApiResponse<List<ServiceCategoryResponse>> categoryOptions(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.categoryOptions(user.getTenantId()));
    }

    @GetMapping("/categories/{id}")
    public ApiResponse<ServiceCategoryResponse> getCategory(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.getCategory(user.getTenantId(), id));
    }

    @PostMapping("/categories")
    public ApiResponse<ServiceCategoryResponse> createCategory(@AuthenticationPrincipal AuthenticatedUser user,
                                                              @Valid @RequestBody ServiceCategoryRequest request) {
        return ApiResponse.ok(service.createCategory(user.getTenantId(), request));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<ServiceCategoryResponse> updateCategory(@AuthenticationPrincipal AuthenticatedUser user,
                                                              @PathVariable UUID id,
                                                              @Valid @RequestBody ServiceCategoryRequest request) {
        return ApiResponse.ok(service.updateCategory(user.getTenantId(), id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        service.deleteCategory(user.getTenantId(), id);
        return ApiResponse.message("Categoria removida.");
    }

    @PatchMapping("/categories/{id}/activate")
    public ApiResponse<ServiceCategoryResponse> activateCategory(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.activateCategory(user.getTenantId(), id));
    }

    @PatchMapping("/categories/{id}/deactivate")
    public ApiResponse<ServiceCategoryResponse> deactivateCategory(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.ok(service.deactivateCategory(user.getTenantId(), id));
    }

    @GetMapping("/stats")
    public ApiResponse<ServiceCatalogStatsResponse> stats(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.stats(user.getTenantId()));
    }

    @GetMapping("/billing-units")
    public ApiResponse<BillingUnit[]> billingUnits() {
        return ApiResponse.ok(BillingUnit.values());
    }
}
