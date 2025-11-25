package horizon.SeRVe.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter @Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 저장소(금고)에 들어있는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id")
    private TeamRepository teamRepository;

    private String originalFileName; // 원래 파일 이름 (예: design_v1.pdf)

    @Lob // 대용량 데이터 저장용
    @Column(columnDefinition = "TEXT")
    private String encryptedContent; // ★핵심: 암호화된 내용 (Base64)

    private String uploadedBy; // 올린 사람

    private LocalDateTime uploadedAt = LocalDateTime.now();

    public Document(TeamRepository teamRepository, String originalFileName, String encryptedContent, String uploadedBy) {
        this.teamRepository = teamRepository;
        this.originalFileName = originalFileName;
        this.encryptedContent = encryptedContent;
        this.uploadedBy = uploadedBy;
    }
}