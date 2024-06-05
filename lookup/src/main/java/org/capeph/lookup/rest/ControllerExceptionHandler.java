package org.capeph.lookup.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springdoc.api.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ControllerExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorMessage> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            (error) -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });
    ObjectMapper mapper = new ObjectMapper();
    try {
      ErrorMessage message = new ErrorMessage(mapper.writeValueAsString(errors));
      return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
      return new ResponseEntity<>(
          new ErrorMessage(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @ExceptionHandler(value = {IllegalArgumentException.class})
  public ResponseEntity<ErrorMessage> handleControllerExceptions(Exception ex, WebRequest request) {
    ErrorMessage message = new ErrorMessage(ex.getMessage());
    return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
  }
}
