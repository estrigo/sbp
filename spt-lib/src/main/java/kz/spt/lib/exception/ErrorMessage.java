package kz.spt.lib.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
public class ErrorMessage {
    private final LocalDateTime timestamp;
    private final String message;
}
