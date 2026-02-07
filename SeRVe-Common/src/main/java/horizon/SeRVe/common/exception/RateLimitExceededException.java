package horizon.SeRVe.common.exception;

/**
 * Rate Limit 초과 시 발생하는 예외
 * HTTP 429 Too Many Requests 반환용
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
