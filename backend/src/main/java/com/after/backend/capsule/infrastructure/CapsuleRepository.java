package com.after.backend.capsule.infrastructure;

import com.after.backend.capsule.domain.Capsule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CapsuleRepository
        extends JpaRepository<Capsule, UUID> {
}