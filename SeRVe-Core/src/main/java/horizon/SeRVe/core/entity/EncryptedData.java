package horizon.SeRVe.core.entity;

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
    @JoinColumn(name = "task_id")
    private Task task;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] encryptedBlob; // 실제 암호화 데이터 (바이너리)

    @Version
    @Column(nullable = false)
    private int version;

    public void updateContent(byte[] newBlob) {
        this.encryptedBlob = newBlob;
    }
}
