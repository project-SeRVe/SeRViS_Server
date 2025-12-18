package horizon.SeRVe.dto.document;

import horizon.SeRVe.entity.Document;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DocumentResponse {
    private String docId;
    private String fileName;
    private String fileType;
    private String uploaderId;
    private LocalDateTime createdAt;

    public static DocumentResponse from(Document document) {
        return DocumentResponse.builder()
                .docId(document.getDocumentId())
                .fileName(document.getOriginalFileName())
                .fileType(document.getFileType())
                .uploaderId(document.getUploader().getUserId())
                .createdAt(document.getUploadedAt())
                .build();
    }
}