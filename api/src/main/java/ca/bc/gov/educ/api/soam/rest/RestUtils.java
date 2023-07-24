package ca.bc.gov.educ.api.soam.rest;

import ca.bc.gov.educ.api.soam.codetable.CodeTableUtils;
import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.model.entity.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.model.entity.StsLoginPrincipalEntity;
import ca.bc.gov.educ.api.soam.model.entity.StudentEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.struct.v1.penmatch.PenMatchResult;
import ca.bc.gov.educ.api.soam.struct.v1.penmatch.PenMatchStudent;
import ca.bc.gov.educ.api.soam.struct.v1.tenant.TenantAccess;
import ca.bc.gov.educ.api.soam.util.SoamUtil;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@Component
@Slf4j
public class RestUtils {

  public static final String DIGITAL_ID_GET_CALL = "digitalID get call.";
  public static final String NULL_BODY_FROM = "null body from ";
  private static final String CORRELATION_ID = "correlationID";
  private static final String DIGITAL_ID_API = "digitalIdApi";
  private static final String SERVICES_CARD_API = "servicesCardApi";
  private static final String STUDENT_API = "studentApi";
  private static final String STS_API = "stsAPI";

  private final WebClient webClient;
  private final ApplicationProperties props;
  private final SoamUtil soamUtil;

  public RestUtils(final WebClient webClient, final ApplicationProperties props, final SoamUtil soamUtil, final CodeTableUtils codeTableUtils) {
    this.webClient = webClient;
    this.props = props;
    this.soamUtil = soamUtil;
    codeTableUtils.init();
  }

  @Bulkhead(name = DIGITAL_ID_API)
  @CircuitBreaker(name = DIGITAL_ID_API)
  @Retry(name = DIGITAL_ID_API)
  public Optional<DigitalIDEntity> getDigitalID(@NonNull final String identifierType, @NonNull final String identifierValue, final String correlationID) {
    try {
      val response = this.webClient.get()
        .uri(this.props.getDigitalIdentifierApiURL(),
          uri -> uri.queryParam("identitytype", identifierType)
            .queryParam("identityvalue", identifierValue.toUpperCase())
            .build())
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header(CORRELATION_ID, correlationID)
        .retrieve()
        .bodyToMono(DigitalIDEntity.class)
        .doOnSuccess(entity -> {
          if (entity != null) {
            this.logSuccess(entity.toString(), identifierType, identifierValue.toUpperCase(), correlationID);
          }
        })
        .block();
      if (response == null) {
        throw new SoamRuntimeException(this.getErrorMessageString(HttpStatus.INTERNAL_SERVER_ERROR, NULL_BODY_FROM +
          DIGITAL_ID_GET_CALL));
      }
      return Optional.of(response);
    } catch (final WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        this.logNotFound(e.getStatusCode().toString(), identifierType, identifierValue.toUpperCase());
        return Optional.empty();
      } else {
        throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
  }

  @Bulkhead(name = DIGITAL_ID_API)
  @CircuitBreaker(name = DIGITAL_ID_API)
  @Retry(name = DIGITAL_ID_API)
  public List<DigitalIDEntity> getDigitalIDByStudentID(@NonNull final String studentID, final String correlationID) {
    try {
      val response = this.webClient.get()
        .uri(this.props.getDigitalIdentifierApiURL() + "/list",
          uri -> uri.queryParam("studentID", studentID)
            .build())
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header(CORRELATION_ID, correlationID)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<DigitalIDEntity>>() {})
        .doOnSuccess(entity -> {
          if (entity != null) {
            this.logSuccess(entity.toString(), studentID, correlationID);
          }
        })
        .block();
      if (response == null) {
        throw new SoamRuntimeException(this.getErrorMessageString(HttpStatus.INTERNAL_SERVER_ERROR, NULL_BODY_FROM +
          DIGITAL_ID_GET_CALL));
      }
      return response;
    } catch (final WebClientResponseException e) {
      throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
    }
  }

  @Bulkhead(name = DIGITAL_ID_API)
  @CircuitBreaker(name = DIGITAL_ID_API)
  @Retry(name = DIGITAL_ID_API)
  public Optional<TenantAccess> getTenantAccess(@NonNull final String clientID, @NonNull final String tenantID, final String correlationID) {
    try {
      val response = this.webClient.get()
        .uri(this.props.getDigitalIdentifierApiURL() + "/tenantAccess",
          uri -> uri.queryParam("clientID", clientID)
                  .queryParam("tenantID", tenantID)
            .build())
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header(CORRELATION_ID, correlationID)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<TenantAccess>() {})
        .doOnSuccess(entity -> {
          if (entity != null) {
            this.logSuccess(entity.toString(), clientID, tenantID, correlationID);
          }
        })
        .block();
      if (response == null) {
        throw new SoamRuntimeException(this.getErrorMessageString(HttpStatus.INTERNAL_SERVER_ERROR, NULL_BODY_FROM +
          DIGITAL_ID_GET_CALL));
      }
      return Optional.of(response);
    } catch (final WebClientResponseException e) {
      throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
    }
  }

  @Bulkhead(name = STS_API)
  @CircuitBreaker(name = STS_API)
  @Retry(name = STS_API)
  public Optional<StsLoginPrincipalEntity> getStsLoginPrincipal(@NonNull final String ssoGuid, final String correlationID) {
    try {
      return Optional.ofNullable(this.webClient.get()
        .uri(this.props.getStsApiURL() + "/",
          uri -> uri.path(ssoGuid).build())
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header(CORRELATION_ID, correlationID)
        .retrieve()
        .bodyToMono(StsLoginPrincipalEntity.class)
        .doOnSuccess(entity -> {
          if (entity != null) {
            this.logSuccess(entity.toString(), ssoGuid, correlationID);
          }
        }).block());
    } catch (final WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        this.logNotFound(e.getStatusCode().toString(), ssoGuid);
        return Optional.empty();
      } else {
        throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
  }

  @Bulkhead(name = DIGITAL_ID_API)
  @CircuitBreaker(name = DIGITAL_ID_API)
  @Retry(name = DIGITAL_ID_API)
  public Optional<DigitalIDEntity> getDigitalID(@NonNull final String digitalIdentityID, final String correlationID) {
    try {
      val response = this.webClient.get()
        .uri(this.props.getDigitalIdentifierApiURL(),
          uri -> uri.path(digitalIdentityID).build())
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header(CORRELATION_ID, correlationID)
        .retrieve()
        .bodyToMono(DigitalIDEntity.class)
        .doOnSuccess(entity -> {
          if (entity != null) {
            this.logSuccess(entity.toString(), digitalIdentityID, correlationID);
          }
        })
        .block();
      if (response == null) {
        throw new SoamRuntimeException(this.getErrorMessageString(HttpStatus.INTERNAL_SERVER_ERROR, NULL_BODY_FROM +
          DIGITAL_ID_GET_CALL));
      }
      return Optional.of(response);
    } catch (final WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        this.logNotFound(e.getStatusCode().toString(), digitalIdentityID);
        return Optional.empty();
      } else {
        throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
  }

  private void logSuccess(final String s, final String... args) {
    log.info("API call success :: {} {}", s, args);
  }

  private void logNotFound(final String s, final String... args) {
    log.info("Entity not found :: {} {}", s, args);
  }

  @Bulkhead(name = SERVICES_CARD_API)
  @CircuitBreaker(name = SERVICES_CARD_API)
  @Retry(name = SERVICES_CARD_API)
  public Optional<ServicesCardEntity> getServicesCard(@NonNull final String did, final String correlationID) {
    try {
      val response = this.webClient.get()
        .uri(this.props.getServicesCardApiURL(), uri -> uri
          .queryParam("did", did)
          .build())
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header(CORRELATION_ID, correlationID)
        .retrieve()
        .bodyToMono(ServicesCardEntity.class)
        .doOnSuccess(entity -> {
          if (entity != null) {
            this.logSuccess(entity.toString(), did, correlationID);
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
        this.logNotFound(e.getStatusCode().toString(), did);
        return Optional.empty();
      } else {
        throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
  }

  @Bulkhead(name = SERVICES_CARD_API)
  @CircuitBreaker(name = SERVICES_CARD_API)
  public void updateServicesCard(final ServicesCardEntity servicesCardEntity, final String correlationID) {
    try {
      servicesCardEntity.setCreateDate(null);
      servicesCardEntity.setUpdateDate(null);
      this.webClient.put()
        .uri(this.props.getServicesCardApiURL(), uri -> uri.path("/{id}").build(servicesCardEntity.getServicesCardInfoID()))
        .headers(headers -> {
          headers.add(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
          headers.add(CORRELATION_ID, correlationID);
        })
        .body(Mono.just(servicesCardEntity), ServicesCardEntity.class)
        .retrieve()
        .bodyToMono(ServicesCardEntity.class)
        .doOnSuccess(entity -> {
          if (entity != null) {
            this.logSuccess(entity.toString(), correlationID);
          }
        })
        .block();
    } catch (final WebClientResponseException e) {
      throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
    }
  }

  @Bulkhead(name = DIGITAL_ID_API)
  @CircuitBreaker(name = DIGITAL_ID_API)
  public void updateDigitalID(final DigitalIDEntity digitalIDEntity, final String correlationID) {
    val updatedDigitalID = this.soamUtil.getUpdatedDigitalId(digitalIDEntity);
    try {
      this.webClient.put()
        .uri(this.props.getDigitalIdentifierApiURL(), uriBuilder -> uriBuilder.path("/{id}").build(digitalIDEntity.getDigitalID()))
        .headers(headers -> {
          headers.add(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
          headers.add(CORRELATION_ID, correlationID);
        })
        .body(Mono.just(updatedDigitalID), DigitalIDEntity.class)
        .retrieve()
        .bodyToMono(DigitalIDEntity.class)
        .doOnSuccess(entity -> {
          if (entity != null) {
            this.logSuccess(entity.toString(), correlationID);
          }
        })
        .block();
    } catch (final WebClientResponseException e) {
      throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
    }
  }

  @Bulkhead(name = DIGITAL_ID_API)
  @CircuitBreaker(name = DIGITAL_ID_API)
  public DigitalIDEntity createDigitalID(@NonNull final String identifierType, @NonNull final String identifierValue, final String correlationID) {
    val entity = this.soamUtil.createDigitalIdentity(identifierType, identifierValue.toUpperCase());
    try {
      val response = this.webClient.post()
        .uri(this.props.getDigitalIdentifierApiURL())
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header(CORRELATION_ID, correlationID)
        .body(Mono.just(entity), DigitalIDEntity.class)
        .retrieve()
        .bodyToMono(DigitalIDEntity.class)
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

  public Optional<PenMatchResult> postToMatchAPI(PenMatchStudent request) {
    try {
      val response = this.webClient.post()
        .uri(this.props.getPenMatchApiURL())
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(Mono.just(request), PenMatchStudent.class)
        .retrieve()
        .bodyToMono(PenMatchResult.class)
        .doOnSuccess(entity -> {
          if (entity != null) {
            this.logSuccess(entity.toString());
          }
        })
        .block();
      if (response == null) {
        throw new SoamRuntimeException(this.getErrorMessageString(HttpStatus.INTERNAL_SERVER_ERROR, NULL_BODY_FROM +
          "pen match get call."));
      }
      return Optional.of(response);
    } catch (final WebClientResponseException e) {
      throw new SoamRuntimeException(this.getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
    }
  }

  @Bulkhead(name = SERVICES_CARD_API)
  @CircuitBreaker(name = SERVICES_CARD_API)
  public void createServicesCard(@NonNull final ServicesCardEntity servicesCardEntity, final String correlationID) {
    try {
      this.webClient.post()
        .uri(this.props.getServicesCardApiURL())
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header(CORRELATION_ID, correlationID)
        .body(Mono.just(servicesCardEntity), ServicesCardEntity.class)
        .retrieve()
        .bodyToMono(ServicesCardEntity.class)
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

  @Bulkhead(name = STUDENT_API)
  @CircuitBreaker(name = STUDENT_API)
  @Retry(name = STUDENT_API)
  public StudentEntity getStudentByStudentID(final String studentID, final String correlationID) {
    try {
      val apiResponse = this.webClient.get()
        .uri(this.props.getStudentApiURL(), uri -> uri.path("/{studentID}")
          .build(studentID))
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header(CORRELATION_ID, correlationID)
        .retrieve()
        .bodyToMono(StudentEntity.class)
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
        this.logNotFound(e.getStatusCode().toString(), studentID);
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
