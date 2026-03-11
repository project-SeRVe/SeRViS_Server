package horizon.SeRVe.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3StorageService(S3Client s3Client,
                            S3Presigner s3Presigner,
                            @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    // 바이너리 업로드 → objectKey 반환
    public String upload(String objectKey, byte[] data) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(data));
        return objectKey;
    }

    // objectKey로 바이너리 다운로드
    public byte[] download(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    // objectKey로 S3 오브젝트 삭제
    public void delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.deleteObject(request);
    }

    // Presigned URL 발급 (기본 15분 유효)
    public String generatePresignedUrl(String objectKey) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build())
                .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }

    // objectKey 생성 헬퍼
    // 형식: {teamId}/{entityId}/{kind}/{filename}
    public String generateObjectKey(String teamId, String entityId, String kind, String filename) {
        return teamId + "/" + entityId + "/" + kind + "/" + filename;
    }
}
