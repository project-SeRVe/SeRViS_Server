package horizon.SeRVe.core.service;

import horizon.SeRVe.common.dto.feign.MemberRoleResponse;
import horizon.SeRVe.core.dto.document.DocumentResponse;
import horizon.SeRVe.core.dto.document.EncryptedDataResponse;
import horizon.SeRVe.core.dto.document.UploadDocumentRequest;
import horizon.SeRVe.core.entity.*;
import horizon.SeRVe.core.feign.AuthServiceClient;
import horizon.SeRVe.core.feign.TeamServiceClient;
import horizon.SeRVe.core.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @InjectMocks
    private DocumentService documentService;

    @Mock private DocumentRepository documentRepository;
    @Mock private EncryptedDataRepository encryptedDataRepository;
    @Mock private VectorChunkRepository vectorChunkRepository;
    @Mock private TeamServiceClient teamServiceClient;
    @Mock private AuthServiceClient authServiceClient;

    @Test
    @DisplayName("문서 업로드 성공 테스트")
    void uploadDocument_Success() {
        // given
        String teamId = "team-1";
        String userId = "user-1";
        String sampleBase64 = Base64.getEncoder().encodeToString("test-content".getBytes());
        UploadDocumentRequest request = new UploadDocumentRequest("test.pdf", "pdf", sampleBase64);

        MemberRoleResponse memberRole = MemberRoleResponse.builder()
                .userId(userId).teamId(teamId).role("ADMIN").build();

        given(teamServiceClient.teamExists(teamId)).willReturn(true);
        given(teamServiceClient.getMemberRole(teamId, userId)).willReturn(memberRole);

        // when
        documentService.uploadDocument(teamId, userId, request);

        // then
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    @DisplayName("문서 목록 조회 성공 테스트")
    void getDocuments_Success() {
        // given
        String teamId = "team-1";
        String userId = "user-1";

        Document doc1 = Document.builder()
                .documentId("doc-1")
                .originalFileName("file1.pdf")
                .fileType("pdf")
                .uploaderId("uploader-1")
                .build();

        given(teamServiceClient.teamExists(teamId)).willReturn(true);
        given(teamServiceClient.memberExists(teamId, userId)).willReturn(true);
        given(documentRepository.findAllByTeamId(teamId)).willReturn(List.of(doc1));

        // when
        List<DocumentResponse> result = documentService.getDocuments(teamId, userId);

        // then
        assertEquals(1, result.size());
        assertEquals("file1.pdf", result.get(0).getFileName());
        assertEquals("uploader-1", result.get(0).getUploaderId());
    }

    @Test
    @DisplayName("데이터 다운로드 성공 - 멤버일 경우")
    void getData_Success() {
        // given
        String docId = "doc-1";
        String userId = "user-1";

        Document mockDoc = Document.builder()
                .documentId(docId)
                .teamId("team-1")
                .build();

        EncryptedData mockData = EncryptedData.builder()
                .dataId("data-1")
                .document(mockDoc)
                .encryptedBlob("secret".getBytes())
                .build();

        given(documentRepository.findByDocumentId(docId)).willReturn(Optional.of(mockDoc));
        given(authServiceClient.userExists(userId)).willReturn(true);
        given(teamServiceClient.memberExists("team-1", userId)).willReturn(true);
        given(encryptedDataRepository.findByDocument(mockDoc)).willReturn(Optional.of(mockData));

        // when
        EncryptedDataResponse response = documentService.getData(docId, userId);

        // then
        assertNotNull(response);
        assertArrayEquals("secret".getBytes(), response.getContent());
    }

    @Test
    @DisplayName("데이터 다운로드 실패 - 멤버가 아닐 경우")
    void getData_Fail_NotMember() {
        // given
        String docId = "doc-1";
        String userId = "intruder";

        Document mockDoc = Document.builder()
                .documentId(docId)
                .teamId("team-1")
                .build();

        given(documentRepository.findByDocumentId(docId)).willReturn(Optional.of(mockDoc));
        given(authServiceClient.userExists(userId)).willReturn(true);
        given(teamServiceClient.memberExists("team-1", userId)).willReturn(false);

        // when & then
        assertThrows(SecurityException.class, () -> {
            documentService.getData(docId, userId);
        });
    }
}
