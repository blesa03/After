package com.after.mediaworker.processing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class MediaInfoRepository {

    private final JdbcTemplate jdbcTemplate;

    public MediaInfoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<MediaInfo> findByContributionId(UUID contributionId) {
        String sql = """
                SELECT 
                    c.type, 
                    m.original_object_key, 
                    m.original_filename
                FROM contributions c
                JOIN media_objects m ON c.id = m.contribution_id
                WHERE c.id = ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new MediaInfo(
                        contributionId,
                        rs.getString("type"),
                        rs.getString("original_object_key"),
                        rs.getString("original_filename")
                ),
                contributionId
        ).stream().findFirst();

        
    }

    public void updateMetadata(
            UUID contributionId,
            Integer width,
            Integer height,
            Long durationMs
    ) {
        jdbcTemplate.update(
                """
                UPDATE media_objects
                SET width = ?, height = ?, duration_ms = ?
                WHERE contribution_id = ?
                """,
                width,
                height,
                durationMs,
                contributionId
        );
    }
}