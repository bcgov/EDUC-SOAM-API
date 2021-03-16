package ca.bc.gov.educ.api.soam.rest;

import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.model.entity.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.model.entity.StudentEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.util.SoamUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@Component
@Slf4j
public class RestUtils {

  public static final String NULL_BODY_FROM = "null body from ";
  private final WebClient webClient;
  private final ApplicationProperties props;
  private final SoamUtil soamUtil;

  public RestUtils(final WebClient webClient, final ApplicationProperties props, final SoamUtil soamUtil) {
    this.webClient = webClient;
    this.props = props;
    this.soamUtil = soamUtil;
  }

  public Optional<DigitalIDEntity> getDigitalID(@NonNull final String identifierType, @NonNull final String identifierValue) {
    try {
      val response = this.webClient.get()
          .uri(this.props.getDigitalIdentifierApiURL(),
              uri -> uri.queryParam("identitytype", identifierType)
                  .queryParam("identityvalue", identifierValue.toUpperCase())
                  .build())
          .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .retrieve()
          .bodyToMono(DigitalIDEntity.class)
          .doOnError(error -> this.logError(error, identifierType, identifierValue))
          .doOnSuccess(entity -> {
            if (entity != null) {
              this.logSuccess(entity.toString(), identifierType, identifierValue);
            }
          })
          .block();
      if (response == null) {
        throw new SoamRuntimeException(this.getErrorMessageString(HttpStatus.INTERNAL_SERVER_ERROR, NULL_BODY_FROM +
            "digitalID get call."));
      }
      return Optional.of(response);
    } catch (final WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        return Optional.empty();
      } else {
        throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
  }

  private void logError(final Throwable error, final String... args) {
    log.error("Error from API call :: {} ", args, error);
  }

  private void logSuccess(final String s, final String... args) {
    log.info("API call success :: {} {}", s, args);
  }

  public Optional<ServicesCardEntity> getServicesCard(@NonNull final String did) {
    try {
      val response = this.webClient.get()
          .uri(this.props.getServicesCardApiURL(), uri -> uri
              .queryParam("did", did)
              .build())
          .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .retrieve()
          .bodyToMono(ServicesCardEntity.class)
          .doOnError(error -> this.logError(error, did))
          .doOnSuccess(entity -> {
            if (entity != null) {
              this.logSuccess(entity.toString(), did);
            }
          })
          .block();
      if (response == null) {
        throw new SoamRuntimeException(this.getErrorMessageString(HttpStatus.INTERNAL_SERVER_ERROR, NULL_BODY_FROM +
            "Services card get call."));
      }
      return Optional.of(response);
    } catch (final WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        return Optional.empty();
      } else {
        throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
  }

  public void updateServicesCard(final ServicesCardEntity servicesCardEntity) {
    try {
      servicesCardEntity.setCreateDate(null);
      servicesCardEntity.setUpdateDate(null);
      this.webClient.put()
          .uri(this.props.getServicesCardApiURL())
          .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .body(Mono.just(servicesCardEntity), ServicesCardEntity.class)
          .retrieve()
          .bodyToMono(ServicesCardEntity.class)
          .doOnError(this::logError)
          .doOnSuccess(entity -> {
            if (entity != null) {
              this.logSuccess(entity.toString());
            }
          })
          .block();
    } catch (final WebClientResponseException e) {
      throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
    }
  }

  public void updateDigitalID(final DigitalIDEntity digitalIDEntity) {
    try {
      this.webClient.put()
          .uri(this.props.getDigitalIdentifierApiURL())
          .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .body(Mono.just(digitalIDEntity), DigitalIDEntity.class)
          .retrieve()
          .bodyToMono(DigitalIDEntity.class)
          .doOnError(this::logError)
          .doOnSuccess(entity -> {
            if (entity != null) {
              this.logSuccess(entity.toString());
            }
          })
          .block();
    } catch (final WebClientResponseException e) {
      throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
    }
  }

  public DigitalIDEntity createDigitalID(@NonNull final String identifierType, @NonNull final String identifierValue) {
    final DigitalIDEntity entity = this.soamUtil.createDigitalIdentity(identifierType, identifierValue.toUpperCase());
    try {
      val response = this.webClient.post()
          .uri(this.props.getDigitalIdentifierApiURL())
          .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .body(Mono.just(entity), DigitalIDEntity.class)
          .retrieve()
          .bodyToMono(DigitalIDEntity.class)
          .doOnError(error -> this.logError(error, identifierType, identifierValue))
          .doOnSuccess(responseEntity -> {
            if (responseEntity != null) {
              this.logSuccess(responseEntity.toString(), identifierType, identifierValue);
            }
          })
          .block();
      if (response == null) {
        throw new SoamRuntimeException(this.getErrorMessageString(HttpStatus.INTERNAL_SERVER_ERROR, NULL_BODY_FROM +
            "digitalID post call."));
      }
      return response;
    } catch (final WebClientResponseException e) {
      throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
    }
  }

  public void createServicesCard(@NonNull final ServicesCardEntity servicesCardEntity) {
    try {
      this.webClient.post()
          .uri(this.props.getServicesCardApiURL())
          .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .body(Mono.just(servicesCardEntity), ServicesCardEntity.class)
          .retrieve()
          .bodyToMono(ServicesCardEntity.class)
          .doOnError(this::logError)
          .doOnSuccess(responseEntity -> {
            if (responseEntity != null) {
              this.logSuccess(responseEntity.toString());
            }
          })
          .block();
    } catch (final WebClientResponseException e) {
      throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
    }
  }

  public StudentEntity getStudentByStudentID(final String studentID) {
    try {
      val apiResponse = this.webClient.get()
          .uri(this.props.getStudentApiURL(), uri -> uri.path("/{studentID}")
              .build(studentID))
          .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .retrieve()
          .bodyToMono(StudentEntity.class)
          .doOnError(error -> this.logError(error, studentID))
          .doOnSuccess(responseEntity -> {
            if (responseEntity != null) {
              this.logSuccess(responseEntity.toString(), studentID);
            }
          })
          .block();
      if (apiResponse == null) {
        throw new SoamRuntimeException(this.getErrorMessageString(HttpStatus.INTERNAL_SERVER_ERROR, NULL_BODY_FROM +
            "student get call."));
      }
      return apiResponse;
    } catch (final WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        throw new SoamRuntimeException("Student was not found. URL was: " + this.props.getStudentApiURL() + "/" + studentID);
      } else {
        throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
  }


  private String getErrorMessageString(final HttpStatus status, final String body) {
    return "Unexpected HTTP return code: " + status + " error message: " + body;
  }
}
