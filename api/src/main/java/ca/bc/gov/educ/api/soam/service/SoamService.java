package ca.bc.gov.educ.api.soam.service;

import ca.bc.gov.educ.api.soam.codetable.CodeTableUtils;
import ca.bc.gov.educ.api.soam.exception.InvalidParameterException;
import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.model.entity.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.model.entity.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.model.entity.StudentEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.util.SoamUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@Service
@Slf4j
public class SoamService {
  private final ApplicationProperties props;

  private final CodeTableUtils codeTableUtils;


  private final SoamUtil soamUtil;

  private final WebClient webClient;

  @Autowired
  public SoamService(final ApplicationProperties props, final CodeTableUtils codeTableUtils,final SoamUtil util, final WebClient webClient) {
    this.props = props;
    this.codeTableUtils = codeTableUtils;
    this.soamUtil = util;
    this.webClient = webClient;
  }

  public void performLogin(String identifierType, String identifierValue, ServicesCardEntity servicesCard) {
    validateExtendedSearchParameters(identifierType, identifierValue);
    //RestTemplate restTemplate = restUtils.getRestTemplate();
    //HttpHeaders headers = new HttpHeaders();
    //headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    manageLogin(identifierType, identifierValue, servicesCard);
  }

  private void updateDigitalID(ServicesCardEntity servicesCard, DigitalIDEntity response) {
    try {
      //Update the last used date
      DigitalIDEntity digitalIDEntity = response;
      //restTemplate.put(props.getDigitalIdentifierApiURL(), digitalIDEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
      this.webClient.put()
              .uri(this.props.getDigitalIdentifierApiURL())
              .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .body(Mono.just(digitalIDEntity),DigitalIDEntity.class)
              .retrieve()
              .bodyToMono(DigitalIDEntity.class)
              .block();
      if (servicesCard != null) {
        createOrUpdateBCSC(servicesCard, digitalIDEntity.getDigitalID());
      }
    } catch (final WebClientResponseException e) {
      throw new SoamRuntimeException(getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
    }
  }

  private void manageLogin(String identifierType, String identifierValue, ServicesCardEntity servicesCard) {
    DigitalIDEntity response;
    try {
      //This is the initial call to determine if we have this digital identity
      //response = restTemplate
        //        .exchange(props.getDigitalIdentifierApiURL() + "?identitytype=" + identifierType + "&identityvalue=" + identifierValue.toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
     //props.getDigitalIdentifierApiURL()+"?identitytype=" + identifierType + "&identityvalue=" + identifierValue.toUpperCase()
      response = this.webClient.get()
              .uri(this.props.getDigitalIdentifierApiURL(),
                       uri -> uri.queryParam("identitytype",identifierType)
                      .queryParam("identityvalue",identifierValue.toUpperCase())
                      .build())
              .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .retrieve()
              .bodyToMono(DigitalIDEntity.class)
              .block();
      updateDigitalID(servicesCard, response); //update Digital Id if we have one.
    } catch (final WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        //Digital Identity does not exist, let's create it
        DigitalIDEntity entity = soamUtil.createDigitalIdentity(identifierType, identifierValue.toUpperCase());
        try {
          //ResponseEntity<DigitalIDEntity> responseEntity = restTemplate.postForEntity(props.getDigitalIdentifierApiURL(), entity, DigitalIDEntity.class);
          DigitalIDEntity responseEntity = this.webClient.post()
                  .uri(this.props.getDigitalIdentifierApiURL())
                  .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .body(Mono.just(entity),DigitalIDEntity.class)
                  .retrieve()
                  .bodyToMono(DigitalIDEntity.class)
                  .block();
          if (servicesCard != null && responseEntity != null) {
            createOrUpdateBCSC(servicesCard, responseEntity.getDigitalID());
          }
        } catch (final WebClientResponseException ex) {
          throw new SoamRuntimeException(getErrorMessageString(ex.getStatusCode(), ex.getResponseBodyAsString()));
        }
      } else {
        throw new SoamRuntimeException(getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
  }

  public void createOrUpdateBCSC(ServicesCardEntity servicesCard, UUID digitalIdentityID) {
    //HttpHeaders headers = new HttpHeaders();
   // headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    ServicesCardEntity response;
    servicesCard.setDigitalIdentityID(digitalIdentityID);
    try {
      //This is the initial call to determine if we have this digital identity
      //response = restTemplate.exchange(
      // props.getServicesCardApiURL() + "?did=" + servicesCard.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
      //Record found , let's update the record
      response = this.webClient.get()
              .uri(this.props.getServicesCardApiURL(),uri ->uri
                      .queryParam("did", servicesCard.getDid())
                      .build())
              .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .retrieve()
              .bodyToMono(ServicesCardEntity.class)
              .block();
      updateBCSC(response);
    } catch (final WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        //Services Card record does not exist, let's create it
        //restTemplate.postForEntity(props.getServicesCardApiURL(), servicesCard, ServicesCardEntity.class);
          this.webClient.post()
                  .uri(this.props.getServicesCardApiURL())
                  .header(CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE)
                  .body(Mono.just(servicesCard), ServicesCardEntity.class)
                  .retrieve()
                  .bodyToMono(ServicesCardEntity.class)
                  .block();


      } else {
        throw new SoamRuntimeException(getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
  }

  private void updateBCSC(ServicesCardEntity response) {
    try {
      ServicesCardEntity servicesCardEntity = response;
      assert servicesCardEntity != null;
      servicesCardEntity.setCreateDate(null);
      servicesCardEntity.setUpdateDate(null);
     // restTemplate.put(props.getServicesCardApiURL(), servicesCardEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
      this.webClient.put()
              .uri(this.props.getServicesCardApiURL())
              .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .body(Mono.just(servicesCardEntity),ServicesCardEntity.class)
              .retrieve()
              .bodyToMono(ServicesCardEntity.class)
              .block();

    } catch (final WebClientResponseException e) {
      throw new SoamRuntimeException(getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
    }
  }

  public SoamLoginEntity getSoamLoginEntity(String identifierType, String identifierValue) {
    validateSearchParameters(identifierType, identifierValue);
    DigitalIDEntity digitalIDEntity = getDigitalIDEntityForLogin(identifierType, identifierValue);
    try {
      //If we've reached here we do have a digital identity for this user, if they have a student ID in the digital ID record then we fetch the student
      ServicesCardEntity serviceCardEntity = null;
      if (identifierType.equals(ApplicationProperties.BCSC)) {
        serviceCardEntity = getServiceCardEntity(identifierValue);
      }

      if (digitalIDEntity.getStudentID() != null) {
        StudentEntity studentResponse;
        //studentResponse = restTemplate.exchange(props.getStudentApiURL() + "/" + digitalIDEntity.getStudentID(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), StudentEntity.class);
        studentResponse = this.webClient.get()
                .uri(this.props.getStudentApiURL(),uri-> uri.path("/{studentID}")
                        .build(digitalIDEntity.getStudentID()))
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(StudentEntity.class)
                .block();
        return soamUtil.createSoamLoginEntity(studentResponse, digitalIDEntity.getDigitalID(), serviceCardEntity);
      } else {
        return soamUtil.createSoamLoginEntity(null, digitalIDEntity.getDigitalID(), serviceCardEntity);
      }
    } catch (final WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        throw new SoamRuntimeException("Student was not found. URL was: " + props.getStudentApiURL() + "/" + digitalIDEntity.getStudentID());
      } else {
        throw new SoamRuntimeException(getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
  }

  private ServicesCardEntity getServiceCardEntity(String identifierValue) {
    ServicesCardEntity serviceCardEntity;
    try {
      //This is the initial call to determine if we have this service card
      //restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + identifierValue.toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
      serviceCardEntity = this.webClient.get()
              .uri(this.props.getServicesCardApiURL(),
                       uri -> uri.queryParam("did",identifierValue.toUpperCase())
                      .build())
              .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .retrieve()
              .bodyToMono(ServicesCardEntity.class)
              .block();
    } catch (final WebClientResponseException e) {
      throw new SoamRuntimeException(getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
    }
    return serviceCardEntity;
  }

  private DigitalIDEntity getDigitalIDEntityForLogin(String identifierType, String identifierValue) {
    DigitalIDEntity digitalIDEntity;
    try {
      //This is the initial call to determine if we have this digital identity
      //response = restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=" + identifierType + "&identityvalue=" + identifierValue.toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
      digitalIDEntity = this.webClient.get()
              .uri(uri -> uri.path(this.props.getDigitalIdentifierApiURL())
              .queryParam("identityType",identifierType)
              .queryParam("identityvalue",identifierValue.toUpperCase())
              .build())
              .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .retrieve()
              .bodyToMono(DigitalIDEntity.class)
              .block();;
      if (digitalIDEntity == null) {
        throw new SoamRuntimeException("Digital ID was null - unexpected error");
      }
    } catch (final WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        //This should not occur
        throw new SoamRuntimeException("Digital identity was not found. IdentifierType: " + identifierType + " IdentifierValue: " + identifierValue.toUpperCase());
      } else {
        throw new SoamRuntimeException(getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
    return digitalIDEntity;
  }

  private String getErrorMessageString(HttpStatus status, String body) {
    return "Unexpected HTTP return code: " + status + " error message: " + body;
  }

  private void validateExtendedSearchParameters(String identifierType, String identifierValue) {
    validateSearchParameters(identifierType, identifierValue);
  }

  private void validateSearchParameters(String identifierType, String identifierValue) {
    if (identifierType == null || !codeTableUtils.getAllIdentifierTypeCodes().containsKey(identifierType)) {
      log.error("Invalid Identifier Type :: {}", identifierType);
      throw new InvalidParameterException("identifierType");
    } else if (identifierValue == null || identifierValue.length() < 1) {
      throw new InvalidParameterException("identifierValue");
    }
  }
}
