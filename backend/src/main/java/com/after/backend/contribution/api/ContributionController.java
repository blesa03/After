package com.after.backend.contribution.api;

import com.after.backend.auth.application.AuthService;
import com.after.backend.contribution.api.dto.ContributionResponse;
import com.after.backend.contribution.api.dto.CreateTextContributionRequest;
import com.after.backend.contribution.api.dto.UpdateTextContributionRequest;
import com.after.backend.contribution.application.ContributionService;
import com.after.backend.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/capsules/{capsuleId}/contributions"
)
public class ContributionController {

    private final ContributionService
            contributionService;

    private final AuthService authService;

    public ContributionController(
            ContributionService contributionService,
            AuthService authService
    ) {
        this.contributionService =
                contributionService;

        this.authService =
                authService;
    }

    @PostMapping("/text")
    public ResponseEntity<ContributionResponse>
    createText(
            @PathVariable UUID capsuleId,
            @Valid
            @RequestBody
            CreateTextContributionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User currentUser =
                authService.currentUser(
                        jwt.getSubject()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        contributionService
                                .createText(
                                        currentUser,
                                        capsuleId,
                                        request
                                )
                );
    }

    @GetMapping("/text")
    public List<ContributionResponse>
    listOwnText(
            @PathVariable UUID capsuleId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User currentUser =
                authService.currentUser(
                        jwt.getSubject()
                );

        return contributionService
                .listOwnText(
                        currentUser,
                        capsuleId
                );
    }

    @PutMapping("/{contributionId}/text")
    public ContributionResponse updateText(
            @PathVariable UUID capsuleId,
            @PathVariable UUID contributionId,
            @Valid
            @RequestBody
            UpdateTextContributionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User currentUser =
                authService.currentUser(
                        jwt.getSubject()
                );

        return contributionService
                .updateText(
                        currentUser,
                        capsuleId,
                        contributionId,
                        request
                );
    }

    @DeleteMapping("/{contributionId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID capsuleId,
            @PathVariable UUID contributionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User currentUser =
                authService.currentUser(
                        jwt.getSubject()
                );

        contributionService.delete(
                currentUser,
                capsuleId,
                contributionId
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}