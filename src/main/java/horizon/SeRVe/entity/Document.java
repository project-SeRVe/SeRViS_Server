package horizon.SeRVe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB PK

    @Column(unique = true, nullable = false)
    private String documentId; // 외부 식별용 UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id")
    private TeamRepository teamRepository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader; // 계획대로 User 객체 매핑

    private String originalFileName;

    private String fileType;

    // 메타데이터와 실제 데이터 분리 (1:1 관계)
    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
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