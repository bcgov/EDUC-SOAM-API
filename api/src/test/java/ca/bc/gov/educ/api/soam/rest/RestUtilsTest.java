package ca.bc.gov.educ.api.soam.rest;

import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.model.entity.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.model.entity.StudentEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.struct.v1.penmatch.PenMatchResult;
import ca.bc.gov.educ.api.soam.struct.v1.penmatch.PenMatchStudent;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
@SuppressWarnings("java:S5778")
public class RestUtilsTest {
  private static final String correlationID = UUID.randomUUID().toString();
  @Autowired
  RestUtils restUtils;

  @Autowired
  WebClient webClient;

  @Autowired
  ApplicationProperties props;
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
  public void setUp() throws Exception {
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    openMocks(this);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testGetDigitalID_givenAPICallSuccess_shouldReturnDigitalID() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
      .thenReturn(Mono.just(responseEntity));
    val response = this.restUtils.getDigitalID("BCeId", "12345", correlationID);
    assertThat(response).isPresent();
    assertThat(response.get().getDigitalID()).isNotNull();
  }

  @Test
  public void testGetDigitalID_givenAPICallSuccess_shouldReturnDigitalIDDID() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(responseEntity));
    val response = this.restUtils.getDigitalID(responseEntity.getDigitalID().toString(), correlationID);
    assertThat(response).isPresent();
    assertThat(response.get().getDigitalID()).isNotNull();
  }

  @Test
  public void testGetDigitalIDList_givenAPICallSuccess_shouldReturnDigitalIDDID() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    responseEntity.setStudentID("12345");
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(List.class))
      .thenReturn(Mono.just(new ArrayList<>(Arrays.asList(responseEntity))));
    val response = this.restUtils.getDigitalIDByStudentID(responseEntity.getStudentID().toString(), correlationID);
    assertThat(response).size().isEqualTo(1);
  }

  @Test
  public void testGetDigitalID_givenAPICallSuccessButBlankBody_shouldThrowException() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
      .thenReturn(Mono.justOrEmpty(Optional.empty()));
    assertThrows(SoamRuntimeException.class, () -> this.restUtils.getDigitalID("BCeId", "12345", correlationID));
  }

  @Test
  public void testGetDigitalIDList_givenAPICallSuccessButBlankBody_shouldThrowException() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(List.class))
      .thenReturn(Mono.justOrEmpty(Optional.empty()));
    assertThrows(SoamRuntimeException.class, () -> this.restUtils.getDigitalIDByStudentID("12345", correlationID));
  }

  @Test
  public void testGetPenMatchResult_givenAPICallSuccess_shouldReturnPenMatchResult() {
    final PenMatchStudent student = this.createPenMatchStudent();
    final PenMatchResult penMatchResult = this.createPenMatchResponse();
    when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getPenMatchApiURL())).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any())).thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class))).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(PenMatchResult.class)).thenReturn(Mono.just(penMatchResult));

    val response = this.restUtils.postToMatchAPI(student);
    assertThat(response).isNotNull();
    assertThat(response.isPresent()).isTrue();
    assertThat(response.get().getPenStatus()).isNotNull();
  }

  @Test
  public void testGetDigitalID_givenAPICall404_shouldReturnOptionalEmpty() {
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenThrow(new WebClientResponseException(404, "NOT FOUND", null, null, null));
    val response = this.restUtils.getDigitalID("BCeId", "12345", correlationID);
    assertThat(response).isEmpty();
  }

  @Test
  public void testGetDigitalID_givenAPICall404Did_shouldReturnOptionalEmpty() {
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenThrow(new WebClientResponseException(404, "NOT FOUND", null, null, null));
    val response = this.restUtils.getDigitalID("ABC", correlationID);
    assertThat(response).isEmpty();
  }

  @Test
  public void testGetDigitalID_givenAPICallError_shouldThrowException() {
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenThrow(new WebClientResponseException(503, "SERVICE UNAVAILABLE", null, null, null));
    assertThrows(SoamRuntimeException.class, () -> this.restUtils.getDigitalID("BCeId", "12345", correlationID));
  }

  @Test
  public void getServicesCard_givenAPICallSuccess_shouldReturnServicesCard() {
    val responseEntity = this.createServiceCardEntity();
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getServicesCardApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
      .thenReturn(Mono.just(responseEntity));
    val response = this.restUtils.getServicesCard("12345", correlationID);
    assertThat(response).isPresent();
    assertThat(response.get().getDid()).isNotNull();
  }

  @Test
  public void testGetServicesCard_givenAPICallError_shouldThrowException() {
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getServicesCardApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenThrow(new WebClientResponseException(503, "SERVICE UNAVAILABLE", null, null, null));
    assertThrows(SoamRuntimeException.class, () -> this.restUtils.getServicesCard("12345", correlationID));
  }

  @Test
  public void testGetServicesCard_givenAPICall404_shouldReturnOptionalEmpty() {
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getServicesCardApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenThrow(new WebClientResponseException(404, "NOT FOUND", null, null, null));
    val response = this.restUtils.getServicesCard("12345", correlationID);
    assertThat(response).isEmpty();
  }

  @Test
  public void getStudentByStudentID_givenAPICall404_shouldThrowException() {
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getStudentApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenThrow(new WebClientResponseException(404, "NOT FOUND", null, null, null));
    assertThrows(SoamRuntimeException.class, () -> this.restUtils.getStudentByStudentID("12345", correlationID));
  }

  @Test
  public void getStudentByStudentID_givenAPICall503_shouldThrowException() {
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getStudentApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenThrow(new WebClientResponseException(HttpStatus.SERVICE_UNAVAILABLE.value(),
        HttpStatus.SERVICE_UNAVAILABLE.toString(), null, null, null));
    assertThrows(SoamRuntimeException.class, () -> this.restUtils.getStudentByStudentID("12345", correlationID));
  }

  @Test
  public void getStudentByStudentID_givenAPICallSuccess_shouldReturnStudent() {
    val studentID = UUID.randomUUID();
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getStudentApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(StudentEntity.class))
      .thenReturn(Mono.just(this.createStudentEntity(studentID)));
    val response = this.restUtils.getStudentByStudentID(studentID.toString(), correlationID);
    assertThat(response).isNotNull();
    assertThat(response.getPen()).isEqualTo("123456789");
  }

  @Test
  public void createDigitalID_givenAPICallSuccess_shouldNotDoAnything() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getDigitalIdentifierApiURL()))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.header(any(), any()))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class))).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.retrieve()).thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
      .thenReturn(Mono.just(responseEntity));
    val response = this.restUtils.createDigitalID("BCeId", "12345", correlationID);
    assertThat(response).isNotNull();
    assertThat(response.getDigitalID()).isNotNull();
    assertThat(response.getIdentityValue()).isEqualTo("12345", correlationID);
  }

  @Test
  public void createDigitalID_givenAPICallError_shouldThrowException() {
    when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getDigitalIdentifierApiURL()))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.header(any(), any()))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class))).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.retrieve())
      .thenThrow(new WebClientResponseException(HttpStatus.SERVICE_UNAVAILABLE.value(),
        HttpStatus.SERVICE_UNAVAILABLE.toString(), null, null, null));
    assertThrows(SoamRuntimeException.class, () -> this.restUtils.createDigitalID("BCeId", "12345", correlationID));
  }

  @Test
  public void updateDigitalID_givenAPICallSuccess_shouldNotDoAnything() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.headers(any()))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class))).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.retrieve()).thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
      .thenReturn(Mono.just(responseEntity));
    val did = this.createDigitalIdentity();
    did.setDigitalID(UUID.randomUUID());
    this.restUtils.updateDigitalID(did, correlationID);
    verify(this.webClient.put(), times(1)).uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class));
  }

  @SneakyThrows
  @Test
  public void updateDigitalID_givenAPICallError_shouldThrowException() {
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.headers(any()))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class))).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.retrieve())
      .thenThrow(new WebClientResponseException(HttpStatus.SERVICE_UNAVAILABLE.value(),
        HttpStatus.SERVICE_UNAVAILABLE.toString(), null, null, null));
    val did = this.createDigitalIdentity();
    did.setDigitalID(UUID.randomUUID());
    assertThrows(SoamRuntimeException.class, () -> this.restUtils.updateDigitalID(did, correlationID));
  }


  @Test
  public void createServicesCard_givenAPICallSuccess_shouldNotDoAnything() {
    when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getServicesCardApiURL()))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.header(any(), any()))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class))).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.retrieve()).thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
      .thenReturn(Mono.just(this.createServiceCardEntity()));
    this.restUtils.createServicesCard(this.createServiceCardEntity(), correlationID);
    verify(this.webClient.post(), times(1)).uri(this.props.getServicesCardApiURL());
  }

  @Test
  public void createServicesCard_givenAPICallError_shouldThrowException() {
    when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getServicesCardApiURL()))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.header(any(), any()))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class))).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.retrieve())
      .thenThrow(new WebClientResponseException(HttpStatus.SERVICE_UNAVAILABLE.value(),
        HttpStatus.SERVICE_UNAVAILABLE.toString(), null, null, null));
    assertThrows(SoamRuntimeException.class, () -> this.restUtils.createServicesCard(this.createServiceCardEntity(), correlationID));
  }

  @Test
  public void updateServicesCard_givenAPICallSuccess_shouldNotDoAnything() {
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(this.props.getServicesCardApiURL()), any(Function.class)))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.headers(any()))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class))).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.retrieve()).thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
      .thenReturn(Mono.just(this.createServiceCardEntity()));
    val entity = this.createServiceCardEntity();
    entity.setServicesCardInfoID(UUID.randomUUID());
    this.restUtils.updateServicesCard(entity, correlationID);
    verify(this.webClient.put(), times(1)).uri(eq(this.props.getServicesCardApiURL()), any(Function.class));
  }

  @Test
  public void updateServicesCard_givenAPICallError_shouldThrowException() {
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(this.props.getServicesCardApiURL()), any(Function.class)))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.headers(any()))
      .thenReturn(this.requestBodyMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class))).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.retrieve())
      .thenThrow(new WebClientResponseException(HttpStatus.SERVICE_UNAVAILABLE.value(),
        HttpStatus.SERVICE_UNAVAILABLE.toString(), null, null, null));
    val entity = this.createServiceCardEntity();
    entity.setServicesCardInfoID(UUID.randomUUID());
    assertThrows(SoamRuntimeException.class, () -> this.restUtils.updateServicesCard(entity, correlationID));
  }

  @Test
  public void testGetStsLoginPrincipal_givenAPICall404_shouldReturnOptionalEmpty() {
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getStsApiURL() + "/"), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenThrow(new WebClientResponseException(404, "NOT FOUND", null, null, null));
    val response = this.restUtils.getStsLoginPrincipal("ABC", correlationID);
    assertThat(response).isEmpty();
  }

  @Test
  public void testGetStsLoginPrincipal_givenAPICallError_shouldThrowException() {
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getStsApiURL() + "/"), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenThrow(new WebClientResponseException(503, "SERVICE UNAVAILABLE", null, null, null));
    assertThrows(SoamRuntimeException.class, () -> this.restUtils.getStsLoginPrincipal("ABC", correlationID));
  }

  private StudentEntity createStudentEntity(final UUID studentId) {
    final StudentEntity.StudentEntityBuilder builder = StudentEntity.builder();
    builder.studentID(Objects.requireNonNullElseGet(studentId, UUID::randomUUID));
    builder.dob(LocalDate.now().toString());
    builder.legalFirstName("test").legalLastName("test").email("test@abc.com").genderCode("M").pen("123456789").sexCode("M");
    return builder.build();
  }

  private ServicesCardEntity createServiceCardEntity() {
    final ServicesCardEntity serviceCard = new ServicesCardEntity();
    serviceCard.setBirthDate("1984-11-02");
    serviceCard.setDid("DIGITALID");
    serviceCard.setEmail("abc@gmail.com");
    serviceCard.setGender("M");
    serviceCard.setIdentityAssuranceLevel("1");
    serviceCard.setGivenName("Given");
    serviceCard.setGivenNames(null);
    serviceCard.setPostalCode("V8W 2E1");
    serviceCard.setSurname("Surname");
    serviceCard.setUserDisplayName("displayName");
    return serviceCard;
  }

  private DigitalIDEntity createResponseEntity(final DigitalIDEntity entity) {
    final DigitalIDEntity responseEntity = new DigitalIDEntity();
    BeanUtils.copyProperties(entity, responseEntity);
    if (responseEntity.getDigitalID() == null) {
      responseEntity.setDigitalID(UUID.randomUUID());
    }
    return responseEntity;
  }

  private DigitalIDEntity createDigitalIdentity() {
    final DigitalIDEntity entity = new DigitalIDEntity();
    entity.setIdentityTypeCode("BCeId");
    entity.setIdentityValue("12345");
    entity.setLastAccessChannelCode("OSPR");
    entity.setLastAccessDate(LocalDateTime.now().toString());
    entity.setCreateUser("TESTMARCO");
    entity.setUpdateUser("TESTMARCO");
    return entity;
  }

  private PenMatchStudent createPenMatchStudent() {
    PenMatchStudent penMatchStudent = new PenMatchStudent();
    penMatchStudent.setPen("123456789");
    penMatchStudent.setDob("19980101");
    penMatchStudent.setSex("M");
    penMatchStudent.setSurname("SMITH");
    penMatchStudent.setGivenName("JOHN");
    penMatchStudent.setMiddleName("WAYNE");
    penMatchStudent.setPostal("V0H1A0");
    return penMatchStudent;
  }

  private PenMatchResult createPenMatchResponse() {
    PenMatchResult penMatchResult = new PenMatchResult();
    penMatchResult.setPenStatus("AA");
    return penMatchResult;
  }
}
