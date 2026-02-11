package horizon.SeRVe.core.dto.document;

import horizon.SeRVe.core.entity.Document;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DocumentResponse {
    private Long id;
    private String fileName;
    private String fileType;
    private String uploaderId;
    private LocalDateTime createdAt;

    public static DocumentResponse from(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getOriginalFileName())
                .fileType(document.getFileType())
                .uploaderId(document.getUploaderId())
                .createdAt(document.getUploadedAt())
                .build();
    }
}
