package com.after.backend.capsule.api;

import com.after.backend.auth.application.AuthService;
import com.after.backend.capsule.api.dto.InvitationAcceptedResponse;
import com.after.backend.capsule.api.dto.InvitationCreatedResponse;
import com.after.backend.capsule.api.dto.InvitationResponse;
import com.after.backend.capsule.application.InvitationService;
import com.after.backend.user.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class InvitationController {

    private final InvitationService invitationService;
    private final AuthService authService;

    public InvitationController(
            InvitationService invitationService,
            AuthService authService
    ) {
        this.invitationService = invitationService;
        this.authService = authService;
    }

    @PostMapping(
            "/capsules/{capsuleId}/invitations"
    )
    public ResponseEntity<InvitationCreatedResponse>
    create(
            @PathVariable UUID capsuleId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User currentUser =
                authService.currentUser(jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        invitationService.create(
                                currentUser,
                                capsuleId
                        )
                );
    }

    @GetMapping("/invitations/{token}")
    public InvitationResponse get(
            @PathVariable String token
    ) {
        return invitationService.get(token);
    }

    @PostMapping("/invitations/{token}/accept")
    public InvitationAcceptedResponse accept(
            @PathVariable String token,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User currentUser =
                authService.currentUser(jwt.getSubject());

        return invitationService.accept(
                currentUser,
                token
        );
    }
}