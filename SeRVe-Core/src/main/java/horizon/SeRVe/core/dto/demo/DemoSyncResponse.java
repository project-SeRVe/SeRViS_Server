package horizon.SeRVe.core.dto.demo;

import horizon.SeRVe.core.entity.VectorDemo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DemoSyncResponse {
    private String taskId;
    private String demoId;
    private int demoIndex;
    private byte[] encryptedBlob;
    private int version;
    private boolean isDeleted;
    private String createdBy;

    public static DemoSyncResponse from(VectorDemo demo) {
        return DemoSyncResponse.builder()
                .taskId(demo.getTaskId())
                .demoId(demo.getDemoId())
                .demoIndex(demo.getDemoIndex())
                .encryptedBlob(demo.getEncryptedBlob())
                .version(demo.getVersion())
                .isDeleted(demo.isDeleted())
                .createdBy(null)
                .build();
    }

    public static DemoSyncResponse from(VectorDemo demo, String createdBy) {
        return DemoSyncResponse.builder()
                .taskId(demo.getTaskId())
                .demoId(demo.getDemoId())
                .demoIndex(demo.getDemoIndex())
                .encryptedBlob(demo.getEncryptedBlob())
                .version(demo.getVersion())
                .isDeleted(demo.isDeleted())
                .createdBy(createdBy)
                .build();
    }
}
