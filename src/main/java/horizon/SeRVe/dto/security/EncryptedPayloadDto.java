package horizon.SeRVe.dto.security;

import lombok.Data;

@Data
public class EncryptedPayloadDto {
    private String content; // Base64로 암호화된 데이터 본문
    private Long repositoryId; // 어느 저장소의 데이터인지
}