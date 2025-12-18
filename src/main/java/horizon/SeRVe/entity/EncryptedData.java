package horizon.SeRVe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "encrypted_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EncryptedData {

    @Id
    private String dataId; // UUID

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] encryptedBlob; // 실제 암호화 데이터 (바이너리)

    @Column(nullable = false)
    private int version;
}