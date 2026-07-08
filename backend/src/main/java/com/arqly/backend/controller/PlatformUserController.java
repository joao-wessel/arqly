package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.PlatformUserDtos.PlatformUserRequest;
import com.arqly.backend.dto.PlatformUserDtos.PlatformUserResponse;
import com.arqly.backend.service.PlatformUserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/users")
public class PlatformUserController {
    private final PlatformUserService service;

    public PlatformUserController(PlatformUserService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Page<PlatformUserResponse>> list(Pageable pageable) {
        return ApiResponse.ok(service.list(pageable));
    }

    @PostMapping
    public ApiResponse<PlatformUserResponse> create(@Valid @RequestBody PlatformUserRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PlatformUserResponse> update(@PathVariable UUID id, @Valid @RequestBody PlatformUserRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.message("Administrador excluído.");
    }
}
