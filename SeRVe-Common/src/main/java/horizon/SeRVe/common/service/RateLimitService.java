package horizon.SeRVe.common.service;

import horizon.SeRVe.common.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Service for Member Upload Protection
 *
 * 보안 목적:
 * - Member의 악의적인 대량 업로드 방지
 * - 클라우드 스토리지 비용 폭탄 방지
 * - 서버 리소스 보호
 *
 * 현재 정책: 1시간당 100회 업로드 제한
 */
@Slf4j
@Service
public class RateLimitService {

    // Key: userId, Value: List of upload timestamps
    private final Map<String, List<LocalDateTime>> uploadHistory = new ConcurrentHashMap<>();

    // Rate limit configuration
    private static final int MAX_UPLOADS_PER_HOUR = 100;
    private static final int TIME_WINDOW_HOURS = 1;

    /**
     * 업로드 허용 여부 확인 및 기록
     *
     * @param userId 사용자 ID
     * @throws RateLimitExceededException 업로드 제한 초과 시
     */
    public void checkAndRecordUpload(String userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusHours(TIME_WINDOW_HOURS);

        // 1. 해당 사용자의 업로드 기록 가져오기 (없으면 새로 생성)
        List<LocalDateTime> userUploads = uploadHistory.computeIfAbsent(userId, k -> new ArrayList<>());

        // 2. 시간 윈도우 밖의 오래된 기록 제거 (메모리 절약)
        synchronized (userUploads) {
            userUploads.removeIf(timestamp -> timestamp.isBefore(windowStart));

            // 3. Rate Limit 체크
            if (userUploads.size() >= MAX_UPLOADS_PER_HOUR) {
                log.warn("Rate limit exceeded for user: {}. Uploads in last hour: {}", userId, userUploads.size());
                throw new RateLimitExceededException(
                    String.format("업로드 제한 초과: 1시간당 최대 %d회까지 업로드 가능합니다. 잠시 후 다시 시도해주세요.",
                            MAX_UPLOADS_PER_HOUR)
                );
            }

            // 4. 현재 업로드 기록 추가
            userUploads.add(now);
            log.debug("Upload recorded for user: {}. Total uploads in last hour: {}", userId, userUploads.size());
        }
    }

    /**
     * 특정 사용자의 업로드 기록 초기화 (관리자 용도)
     */
    public void resetUserLimit(String userId) {
        uploadHistory.remove(userId);
        log.info("Rate limit reset for user: {}", userId);
    }

    /**
     * 모든 사용자의 업로드 기록 초기화 (서버 재시작 시 자동 초기화됨)
     */
    public void resetAllLimits() {
        uploadHistory.clear();
        log.info("All rate limits reset");
    }

    /**
     * 특정 사용자의 현재 업로드 횟수 조회
     */
    public int getCurrentUploadCount(String userId) {
        LocalDateTime windowStart = LocalDateTime.now().minusHours(TIME_WINDOW_HOURS);
        List<LocalDateTime> userUploads = uploadHistory.get(userId);

        if (userUploads == null) {
            return 0;
        }

        synchronized (userUploads) {
            return (int) userUploads.stream()
                    .filter(timestamp -> timestamp.isAfter(windowStart))
                    .count();
        }
    }
}
