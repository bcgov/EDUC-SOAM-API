package ca.bc.gov.educ.api.soam.service;

import ca.bc.gov.educ.api.soam.SoamApiResourceApplication;
import ca.bc.gov.educ.api.soam.codetable.CodeTableUtils;
import ca.bc.gov.educ.api.soam.exception.InvalidParameterException;
import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.model.SoamServicesCard;
import ca.bc.gov.educ.api.soam.model.SoamStudent;
import ca.bc.gov.educ.api.soam.model.entity.*;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.util.SoamUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class SoamServiceTest {
  private final HttpHeaders headers = new HttpHeaders();

  @Autowired
  SoamService service;


  @Autowired
  ApplicationProperties props;

  @MockBean
  CodeTableUtils codeTableUtils;


  @Autowired
  WebClient webClient;

  @MockBean
  SoamUtil soamUtil;

  @Mock
  private WebClient.RequestHeadersSpec requestHeadersMock;

  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriMock;

  @Mock
  private WebClient.RequestBodySpec requestBodyMock;

  @Mock
  private WebClient.RequestBodyUriSpec requestBodyUriMock;

  @Mock
  private WebClient.ResponseSpec responseMock;


  @Before
  public void setUp() {
    openMocks(this);
  }

  @Test
  public void testPerformLogin_GivenIdentifierTypeNull_ThrowsInvalidParameterException() {
    assertThrows(InvalidParameterException.class, () -> service.performLogin(null, "12345", null));
  }

  @Test
  public void testPerformLogin_GivenIdentifierValueNull_ThrowsInvalidParameterException() {
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
    assertThrows(InvalidParameterException.class, () -> service.performLogin("BCeId", null, null));
  }

  @Test
  public void testPerformLogin_GivenIdentifierValueBlank_ThrowsInvalidParameterException() {
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
    assertThrows(InvalidParameterException.class, () -> service.performLogin("BCeId", "", null));
  }

  @Test
  public void testPerformLogin_GivenIdentifierTypeNotInCodeTable_ThrowsInvalidParameterException() {
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createBlankDummyIdentityTypeMap());
    assertThrows(InvalidParameterException.class, () -> service.performLogin("BCS", "12345", null));
  }

  @Test
  public void testPerformLogin_GivenDigitalIdGetCallFailed_ShouldThrowSoamRuntimeException() {

    DigitalIDEntity entity = createDigitalIdentity();
    when(soamUtil.createDigitalIdentity("BCeId", "12345")).thenReturn(entity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());

    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    //when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class))
    //        .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
    //when(restTemplate.postForEntity(props.getDigitalIdentifierApiURL(), entity
     //       , DigitalIDEntity.class)).thenReturn(ResponseEntity.ok(entity));

    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenThrow(new SoamRuntimeException());
    //assertThrows(SoamRuntimeException.class, () -> service.performLogin("BCeId", "12345", null));

    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
    //assertThrows(SoamRuntimeException.class, () -> service.performLogin("BCeId", "12345", null));

    //post
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    when(webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getDigitalIdentifierApiURL())))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.header(any(),any()))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyUriMock.header(any(), any())).thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.accept(any())).thenReturn(requestBodyMock);
    when(this.requestBodyMock.body(any(),(Class<?>) Object.class)).thenReturn(requestHeadersMock);
    when(this.requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class)).thenReturn(Mono.just(entity));

    verify(this.webClient, atMost(invocations + 1)).post();
    assertThat(entity).isNotNull();
    assertThat(entity.getIdentityTypeCode()).isEqualTo("BCeId");

    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
 //   assertThrows(SoamRuntimeException.class, () -> service.performLogin("BCeId", "12345", null));

  }

  @Test
  public void testPerformLogin_GivenDigitalIdPostCallFailed_ShouldThrowSoamRuntimeException() {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    DigitalIDEntity entity = createDigitalIdentity();
    when(soamUtil.createDigitalIdentity("BCeId", "12345")).thenReturn(entity);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class))
//            .thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND));
//    when(restTemplate.postForEntity(props.getDigitalIdentifierApiURL(), entity
//            , DigitalIDEntity.class)).thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));'
    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
    assertThrows(SoamRuntimeException.class, () -> service.performLogin("BCeId", "12345", null));
    //post
    when(webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getDigitalIdentifierApiURL())))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.header(any(),any()))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyUriMock.header(any(), any())).thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.accept(any())).thenReturn(requestBodyMock);
    when(this.requestBodyMock.body(any(),(Class<?>) Object.class)).thenReturn(requestHeadersMock);
    when(this.requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

    verify(this.webClient, atMost(invocations + 1)).post();
    assertThat(entity).isNotNull();
    assertThat(entity.getIdentityTypeCode()).isEqualTo("BCeId");
    assertThrows(SoamRuntimeException.class, () -> service.performLogin("BCeId", "12345", null));
  }

  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardCreationFailed_ShouldThrowSoamRuntimeException() {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    DigitalIDEntity entity = createDigitalIdentity();
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity;
    ServicesCardEntity servicesCardEntity = createServiceCardEntity();
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(soamUtil.getUpdatedDigitalId(responseEntity)).thenReturn(updatedEntity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
//    doNothing().when(restTemplate).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//    when(restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class))
//            .thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND));
//    when(restTemplate.postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class))
//            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");

    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getServicesCardApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND));
    //put()
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getDigitalIdentifierApiURL()), any(Function.class)))
            .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
            .thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(updatedEntity));
    assertThat(updatedEntity).isNotNull();
    verify(this.webClient,atMost(invocations+1)).put();




    assertThrows(SoamRuntimeException.class, () -> service.performLogin("BCeId", "12345", servicesCardEntity));
    //lets verify the get method was called to  get digital id.
    //verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);


    //lets verify the post method was never called to create a new digital id record.
//    verify(restTemplate, never()).postForEntity(props.getDigitalIdentifierApiURL(), entity
//            , DigitalIDEntity.class);
//
//    //lets verify digital id api update was called.
//    verify(restTemplate, atLeastOnce()).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify Service card api get was called.
//    verify(restTemplate, atLeastOnce()).exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
//
//    //lets verify service card api create was called.
//    verify(restTemplate, atLeastOnce()).postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class);
  }

  @Test
  public void testPerformLogin_GivenDigitalIdDoesNotExistAndServiceCardIsNull_ShouldCreateDigitalId() {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    DigitalIDEntity entity = createDigitalIdentity();
    when(soamUtil.createDigitalIdentity("BCeId", "12345")).thenReturn(entity);
    when(soamUtil.getUpdatedDigitalId(entity)).thenReturn(entity);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
   // when(restUtils.getRestTemplate()).thenReturn(restTemplate);
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class))
//            .thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND));
//    when(restTemplate.postForEntity(props.getDigitalIdentifierApiURL(), entity
//            , DigitalIDEntity.class)).thenReturn(ResponseEntity.ok(entity));
    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND));

    //post()
    when(webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getDigitalIdentifierApiURL())))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.header(any(),any()))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyUriMock.header(any(), any())).thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.accept(any())).thenReturn(requestBodyMock);
    when(this.requestBodyMock.body(any(),(Class<?>) any(Object.class))).thenReturn(requestHeadersMock);
    when(this.requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(entity));

    service.performLogin("BCeId", "12345", null);
    verify(this.webClient, atMost(invocations + 1)).post();
    assertThat(entity).isNotNull();
    assertThat(entity.getIdentityTypeCode()).isEqualTo("BCeId");
    //lets verify the get method was called.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify the post method was called to create a new record.
//    verify(restTemplate, atLeastOnce()).postForEntity(props.getDigitalIdentifierApiURL(), entity
//            , DigitalIDEntity.class);
//
//    //lets verify update was never called.
//    verify(restTemplate, never()).put(props.getDigitalIdentifierApiURL(), entity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify Service card api was not called.
//    verify(restTemplate, never()).exchange(props.getServicesCardApiURL() + "?did=" + null, HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
  }

  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardIsNull_ShouldUpdateDigitalId() {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    DigitalIDEntity entity = createDigitalIdentity();
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity;
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(soamUtil.getUpdatedDigitalId(responseEntity)).thenReturn(updatedEntity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
//    doNothing().when(restTemplate).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");

    //put()
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getDigitalIdentifierApiURL()), any(Function.class)))
            .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
            .thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(updatedEntity));

    assertThat(updatedEntity).isNotNull();
    verify(this.webClient,atMost(invocations+1)).put();


    service.performLogin("BCeId", "12345", null);

    //lets verify the get method was called.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//
//    //lets verify the post method was never called to create a new record.
//    verify(restTemplate, never()).postForEntity(props.getDigitalIdentifierApiURL(), entity
//            , DigitalIDEntity.class);
//
//    //lets verify update was called.
//    verify(restTemplate, atLeastOnce()).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify Service card api was not called.
//    verify(restTemplate, never()).exchange(props.getServicesCardApiURL() + "?did=" + null, HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
  }

  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardDoesNot_ShouldUpdateDigitalIdAndCreateServicesCard() {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    DigitalIDEntity entity = createDigitalIdentity();
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity;
    ServicesCardEntity servicesCardEntity = createServiceCardEntity();
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(soamUtil.getUpdatedDigitalId(Objects.requireNonNull(responseEntity))).thenReturn(updatedEntity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
//    doNothing().when(restTemplate).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//    when(restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class))
//            .thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND));
//    when(restTemplate.postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class)).thenReturn(ResponseEntity.ok().build());
    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");
    //put()
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getDigitalIdentifierApiURL()), any(Function.class)))
            .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
            .thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(updatedEntity));

    assertThat(updatedEntity).isNotNull();
    verify(this.webClient,atMost(invocations+1)).put();

    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getServicesCardApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND));

    //post()
    when(webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getServicesCardApiURL())))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.header(any(),any()))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyUriMock.header(any(), any())).thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.accept(any())).thenReturn(requestBodyMock);
    when(this.requestBodyMock.body(any(),(Class<?>) Object.class)).thenReturn(requestHeadersMock);
    when(this.requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenReturn(Mono.just(servicesCardEntity));

    verify(this.webClient, atMost(invocations + 1)).post();
    assertThat(entity).isNotNull();
    assertThat(entity.getIdentityTypeCode()).isEqualTo("BCeId");
    service.performLogin("BCeId", "12345", servicesCardEntity);

//    //lets verify the get method was called to  get digital id.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//
//    //lets verify the post method was never called to create a new digital id record.
//    verify(restTemplate, never()).postForEntity(props.getDigitalIdentifierApiURL(), entity
//            , DigitalIDEntity.class);
//
//    //lets verify digital id api update was called.
//    verify(restTemplate, atLeastOnce()).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify Service card api get was called.
//    verify(restTemplate, atLeastOnce()).exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
//
//    //lets verify service card api create was called.
//    verify(restTemplate, atLeastOnce()).postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class);
  }


  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardExist_ShouldUpdateDigitalIdAndServicesCard() {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    DigitalIDEntity entity = createDigitalIdentity();
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity;
    ServicesCardEntity servicesCardEntity = createServiceCardEntity();
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(soamUtil.getUpdatedDigitalId(responseEntity)).thenReturn(updatedEntity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
//    doNothing().when(restTemplate).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//    when(restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class))
//            .thenReturn(ResponseEntity.ok(servicesCardEntity));
//    doNothing().when(restTemplate).put(props.getServicesCardApiURL(), servicesCardEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);

    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");
    //put()
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getDigitalIdentifierApiURL()), any(Function.class)))
            .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
            .thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(updatedEntity));

    assertThat(updatedEntity).isNotNull();
    verify(this.webClient,atMost(invocations+1)).put();

    assertThat(updatedEntity).isNotNull();
    verify(this.webClient,atMost(invocations+1)).put();

    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getServicesCardApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenReturn(Mono.just((servicesCardEntity)));

    assertThat(servicesCardEntity.getBirthDate()).isEqualTo("1984-11-02");
    assertThat(servicesCardEntity.getCity()).isEqualTo("Victoria");
    assertThat(servicesCardEntity.getDid()).isEqualTo("DIGITALID");

    //put()
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getServicesCardApiURL()), any(Function.class)))
            .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
            .thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenReturn(Mono.just(servicesCardEntity));

    assertThat(updatedEntity).isNotNull();
    verify(this.webClient,atMost(invocations+1)).put();
    service.performLogin("BCeId", "12345", servicesCardEntity);
  }


  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardGetCallReturnsNullBody_ShouldThrowAssertionError() {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    DigitalIDEntity entity = createDigitalIdentity();
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity;
    ServicesCardEntity servicesCardEntity = createServiceCardEntity();
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(soamUtil.getUpdatedDigitalId(Objects.requireNonNull(responseEntity))).thenReturn(updatedEntity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
//    doNothing().when(restTemplate).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//    when(restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class))
 //         .thenReturn(ResponseEntity.ok().build());
//    doNothing().when(restTemplate).put(props.getServicesCardApiURL(), servicesCardEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");
    //put()
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getDigitalIdentifierApiURL()), any(Function.class)))
            .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
            .thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(updatedEntity));

    assertThat(updatedEntity).isNotNull();
    verify(this.webClient,atMost(invocations+1)).put();
    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getServicesCardApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenReturn(Mono.just(servicesCardEntity));
    //put()
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getServicesCardApiURL()), any(Function.class)))
            .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
            .thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenReturn(Mono.just(servicesCardEntity));

    assertThat(updatedEntity).isNotNull();
    verify(this.webClient,atMost(invocations+1)).put();

    assertThrows(AssertionError.class, ()-> service.performLogin("BCeId", "12345", servicesCardEntity));

//    //lets verify the get method was called to  get digital id.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//
//    //lets verify the post method was never called to create a new digital id record.
//    verify(restTemplate, never()).postForEntity(props.getDigitalIdentifierApiURL(), entity
//            , DigitalIDEntity.class);
//
//    //lets verify digital id api update was called.
//    verify(restTemplate, atLeastOnce()).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify Service card api get was called.
//    verify(restTemplate, atLeastOnce()).exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
//
//    //lets verify service card api create was never called.
//    verify(restTemplate, never()).postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class);
//
//    //lets verify service card api update was called.
//    verify(restTemplate, never()).put(props.getServicesCardApiURL(), servicesCardEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
//
}

  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardGetCallFailed_ShouldThrowSoamRuntimeException() {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    DigitalIDEntity entity = createDigitalIdentity();
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity;
    ServicesCardEntity servicesCardEntity = createServiceCardEntity();
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(soamUtil.getUpdatedDigitalId(Objects.requireNonNull(responseEntity))).thenReturn(updatedEntity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
//    doNothing().when(restTemplate).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//    when(restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class))
//            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");

    //put()
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getDigitalIdentifierApiURL()), any(Function.class)))
            .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
            .thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(updatedEntity));

    assertThat(updatedEntity).isNotNull();
    verify(this.webClient,atMost(invocations+1)).put();
    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getServicesCardApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));



    assertThrows(SoamRuntimeException.class, () -> service.performLogin("BCeId", "12345", servicesCardEntity));

//    //lets verify the get method was called to  get digital id.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//
//    //lets verify the post method was never called to create a new digital id record.
//    verify(restTemplate, never()).postForEntity(props.getDigitalIdentifierApiURL(), entity
//            , DigitalIDEntity.class);
//
//    //lets verify digital id api update was called.
//    verify(restTemplate, atLeastOnce()).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify Service card api get was called.
//    verify(restTemplate, atLeastOnce()).exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
//
//    //lets verify service card api create was never called.
//    verify(restTemplate, never()).postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class);
//
//    //lets verify service card api update was never called.
//    verify(restTemplate, never()).put(props.getServicesCardApiURL(), servicesCardEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
  }

  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardPostCallFailed_ShouldThrowSoamRuntimeException() {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    DigitalIDEntity entity = createDigitalIdentity();
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity;
    ServicesCardEntity servicesCardEntity = createServiceCardEntity();
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(soamUtil.getUpdatedDigitalId(Objects.requireNonNull(responseEntity))).thenReturn(updatedEntity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
//    doNothing().when(restTemplate).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//    when(restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class))
//            .thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND));
//    when(restTemplate.postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class)).thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");
    //put()
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getDigitalIdentifierApiURL()), any(Function.class)))
            .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
            .thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(updatedEntity));

    assertThat(updatedEntity).isNotNull();
    verify(this.webClient,atMost(invocations+1)).put();
    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getServicesCardApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
    //post()
    when(webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getServicesCardApiURL())))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.header(any(),any()))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyUriMock.header(any(), any())).thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.accept(any())).thenReturn(requestBodyMock);
    when(this.requestBodyMock.body(any(),(Class<?>) Object.class)).thenReturn(requestHeadersMock);
    when(this.requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

    verify(this.webClient, atMost(invocations + 1)).post();
    assertThat(entity).isNotNull();
    assertThat(entity.getIdentityTypeCode()).isEqualTo("BCeId");

    assertThrows(SoamRuntimeException.class, () -> service.performLogin("BCeId", "12345", servicesCardEntity));

    //lets verify the get method was called to  get digital id.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//
//    //lets verify the post method was never called to create a new digital id record.
//    verify(restTemplate, never()).postForEntity(props.getDigitalIdentifierApiURL(), entity
//            , DigitalIDEntity.class);
//
//    //lets verify digital id api update was called.
//    verify(restTemplate, atLeastOnce()).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify Service card api get was called.
//    verify(restTemplate, atLeastOnce()).exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
//
//    //lets verify service card api create was called.
//    verify(restTemplate, atLeastOnce()).postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class);
//
//    //lets verify service card api update was never called.
//    verify(restTemplate, never()).put(props.getServicesCardApiURL(), servicesCardEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
  }

  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardUpdateCallFailed_ShouldThrowSoamRuntimeException() {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    DigitalIDEntity entity = createDigitalIdentity();
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity;
    ServicesCardEntity servicesCardEntity = createServiceCardEntity();
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(soamUtil.getUpdatedDigitalId(Objects.requireNonNull(responseEntity))).thenReturn(updatedEntity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
//    doNothing().when(restTemplate).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//    when(restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class))
//            .thenReturn(ResponseEntity.ok(servicesCardEntity));
//    doThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(restTemplate).put(props.getServicesCardApiURL(), servicesCardEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);

    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");
    //put()
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getDigitalIdentifierApiURL()), any(Function.class)))
            .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
            .thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(updatedEntity));

    assertThat(updatedEntity).isNotNull();
    verify(this.webClient,atMost(invocations+1)).put();
    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getServicesCardApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenReturn(Mono.just(servicesCardEntity));
    //put()
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getServicesCardApiURL()), any(Function.class)))
            .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
            .thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

    assertThat(updatedEntity).isNotNull();
    verify(this.webClient,atMost(invocations+1)).put();

    assertThrows(SoamRuntimeException.class, () -> service.performLogin("BCeId", "12345", servicesCardEntity));
//
//    //lets verify the get method was called to  get digital id.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//
//    //lets verify the post method was never called to create a new digital id record.
//    verify(restTemplate, never()).postForEntity(props.getDigitalIdentifierApiURL(), entity
//            , DigitalIDEntity.class);
//
//    //lets verify digital id api update was called.
//    verify(restTemplate, atLeastOnce()).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify Service card api get was called.
//    verify(restTemplate, atLeastOnce()).exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
//
//    //lets verify service card api create was never called.
//    verify(restTemplate, never()).postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class);
//
//    //lets verify service card api update was never called.
//    verify(restTemplate, atLeastOnce()).put(props.getServicesCardApiURL(), servicesCardEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
  }

  @Test
  public void testPerformLogin_GivenDigitalIdAndServiceCardDoesNotExist_ShouldCreateBothRecords() {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    DigitalIDEntity entity = createDigitalIdentity();
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity;
    ServicesCardEntity servicesCardEntity = createServiceCardEntity();
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(soamUtil.createDigitalIdentity("BCeId", "12345")).thenReturn(entity);
    when(soamUtil.getUpdatedDigitalId(Objects.requireNonNull(responseEntity))).thenReturn(updatedEntity);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND));
//    when(restTemplate.postForEntity(props.getDigitalIdentifierApiURL(), entity
//            , DigitalIDEntity.class)).thenReturn(ResponseEntity.ok(entity));
//    when(restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class)).
//            thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND));
//    when(restTemplate.postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class)).thenReturn(ResponseEntity.ok().build());

    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

    //post
    when(webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getDigitalIdentifierApiURL())))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.header(any(),any()))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyUriMock.header(any(), any())).thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.accept(any())).thenReturn(requestBodyMock);
    when(this.requestBodyMock.body(any(),(Class<?>)any(Object.class))).thenReturn(requestHeadersMock);
    when(this.requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(entity));

    verify(this.webClient, atMost(invocations + 1)).post();
    assertThat(entity).isNotNull();
    assertThat(entity.getIdentityTypeCode()).isEqualTo("BCeId");

    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

    //post()
    when(webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(props.getServicesCardApiURL())))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.header(any(),any()))
            .thenReturn(this.requestBodyMock);
    when(this.requestBodyUriMock.header(any(), any())).thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.accept(any())).thenReturn(requestBodyMock);
    when(this.requestBodyMock.body(any(),(Class<?>) Object.class)).thenReturn(requestHeadersMock);
    when(this.requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenReturn(Mono.just(servicesCardEntity));

    verify(this.webClient, atMost(invocations + 1)).post();
    assertThat(entity).isNotNull();
    assertThat(entity.getIdentityTypeCode()).isEqualTo("BCeId");

    service.performLogin("BCeId", "12345", servicesCardEntity);

    //lets verify the get method was called to  get digital id.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//
//    //lets verify the post method was called to create a new digital id record.
//    verify(restTemplate, atLeastOnce()).postForEntity(props.getDigitalIdentifierApiURL(), entity
//            , DigitalIDEntity.class);
//
//    //lets verify digital id api update was called.
//    verify(restTemplate, never()).put(props.getDigitalIdentifierApiURL(), updatedEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify Service card api get was called.
//    verify(restTemplate, atLeastOnce()).exchange(props.getServicesCardApiURL() + "?did=" + servicesCardEntity.getDid().toUpperCase(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
//
//    //lets verify service card api create was never called.
//    verify(restTemplate, atLeastOnce()).postForEntity(props.getServicesCardApiURL(), servicesCardEntity, ServicesCardEntity.class);
//
//    //lets verify service card api update was never called.
//    verify(restTemplate, never()).put(props.getServicesCardApiURL(), servicesCardEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);
  }

  @Test
  public void testGetSoamLoginEntity_GivenDigitalIdGetCallNotFound_ShouldThrowSoamRuntimeException() {
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
  //    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
  //            thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND));

    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));


    assertThrows(SoamRuntimeException.class, () -> service.getSoamLoginEntity("BCeId", "12345"));

//    //lets verify the get method was called to  get digital id.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);

  }

  @Test
  public void testGetSoamLoginEntity_GivenDigitalIdGetCallReturnsBlankResponse_ShouldThrowSoamRuntimeException() {
//    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    DigitalIDEntity entity = createDigitalIdentity();
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    DigitalIDEntity updatedEntity = responseEntity;
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(ResponseEntity.ok().build());
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");

    assertThrows(SoamRuntimeException.class, () -> service.getSoamLoginEntity("BCeId", "12345"));

    //lets verify the get method was called to  get digital id.
    //verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);

  }

  @Test
  public void testGetSoamLoginEntity_GivenDigitalIdGetCallFailed_ShouldThrowSoamRuntimeException() {
 //   when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
    assertThrows(SoamRuntimeException.class, () -> service.getSoamLoginEntity("BCeId", "12345"));

    //lets verify the get method was called to  get digital id.
   // verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);

  }

  @Test
  public void testGetSoamLoginEntity_GivenDigitalIdGetCallSuccessForBCeIdWithoutStudentId_ShouldReturnEntityWithoutStudentAndServicesCard() {
    UUID digitalId = UUID.randomUUID();
    DigitalIDEntity entity = createDigitalIdentity();
    entity.setDigitalID(digitalId);
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    when(soamUtil.createSoamLoginEntity(null, digitalId, null)).thenReturn(createSoamLoginEntity(null, digitalId, null));
//    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");

    SoamLoginEntity soamLoginEntity = service.getSoamLoginEntity("BCeId", "12345");

    //lets verify the get method was called to  get digital id.
   // verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);

    assertNotNull(soamLoginEntity.getDigitalIdentityID());
    assertThat(soamLoginEntity.getDigitalIdentityID()).isEqualTo(digitalId);
    assertNull(soamLoginEntity.getServiceCard());
    assertNull(soamLoginEntity.getStudent());

  }

  @Test
  public void testGetSoamLoginEntity_GivenBCeIdAssociatedToStudent_ShouldReturnEntityWithStudent() {
    UUID digitalId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    DigitalIDEntity entity = createDigitalIdentity();
    entity.setDigitalID(digitalId);
    entity.setStudentID(studentId.toString());
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    StudentEntity studentEntity = createStudentEntity(studentId);
    StudentEntity studentResponseEntity = createStudentResponseEntity(studentEntity);
    when(soamUtil.createSoamLoginEntity(studentEntity, digitalId, null)).thenReturn(createSoamLoginEntity(studentEntity, digitalId, null));
//    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
//    when(restTemplate.exchange(props.getStudentApiURL() + "/" + studentId.toString(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), StudentEntity.class)).thenReturn(studentResponseEntity);
//get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");
//get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getStudentApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(StudentEntity.class))
            .thenReturn(Mono.just(studentResponseEntity));

    SoamLoginEntity soamLoginEntity = service.getSoamLoginEntity("BCeId", "12345");
//
//    //lets verify the get method was called to  get digital id.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify the get method was called to  get student.
//    verify(restTemplate, atLeastOnce()).exchange(props.getStudentApiURL() + "/" + studentId.toString(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), StudentEntity.class);

    assertNotNull(soamLoginEntity.getDigitalIdentityID());
    assertThat(soamLoginEntity.getDigitalIdentityID()).isEqualTo(digitalId);
    assertNull(soamLoginEntity.getServiceCard());
    assertNotNull(soamLoginEntity.getStudent());
    assertNotNull(soamLoginEntity.getStudent().getDob());
    assertThat(soamLoginEntity.getStudent().getLegalLastName()).isEqualTo("test");
  }

  @Test
  public void testGetSoamLoginEntity_GivenBCeIdNotAssociatedToStudent_ShouldThrowSoamRuntimeException() {
    UUID digitalId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    DigitalIDEntity entity = createDigitalIdentity();
    entity.setDigitalID(digitalId);
    entity.setStudentID(studentId.toString());
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    StudentEntity studentEntity = createStudentEntity(studentId);
    when(soamUtil.createSoamLoginEntity(studentEntity, digitalId, null)).thenReturn(createSoamLoginEntity(studentEntity, digitalId, null));
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
//    when(restTemplate.exchange(props.getStudentApiURL() + "/" + studentId.toString(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), StudentEntity.class)).thenThrow(createHttpClientErrorException(HttpStatus.NOT_FOUND));
    //assertThrows(SoamRuntimeException.class, () -> service.getSoamLoginEntity("BCeId", "12345"));
    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");
    //get()
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getStudentApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(StudentEntity.class))
            .thenThrow(new SoamRuntimeException());

    //lets verify the get method was called to  get digital id.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify the get method was called to  get student.
//    verify(restTemplate, atLeastOnce()).exchange(props.getStudentApiURL() + "/" + studentId.toString(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), StudentEntity.class);

  }

  @Test
  public void testGetSoamLoginEntity_GivenStudentAPICallFailed_ShouldThrowSoamRuntimeException() {
    UUID digitalId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    DigitalIDEntity entity = createDigitalIdentity();
    entity.setDigitalID(digitalId);
    entity.setStudentID(studentId.toString());
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    StudentEntity studentEntity = createStudentEntity(studentId);
    when(soamUtil.createSoamLoginEntity(studentEntity, digitalId, null)).thenReturn(createSoamLoginEntity(studentEntity, digitalId, null));
//    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
//    when(restTemplate.exchange(props.getStudentApiURL() + "/" + studentId.toString(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), StudentEntity.class)).thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");

    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getStudentApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(StudentEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));



    assertThrows(SoamRuntimeException.class, () -> service.getSoamLoginEntity("BCeId", "12345"));

    //lets verify the get method was called to  get digital id.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCeId&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify the get method was called to  get student.
//    verify(restTemplate, atLeastOnce()).exchange(props.getStudentApiURL() + "/" + studentId.toString(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), StudentEntity.class);

  }

  @Test
  public void testGetSoamLoginEntity_GivenBCSCAssociatedToStudent_ShouldReturnEntityWithStudentAndServiceCard() {
    UUID digitalId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    DigitalIDEntity entity = createDigitalIdentity();
    entity.setDigitalID(digitalId);
    entity.setStudentID(studentId.toString());
    entity.setIdentityTypeCode("BCSC");
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    StudentEntity studentEntity = createStudentEntity(studentId);
    StudentEntity studentResponseEntity = createStudentResponseEntity(studentEntity);
    ServicesCardEntity servicesCardEntity = createServiceCardEntity();
    ServicesCardEntity servicesCardResponseEntity = createServicesCardResponseEntity(servicesCardEntity);
    when(soamUtil.createSoamLoginEntity(studentEntity, digitalId, servicesCardEntity)).thenReturn(createSoamLoginEntity(studentEntity, digitalId, servicesCardEntity));
//    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCSC&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
//    when(restTemplate.exchange(props.getStudentApiURL() + "/" + studentId.toString(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), StudentEntity.class)).thenReturn(studentResponseEntity);
//    when(restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + "12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class)).thenReturn(servicesCardResponseEntity);

    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");

    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getStudentApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(StudentEntity.class))
            .thenReturn(Mono.just(studentResponseEntity));

    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getServicesCardApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenReturn(Mono.just(servicesCardResponseEntity));

    SoamLoginEntity soamLoginEntity = service.getSoamLoginEntity("BCSC", "12345");
//    //lets verify the get method was called to  get digital id.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCSC&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify the get method was called to  get student.
//    verify(restTemplate, atLeastOnce()).exchange(props.getStudentApiURL() + "/" + studentId.toString(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), StudentEntity.class);
//
//    //lets verify the get method was called to  get services card.
//    verify(restTemplate, atLeastOnce()).exchange(props.getServicesCardApiURL() + "?did=" + "12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);

    assertNotNull(soamLoginEntity.getDigitalIdentityID());
    assertThat(digitalId).isEqualTo(soamLoginEntity.getDigitalIdentityID());
    assertNotNull(soamLoginEntity.getServiceCard());
    assertNotNull(soamLoginEntity.getStudent());
    assertNotNull(soamLoginEntity.getStudent().getDob());
    assertThat(soamLoginEntity.getStudent().getLegalLastName()).isEqualTo("test");
  }

  @Test
  public void testGetSoamLoginEntity_GivenServicesCardAPICallFailed_ShouldThrowSoamRuntimeException() {
    UUID digitalId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    DigitalIDEntity entity = createDigitalIdentity();
    entity.setDigitalID(digitalId);
    entity.setStudentID(studentId.toString());
    entity.setIdentityTypeCode("BCSC");
    DigitalIDEntity responseEntity = createResponseEntity(entity);
    StudentEntity studentEntity = createStudentEntity(studentId);
    StudentEntity studentResponseEntity = createStudentResponseEntity(studentEntity);
    ServicesCardEntity servicesCardEntity = createServiceCardEntity();
    when(soamUtil.createSoamLoginEntity(studentEntity, digitalId, servicesCardEntity)).thenReturn(createSoamLoginEntity(studentEntity, digitalId, servicesCardEntity));
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(createDummyIdentityTypeMap());
//    when(restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCSC&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class)).
//            thenReturn(responseEntity);
//    when(restTemplate.exchange(props.getStudentApiURL() + "/" + studentId.toString(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), StudentEntity.class)).thenReturn(studentResponseEntity);
//    when(restTemplate.exchange(props.getServicesCardApiURL() + "?did=" + "12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class))
//            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));

    var result = soamUtil.getUpdatedDigitalId(responseEntity);
    assertThat(result.getIdentityTypeCode()).isEqualTo("BCeID");
    assertThat(result.getIdentityValue()).isEqualTo("12345");
    assertThat(result.getLastAccessChannelCode()).isEqualTo("OSPR");

    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getStudentApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(StudentEntity.class))
            .thenReturn(Mono.just(studentResponseEntity));

    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getServicesCardApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenThrow(createHttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));




    assertThrows(SoamRuntimeException.class, ()-> service.getSoamLoginEntity("BCSC", "12345"));

    //lets verify the get method was called to  get digital id.
//    verify(restTemplate, atLeastOnce()).exchange(props.getDigitalIdentifierApiURL() + "?identitytype=BCSC&identityvalue=12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
//
//    //lets verify the get method was never called to  get student.
//    verify(restTemplate, never()).exchange(props.getStudentApiURL() + "/" + studentId.toString(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), StudentEntity.class);
//
//    //lets verify the get method was called to  get services card.
//    verify(restTemplate, atLeastOnce()).exchange(props.getServicesCardApiURL() + "?did=" + "12345", HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), ServicesCardEntity.class);


  }

  private ServicesCardEntity createServicesCardResponseEntity(ServicesCardEntity servicesCardEntity) {
    return servicesCardEntity;
  }

  private StudentEntity createStudentEntity(UUID studentId) {
    StudentEntity.StudentEntityBuilder builder = StudentEntity.builder();
    builder.studentID(Objects.requireNonNullElseGet(studentId, UUID::randomUUID));
    builder.dob(LocalDate.now().toString());
    builder.legalFirstName("test").legalLastName("test").email("test@abc.com").genderCode('M').pen("123456789").sexCode('M').dataSourceCode("MYED");
    return builder.build();
  }

  private StudentEntity createStudentResponseEntity(StudentEntity entity) {
    return entity;
  }

  private DigitalIDEntity createResponseEntity(DigitalIDEntity entity) {
    DigitalIDEntity responseEntity = new DigitalIDEntity();
    BeanUtils.copyProperties(entity, responseEntity);
    if (responseEntity.getDigitalID() == null) {
      responseEntity.setDigitalID(UUID.randomUUID());
    }
    return responseEntity;
  }


  private HttpClientErrorException createHttpClientErrorException(HttpStatus status) {
    return new HttpClientErrorException(status, status.toString());
  }

  private Map<String, IdentityTypeCodeEntity> createDummyIdentityTypeMap() {
    Map<String, IdentityTypeCodeEntity> identityTypeCodeEntityMap = new HashMap<>();
    identityTypeCodeEntityMap.put("BCeId", IdentityTypeCodeEntity.builder().identityTypeCode("BCeId").build());
    identityTypeCodeEntityMap.put("BCSC", IdentityTypeCodeEntity.builder().identityTypeCode("BCSC").build());
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

  public SoamLoginEntity createSoamLoginEntity(StudentEntity student, UUID digitalIdentifierID, ServicesCardEntity serviceCardEntity) {
    SoamLoginEntity entity = new SoamLoginEntity();

    setStudentEntity(student, entity);

    setServicesCard(digitalIdentifierID, serviceCardEntity, entity);

    entity.setDigitalIdentityID(digitalIdentifierID);

    return entity;
  }

  private void setServicesCard(UUID digitalIdentifierID, ServicesCardEntity serviceCardEntity, SoamLoginEntity entity) {
    if (serviceCardEntity != null) {
      SoamServicesCard serviceCard = new SoamServicesCard();
      serviceCard.setServicesCardInfoID(serviceCardEntity.getServicesCardInfoID());
      serviceCard.setDigitalIdentityID(digitalIdentifierID);
      serviceCard.setBirthDate(serviceCardEntity.getBirthDate());
      serviceCard.setCity(serviceCardEntity.getCity());
      serviceCard.setCountry(serviceCardEntity.getCountry());
      serviceCard.setDid(serviceCardEntity.getDid());
      serviceCard.setEmail(serviceCardEntity.getEmail());
      serviceCard.setGender(serviceCardEntity.getGender());
      serviceCard.setGivenName(serviceCardEntity.getGivenName());
      serviceCard.setGivenNames(serviceCardEntity.getGivenNames());
      serviceCard.setPostalCode(serviceCardEntity.getPostalCode());
      serviceCard.setIdentityAssuranceLevel(serviceCardEntity.getIdentityAssuranceLevel());
      serviceCard.setProvince(serviceCardEntity.getProvince());
      serviceCard.setStreetAddress(serviceCardEntity.getStreetAddress());
      serviceCard.setSurname(serviceCardEntity.getSurname());
      serviceCard.setUserDisplayName(serviceCardEntity.getUserDisplayName());
      serviceCard.setUpdateDate(serviceCardEntity.getUpdateDate());
      serviceCard.setUpdateUser(serviceCardEntity.getUpdateUser());
      serviceCard.setCreateDate(serviceCardEntity.getCreateDate());
      serviceCard.setCreateUser(serviceCardEntity.getCreateUser());

      entity.setServiceCard(serviceCard);
    }
  }

  private void setStudentEntity(StudentEntity student, SoamLoginEntity entity) {
    if (student != null) {
      SoamStudent soamStudent = new SoamStudent();

      soamStudent.setCreateDate(student.getCreateDate());
      soamStudent.setCreateUser(student.getCreateUser());
      soamStudent.setDataSourceCode(student.getDataSourceCode());
      soamStudent.setDeceasedDate(student.getDeceasedDate());
      soamStudent.setDob(student.getDob());
      soamStudent.setEmail(student.getEmail());
      soamStudent.setGenderCode(student.getGenderCode());
      soamStudent.setLegalFirstName(student.getLegalFirstName());
      soamStudent.setLegalLastName(student.getLegalLastName());
      soamStudent.setLegalMiddleNames(student.getLegalMiddleNames());
      soamStudent.setPen(student.getPen());
      soamStudent.setSexCode(student.getSexCode());
      soamStudent.setStudentID(student.getStudentID());
      soamStudent.setUpdateDate(student.getUpdateDate());
      soamStudent.setUpdateUser(student.getUpdateUser());
      soamStudent.setUsualFirstName(student.getUsualFirstName());
      soamStudent.setUsualLastName(student.getUsualLastName());
      soamStudent.setUsualMiddleNames(student.getUsualMiddleNames());

      entity.setStudent(soamStudent);
    }
  }
  private WebClient.RequestBodySpec returnMockBodySpec() {
    return this.requestBodyMock;
  }
}
