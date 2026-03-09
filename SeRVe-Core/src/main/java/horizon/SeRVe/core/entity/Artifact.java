package horizon.SeRVe.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "artifacts", indexes = {
    @Index(name = "idx_artifacts_demo", columnList = "demo_id"),
    @Index(name = "idx_artifacts_object_key", columnList = "object_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Artifact {

    @Id
    @Column(name = "artifact_id", length = 64)
    private String artifactId; // UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demo_id", nullable = false)
    private Demo demo;

    // "processed" 또는 "raw"
    @Builder.Default
    @Column(name = "kind", nullable = false, length = 20)
    private String kind = "processed";

    // S3 key: {teamId}/{scenarioId}/{demoId}/processed_demo_v1.npz.enc
    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "size")
    private Long size;

    @Column(name = "version", length = 20)
    private String version;

    // 암호화 메타
    @Column(name = "enc_algo", length = 50)
    private String encAlgo;

    @Column(name = "nonce", length = 100)
    private String nonce;

    @Column(name = "dek_wrapped_by_kek", length = 500)
    private String dekWrappedByKek;

    @Column(name = "kek_version", length = 20)
    private String kekVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
