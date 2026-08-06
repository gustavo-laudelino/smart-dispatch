package br.com.smartdispatch.exception;

import br.com.smartdispatch.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> tratarResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatusCode statusCode = exception.getStatusCode();

        String erro = statusCode.toString();

        if (statusCode instanceof HttpStatus httpStatus) {
            erro = httpStatus.getReasonPhrase();
        }

        ErroResponse resposta = new ErroResponse(
                LocalDateTime.now(),
                statusCode.value(),
                erro,
                exception.getReason(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(statusCode)
                .body(resposta);
    }
}