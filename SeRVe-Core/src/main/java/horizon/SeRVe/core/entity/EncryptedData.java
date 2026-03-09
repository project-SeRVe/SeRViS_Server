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

    @Column(nullable = false, length = 500)
    private String objectKey; // S3 key (바이너리는 S3에 저장)

    @Version
    @Column(nullable = false)
    private int version;

    public void updateObjectKey(String newKey) {
        this.objectKey = newKey;
    }
}
