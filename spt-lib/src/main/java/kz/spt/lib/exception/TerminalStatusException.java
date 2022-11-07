package kz.spt.lib.exception;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpException;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class TerminalStatusException extends HttpException {
    private final HttpStatus status;
    private final String message;

    public TerminalStatusException(
            HttpStatus status,
            String message) {
        super(message);
        this.status = status;
        this.message = message;
    }

}
