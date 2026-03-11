package horizon.SeRVe.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        // EKS에서는 IRSA(IAM Roles for Service Accounts)로 자동 인증
        // 로컬에서는 ~/.aws/credentials 또는 환경변수 AWS_ACCESS_KEY_ID/SECRET_ACCESS_KEY 사용
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

}
