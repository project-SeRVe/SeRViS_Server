package horizon.SeRVe.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // "이미 가입된 이메일입니다" 같은 IllegalArgumentException을 잡아서 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        // 1. 에러 메시지 가져오기 (예: "이미 가입된 이메일입니다.")
        String message = e.getMessage();
        
        // 2. 파이썬 클라이언트가 알아들을 수 있게 영어 키워드 추가
        if (message.contains("이미 가입된")) {
            message += " (Error: Account already exists)"; 
        }

        // 3. 500 대신 409 Conflict(충돌) 또는 400 Bad Request로 응답
        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }
}