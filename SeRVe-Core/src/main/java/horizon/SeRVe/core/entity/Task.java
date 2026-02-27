package horizon.SeRVe.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB PK

    @Column(unique = true, nullable = false)
    private String taskId; // 외부 식별용 UUID

    @Column(name = "team_id")
    private String teamId;

    @Column(name = "uploader_id")
    private String uploaderId;

    private String originalFileName;

    private String fileType;

    // 메타데이터와 실제 데이터 분리 (1:1 관계)
    @OneToOne(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private EncryptedData encryptedData;

    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        this.uploadedAt = LocalDateTime.now();
    }

    public void setEncryptedData(EncryptedData encryptedData) {
        this.encryptedData = encryptedData;
    }
}
