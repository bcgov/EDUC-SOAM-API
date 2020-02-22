package ca.bc.gov.educ.api.soam.service;

import ca.bc.gov.educ.api.soam.SoamApiResourceApplication;
import ca.bc.gov.educ.api.soam.codetable.CodeTableUtils;
import ca.bc.gov.educ.api.soam.exception.InvalidParameterException;
import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.model.entity.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.model.entity.IdentityTypeCodeEntity;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.rest.RestUtils;
import ca.bc.gov.educ.api.soam.util.SoamUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
@SpringBootTest(classes = SoamApiResourceApplication.class)
public class SoamServiceTest {

  private static final String PARAMETERS_ATTRIBUTE = "parameters";
  private final HttpHeaders headers = new HttpHeaders();

  @Autowired
  SoamService service;


  @Autowired
  ApplicationProperties props;

  @MockBean
  CodeTableUtils codeTableUtils;

  @MockBean
  RestUtils restUtils;

  @MockBean
  RestTemplate restTemplate;

  @MockBean
  SoamUtil soamUtil;


  @Before
  public void setUp() {
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testPerformLogin_GivenIdentifierTypeNull_ThrowsInvalidParameterException() {
    assertThrows(InvalidParameterException.class, () -> service.performLogin(null, "12345", "TESTMARCO", null));
  }

  @Test
  public void testPerformLogin_GivenIdentifierValueNull_ThrowsInvalidParameterException() {
    assertThrows(InvalidParameterException.class, () -> service.performLogin("BCeId", null, "TESTMARCO", null));
  }

  @Test
  public void testPerformLogin_GivenUserIDNull_ThrowsInvalidParameterException() {
    assertThrows(InvalidParameterException.class, () -> service.performLogin("BCeId", "12345", null, null));
  }

  @Test
  public void testPerformLogin_GivenIdentifierTypeNotInCodeTable_ThrowsInvalidParameterException() {
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createBlankDummyIdentityTypeMap());
    assertThrows(InvalidParameterException.class, () -> service.performLogin("BCS", "12345", "TESTMARCO", null));
  }

  @Test
  public void testPerformLogin_GivenDigitalIdGetCallFailed_ShouldThrowSoamRuntimeException() {

    DigitalIDEntity entity = createDigitalIdentity();
    when(soamUtil.createDigitalIdentity("BCeId", "12345", "TESTMARCO")).thenReturn(entity);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"));
    when(restTemplate.postForEntity(props.getDigitalIdentifierApiURL(), entity
            , DigitalIDEntity.class)).thenReturn(ResponseEntity.ok(entity));
    assertThrows(SoamRuntimeException.class, () -> service.performLogin("BCeId", "12345", "TESTMARCO", null));
    verify(restTemplate, never()).postForEntity(props.getDigitalIdentifierApiURL(), entity
            , DigitalIDEntity.class);
  }

  @Test
  public void testPerformLogin_GivenDigitalIdPostCallFailed_ShouldThrowSoamRuntimeException() {

    DigitalIDEntity entity = createDigitalIdentity();
    when(soamUtil.createDigitalIdentity("BCeId", "12345", "TESTMARCO")).thenReturn(entity);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found."));
    when(restTemplate.postForEntity(props.getDigitalIdentifierApiURL(), entity
            , DigitalIDEntity.class)).thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server error."));
    assertThrows(HttpClientErrorException.class, () -> service.performLogin("BCeId", "12345", "TESTMARCO", null));
    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
    verify(restTemplate, atLeastOnce()).postForEntity(props.getDigitalIdentifierApiURL(), entity
            , DigitalIDEntity.class);
  }

  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardCreationFailed_ShouldThrowSoamRuntimeException() {

    DigitalIDEntity entity = createDigitalIdentity();
    ResponseEntity<DigitalIDEntity> responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity.getBody();
    ServicesCardEntity servicesCardEntity = createServiceCardEntity();
    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(soamUtil.getUpdatedDigitalId(Objects.requireNonNull(responseEntity.getBody()))).thenReturn(updatedEntity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
            thenReturn(responseEntity);
    doNothing().when(restTemplate).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
    when(restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found."));
    when(restTemplate.postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class)).thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"));
    assertThrows(SoamRuntimeException.class, () -> service.performLogin("BCeId", "12345", "TESTMARCO", servicesCardEntity));
    //lets verify the get method was called to  get digital id.
    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);


    //lets verify the post method was never called to create a new digital id record.
    verify(restTemplate, never()).postForEntity(props.getDigitalIdentifierApiURL(), entity
            , DigitalIDEntity.class);

    //lets verify digital id api update was called.
    verify(restTemplate, atLeastOnce()).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);

    //lets verify Service card api get was called.
    verify(restTemplate, atLeastOnce()).exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);

    //lets verify service card api create was called.
    verify(restTemplate, atLeastOnce()).postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class);
  }

  @Test
  public void testPerformLogin_GivenDigitalIdDoesNotExistAndServiceCardIsNull_ShouldCreateDigitalId() {

    DigitalIDEntity entity = createDigitalIdentity();
    when(soamUtil.createDigitalIdentity("BCeId", "12345", "TESTMARCO")).thenReturn(entity);
    when(soamUtil.getUpdatedDigitalId(entity)).thenReturn(entity);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found."));
    when(restTemplate.postForEntity(props.getDigitalIdentifierApiURL(), entity
            , DigitalIDEntity.class)).thenReturn(ResponseEntity.ok(entity));
    service.performLogin("BCeId", "12345", "TESTMARCO", null);

    //lets verify the get method was called.
    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);

    //lets verify the post method was called to create a new record.
    verify(restTemplate, atLeastOnce()).postForEntity(props.getDigitalIdentifierApiURL(), entity
            , DigitalIDEntity.class);

    //lets verify update was never called.
    verify(restTemplate, never()).put(props.getDigitalIdentifierApiURL(), entity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);

    //lets verify Service card api was not called.
    verify(restTemplate, never()).exchange(props.getServicesCardApiURL() + "?did=" + null, HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
  }

  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardIsNull_ShouldUpdateDigitalId() {
    DigitalIDEntity entity = createDigitalIdentity();
    ResponseEntity<DigitalIDEntity> responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity.getBody();
    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(soamUtil.getUpdatedDigitalId(Objects.requireNonNull(responseEntity.getBody()))).thenReturn(updatedEntity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
            thenReturn(responseEntity);
    doNothing().when(restTemplate).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
    service.performLogin("BCeId", "12345", "TESTMARCO", null);

    //lets verify the get method was called.
    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);


    //lets verify the post method was never called to create a new record.
    verify(restTemplate, never()).postForEntity(props.getDigitalIdentifierApiURL(), entity
            , DigitalIDEntity.class);

    //lets verify update was called.
    verify(restTemplate, atLeastOnce()).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);

    //lets verify Service card api was not called.
    verify(restTemplate, never()).exchange(props.getServicesCardApiURL() + "?did=" + null, HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
  }

  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardDoesNot_ShouldUpdateDigitalIdAndCreateServicesCard() {
    DigitalIDEntity entity = createDigitalIdentity();
    ResponseEntity<DigitalIDEntity> responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity.getBody();
    ServicesCardEntity servicesCardEntity = createServiceCardEntity();
    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(soamUtil.getUpdatedDigitalId(Objects.requireNonNull(responseEntity.getBody()))).thenReturn(updatedEntity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
            thenReturn(responseEntity);
    doNothing().when(restTemplate).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
    when(restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found."));
    when(restTemplate.postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class)).thenReturn(ResponseEntity.ok().build());
    service.performLogin("BCeId", "12345", "TESTMARCO", servicesCardEntity);

    //lets verify the get method was called to  get digital id.
    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);


    //lets verify the post method was never called to create a new digital id record.
    verify(restTemplate, never()).postForEntity(props.getDigitalIdentifierApiURL(), entity
            , DigitalIDEntity.class);

    //lets verify digital id api update was called.
    verify(restTemplate, atLeastOnce()).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);

    //lets verify Service card api get was called.
    verify(restTemplate, atLeastOnce()).exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);

    //lets verify service card api create was called.
    verify(restTemplate, atLeastOnce()).postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class);
  }


  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardExist_ShouldUpdateDigitalIdAndServicesCard() {
    DigitalIDEntity entity = createDigitalIdentity();
    ResponseEntity<DigitalIDEntity> responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity.getBody();
    ServicesCardEntity servicesCardEntity = createServiceCardEntity();
    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(soamUtil.getUpdatedDigitalId(Objects.requireNonNull(responseEntity.getBody()))).thenReturn(updatedEntity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
            thenReturn(responseEntity);
    doNothing().when(restTemplate).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
    when(restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class))
            .thenReturn(ResponseEntity.ok(servicesCardEntity));
    doNothing().when(restTemplate).put(props.getServicesCardApiURL(), servicesCardEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
    service.performLogin("BCeId", "12345", "TESTMARCO", servicesCardEntity);

    //lets verify the get method was called to  get digital id.
    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);


    //lets verify the post method was never called to create a new digital id record.
    verify(restTemplate, never()).postForEntity(props.getDigitalIdentifierApiURL(), entity
            , DigitalIDEntity.class);

    //lets verify digital id api update was called.
    verify(restTemplate, atLeastOnce()).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);

    //lets verify Service card api get was called.
    verify(restTemplate, atLeastOnce()).exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);

    //lets verify service card api create was never called.
    verify(restTemplate, never()).postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class);

    //lets verify service card api update was called.
    verify(restTemplate, atLeastOnce()).put(props.getServicesCardApiURL(), servicesCardEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
  }

  private ResponseEntity<DigitalIDEntity> createResponseEntity(DigitalIDEntity entity) {
    DigitalIDEntity responseEntity = new DigitalIDEntity();
    BeanUtils.copyProperties(entity, responseEntity);
    responseEntity.setDigitalID(UUID.randomUUID());
    return ResponseEntity.ok(responseEntity);
  }


  private HttpClientErrorException createHttpClientErrorException(HttpStatus status, String statusText) {
    return new HttpClientErrorException(status, statusText);
  }

  private Map<String, IdentityTypeCodeEntity> createDummyIdentityTypeMap() {
    Map<String, IdentityTypeCodeEntity> identityTypeCodeEntityMap = new HashMap<>();
    identityTypeCodeEntityMap.put("BCeId", IdentityTypeCodeEntity.builder().identityTypeCode("BCeId").build());
    return identityTypeCodeEntityMap;
  }


  private Map<String, IdentityTypeCodeEntity> createBlankDummyIdentityTypeMap() {
    return new HashMap<>();
  }

  protected DigitalIDEntity createDigitalIdentity() {
    DigitalIDEntity entity = new DigitalIDEntity();
    entity.setIdentityTypeCode("BCeId");
    entity.setIdentityValue("12345");
    entity.setLastAccessChannelCode("OSPR");
    entity.setLastAccessDate(LocalDateTime.now().toString());
    entity.setCreateUser("TESTMARCO");
    entity.setUpdateUser("TESTMARCO");
    return entity;
  }

  private ServicesCardEntity createServiceCardEntity() {
    ServicesCardEntity serviceCard = new ServicesCardEntity();
    serviceCard.setBirthDate("1984-11-02");
    serviceCard.setCity("Victoria");
    serviceCard.setCountry("CAN");
    serviceCard.setDid("DIGITALID");
    serviceCard.setEmail("abc@gmail.com");
    serviceCard.setGender("M");
    serviceCard.setIdentityAssuranceLevel("1");
    serviceCard.setGivenName("Given");
    serviceCard.setGivenNames(null);
    serviceCard.setPostalCode("V8W 2E1");
    serviceCard.setProvince("BC");
    serviceCard.setStreetAddress("Courtney Street");
    serviceCard.setSurname("Surname");
    serviceCard.setUserDisplayName("displayName");
    return serviceCard;
  }
}
