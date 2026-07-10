package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.ClientDtos.PortalPublicResponse;
import com.arqly.backend.service.ClientService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/portal", "/portal"})
public class PortalController {
    private final ClientService service;

    public PortalController(ClientService service) {
        this.service = service;
    }

    @GetMapping("/{token}")
    public ApiResponse<PortalPublicResponse> portal(@PathVariable UUID token) {
        return ApiResponse.ok(service.portal(token));
    }
}
