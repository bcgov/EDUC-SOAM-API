package ca.bc.gov.educ.api.soam.exception.errors;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Api error.
 */
@AllArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("squid:S1948")
public class ApiError implements Serializable {

  /**
   * The Status.
   */
  private HttpStatus status;
  /**
   * The Timestamp.
   */
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
  private LocalDateTime timestamp;
  /**
   * The Message.
   */
  private String message;
  /**
   * The Debug message.
   */
  private String debugMessage;
  /**
   * The Sub errors.
   */
  private List<ApiSubError> subErrors;

  /**
   * Instantiates a new Api error.
   */
  private ApiError() {
    this.timestamp = LocalDateTime.now();
  }

  /**
   * Instantiates a new Api error.
   *
   * @param status the status
   */
  public ApiError(final HttpStatus status) {
    this();
    this.status = status;
  }

  /**
   * Instantiates a new Api error.
   *
   * @param status the status
   * @param ex     the ex
   */
  ApiError(final HttpStatus status, final Throwable ex) {
    this();
    this.status = status;
    this.message = "Unexpected error";
    this.debugMessage = ex.getLocalizedMessage();
  }

  /**
   * Instantiates a new Api error.
   *
   * @param status  the status
   * @param message the message
   * @param ex      the ex
   */
  public ApiError(final HttpStatus status, final String message, final Throwable ex) {
    this();
    this.status = status;
    this.message = message;
    this.debugMessage = ex.getLocalizedMessage();
  }

  /**
   * Add sub error.
   *
   * @param subError the sub error
   */
  private void addSubError(final ApiSubError subError) {
    if (this.subErrors == null) {
      this.subErrors = new ArrayList<>();
    }
    this.subErrors.add(subError);
  }

  /**
   * Add validation error.
   *
   * @param object        the object
   * @param field         the field
   * @param rejectedValue the rejected value
   * @param message       the message
   */
  private void addValidationError(final String object, final String field, final Object rejectedValue, final String message) {
    this.addSubError(new ApiValidationError(object, field, rejectedValue, message));
  }

  /**
   * Add validation error.
   *
   * @param object  the object
   * @param message the message
   */
  private void addValidationError(final String object, final String message) {
    this.addSubError(new ApiValidationError(object, message));
  }

  /**
   * Add validation error.
   *
   * @param fieldError the field error
   */
  private void addValidationError(final FieldError fieldError) {
    this.addValidationError(fieldError.getObjectName(), fieldError.getField(), fieldError.getRejectedValue(),
      fieldError.getDefaultMessage());
  }

  /**
   * Add validation errors.
   *
   * @param fieldErrors the field errors
   */
  public void addValidationErrors(final List<FieldError> fieldErrors) {
    fieldErrors.forEach(this::addValidationError);
  }

  /**
   * Add validation error.
   *
   * @param objectError the object error
   */
  private void addValidationError(final ObjectError objectError) {
    this.addValidationError(objectError.getObjectName(), objectError.getDefaultMessage());
  }

  /**
   * Add validation error.
   *
   * @param globalErrors the global errors
   */
  public void addValidationError(final List<ObjectError> globalErrors) {
    globalErrors.forEach(this::addValidationError);
  }


  /**
   * Gets status.
   *
   * @return the status
   */
  public HttpStatus getStatus() {
    return this.status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(final HttpStatus status) {
    this.status = status;
  }

  /**
   * Gets timestamp.
   *
   * @return the timestamp
   */
  public LocalDateTime getTimestamp() {
    return this.timestamp;
  }

  /**
   * Sets timestamp.
   *
   * @param timestamp the timestamp
   */
  public void setTimestamp(final LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Gets message.
   *
   * @return the message
   */
  public String getMessage() {
    return this.message;
  }

  /**
   * Sets message.
   *
   * @param message the message
   */
  public void setMessage(final String message) {
    this.message = message;
  }

  /**
   * Gets debug message.
   *
   * @return the debug message
   */
  public String getDebugMessage() {
    return this.debugMessage;
  }

  /**
   * Sets debug message.
   *
   * @param debugMessage the debug message
   */
  public void setDebugMessage(final String debugMessage) {
    this.debugMessage = debugMessage;
  }

  /**
   * Gets sub errors.
   *
   * @return the sub errors
   */
  public List<ApiSubError> getSubErrors() {
    return this.subErrors;
  }

  /**
   * Sets sub errors.
   *
   * @param subErrors the sub errors
   */
  public void setSubErrors(final List<ApiSubError> subErrors) {
    this.subErrors = subErrors;
  }

}
