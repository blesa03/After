package com.after.backend.capsule.api;

import com.after.backend.auth.application.AuthService;
import com.after.backend.capsule.api.dto.CapsuleDetailResponse;
import com.after.backend.capsule.api.dto.CapsuleSummaryResponse;
import com.after.backend.capsule.api.dto.CreateCapsuleRequest;
import com.after.backend.capsule.application.CapsuleService;
import com.after.backend.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/capsules")
public class CapsuleController {

    private final CapsuleService capsuleService;
    private final AuthService authService;

    public CapsuleController(
            CapsuleService capsuleService,
            AuthService authService
    ) {
        this.capsuleService = capsuleService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<CapsuleDetailResponse> create(
            @Valid @RequestBody CreateCapsuleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User currentUser =
                authService.currentUser(jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        capsuleService.create(
                                currentUser,
                                request
                        )
                );
    }

    @GetMapping
    public List<CapsuleSummaryResponse> list(
            @AuthenticationPrincipal Jwt jwt
    ) {
        User currentUser =
                authService.currentUser(jwt.getSubject());

        return capsuleService.listFor(currentUser);
    }

    @GetMapping("/{capsuleId}")
    public CapsuleDetailResponse get(
            @PathVariable UUID capsuleId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User currentUser =
                authService.currentUser(jwt.getSubject());

        return capsuleService.getFor(
                currentUser,
                capsuleId
        );
    }
}