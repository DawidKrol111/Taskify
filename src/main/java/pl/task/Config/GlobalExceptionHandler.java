package pl.task.Config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import pl.task.Controllers.UserController;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserController.UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<?> handleUnauthorizedException(UserController.UnauthorizedException ex) {
        return new ResponseEntity<>(Map.of("message", "Błąd podczas przypisywania użytkownika"), HttpStatus.UNAUTHORIZED);

    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<?> handleGenericException(Exception ex) {

        return new ResponseEntity<>(Map.of("message", "Wystąpił nieoczekiwany błąd"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
