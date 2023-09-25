package ca.bc.gov.educ.api.soam.exception;

import ca.bc.gov.educ.api.soam.exception.errors.ApiError;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.springframework.http.HttpStatus.*;

/**
 * The type Rest exception handler.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * The constant log.
   */
  private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

  /**
   * Handle http message not readable response entity.
   *
   * @param ex      the ex
   * @param headers the headers
   * @param status  the status
   * @param request the request
   * @return the response entity
   */
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(final HttpMessageNotReadableException ex, final HttpHeaders headers, final HttpStatusCode status, final WebRequest request) {
    val error = "Malformed JSON request";
    log.error("{} ", error, ex);
    return this.buildResponseEntity(new ApiError(BAD_REQUEST, error, ex));
  }

  /**
   * Build response entity response entity.
   *
   * @param apiError the api error
   * @return the response entity
   */
  private ResponseEntity<Object> buildResponseEntity(final ApiError apiError) {
    return new ResponseEntity<>(apiError, apiError.getStatus());
  }


  /**
   * Handles IllegalArgumentException
   *
   * @param ex the InvalidParameterException
   * @return the ApiError object
   */
  @ExceptionHandler(IllegalArgumentException.class)
  protected ResponseEntity<Object> handleInvalidParameter(final IllegalArgumentException ex) {
    val apiError = new ApiError(BAD_REQUEST);
    apiError.setMessage(ex.getMessage());
    log.error("{} ", apiError.getMessage(), ex);
    return this.buildResponseEntity(apiError);
  }

  /**
   * Handles InvalidParameterException
   *
   * @param ex the InvalidParameterException
   * @return the ApiError object
   */
  @ExceptionHandler(InvalidParameterException.class)
  protected ResponseEntity<Object> handleInvalidParameter(final InvalidParameterException ex) {
    val apiError = new ApiError(BAD_REQUEST);
    apiError.setMessage(ex.getMessage());
    log.error("{} ", apiError.getMessage(), ex);
    return this.buildResponseEntity(apiError);
  }

  /**
   * Handles RequestNotPermitted
   *
   * @param ex the RequestNotPermitted
   * @return the ApiError object
   */
  @ExceptionHandler(RequestNotPermitted.class)
  protected ResponseEntity<Object> handleRequestNotPermitted(final RequestNotPermitted ex) {
    val apiError = new ApiError(TOO_MANY_REQUESTS);
    apiError.setMessage(ex.getMessage());
    log.error("{} ", apiError.getMessage(), ex);
    return this.buildResponseEntity(apiError);
  }

  /**
   * Handles BulkheadFullException
   *
   * @param ex the BulkheadFullException
   * @return the ApiError object
   */
  @ExceptionHandler(BulkheadFullException.class)
  protected ResponseEntity<Object> handleBulkheadFullException(final BulkheadFullException ex) {
    val apiError = new ApiError(BAD_GATEWAY);
    apiError.setMessage(ex.getMessage());
    log.error("{} ", apiError.getMessage(), ex);
    return this.buildResponseEntity(apiError);
  }

  /**
   * Handles CallNotPermittedException
   *
   * @param ex the CallNotPermittedException
   * @return the ApiError object
   */
  @ExceptionHandler(CallNotPermittedException.class)
  protected ResponseEntity<Object> handleCallNotPermittedException(final CallNotPermittedException ex) {
    val apiError = new ApiError(BAD_GATEWAY);
    apiError.setMessage(ex.getMessage());
    log.error("{} ", apiError.getMessage(), ex);
    return this.buildResponseEntity(apiError);
  }

  /**
   * Handles MethodArgumentNotValidException. Triggered when an object fails @Valid validation.
   *
   * @param ex      the MethodArgumentNotValidException that is thrown when @Valid validation fails
   * @param headers HttpHeaders
   * @param status  HttpStatusCode
   * @param request WebRequest
   * @return the ApiError object
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
    final MethodArgumentNotValidException ex,
    final HttpHeaders headers,
    final HttpStatusCode status,
    final WebRequest request) {
    val apiError = new ApiError(BAD_REQUEST);
    apiError.setMessage("Validation error");
    apiError.addValidationErrors(ex.getBindingResult().getFieldErrors());
    apiError.addValidationError(ex.getBindingResult().getGlobalErrors());
    log.error("{} ", apiError.getMessage(), ex);
    return this.buildResponseEntity(apiError);
  }


}
