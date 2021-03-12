package ca.bc.gov.educ.api.soam.controller;

import ca.bc.gov.educ.api.soam.model.entity.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.model.entity.IdentityTypeCodeEntity;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class SoamControllerTest {

  /**
   * The Mock mvc.
   */
  @Autowired
  private MockMvc mockMvc;

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


  @Autowired
  WebClient webClient;

  @Autowired
  ApplicationProperties props;

  private final String guid = UUID.randomUUID().toString();

  @Before
  public void before() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void performLogin_givenValidPayload_shouldReturnNoContent() throws Exception {

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("identifierType", "BASIC");
    map.add("identifierValue", guid);
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    DigitalIDEntity entity=getDigitalIdentity();
    //change it webclient get
    //when(this.webClient.exchange(eq(props.getDigitalIdentifierApiURL() + "?identitytype=BASIC&identityvalue=" + guid.toUpperCase()), eq(HttpMethod.GET), any(), eq(DigitalIDEntity.class))).thenReturn(getDigitalIdentity());
    //doNothing().when(restTemplate).put(eq(props.getDigitalIdentifierApiURL()), any(), any(), eq(DigitalIDEntity.class));
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(entity));

//       .identityTypeCode("BASIC")
//            .identityValue(guid)
//            .lastAccessChannelCode("OSPR")
    assertThat(entity.getIdentityTypeCode()).isEqualTo("BASIC");
    assertThat(entity.getIdentityValue()).isEqualTo(guid);
    assertThat(entity.getLastAccessChannelCode()).isEqualTo("OSPR");

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
            .thenReturn(any());
    verify(this.webClient,atMost(invocations+1)).put();

    this.mockMvc.perform(multipart("/login")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "SOAM_LOGIN")))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(map)
            .accept(MediaType.APPLICATION_FORM_URLENCODED)).andDo(print()).andExpect(status().isNoContent());

  }
  @Test
  public void performLogin_givenValidPayloadWithServicesCard_shouldReturnNoContent() throws Exception {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("identifierType", "BASIC");
    map.add("identifierValue", guid.toUpperCase());
    map.add("did", guid.toUpperCase());
    map.add("birthDate","1984-11-02");
    map.add("city","Victoria");
    map.add("country","CAN");
    map.add("email","abc@gmail.com");
    map.add("gender","M");
    map.add("identityAssuranceLevel","1");
    map.add("givenName","Given");

    ServicesCardEntity servicesCardEntity=createServiceCardEntity();

  //  when(restTemplate.exchange(eq(props.getServicesCardApiURL() + "?did=" + guid.toUpperCase()), eq(HttpMethod.GET), any(),eq(ServicesCardEntity.class)))
   //   .thenReturn(createServiceCardEntity());
  //  when(restTemplate.exchange(eq(props.getDigitalIdentifierApiURL() + "?identitytype=BASIC&identityvalue=" + guid.toUpperCase()), eq(HttpMethod.GET), any(), eq(DigitalIDEntity.class))).thenReturn(getDigitalIdentity());
   // doNothing().when(restTemplate).put(eq(props.getDigitalIdentifierApiURL()), any(), any(), eq(DigitalIDEntity.class));
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getServicesCardApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenReturn(Mono.just(servicesCardEntity));

    assertThat(servicesCardEntity.getDid()).isEqualTo(guid.toUpperCase());
    assertThat(servicesCardEntity.getBirthDate()).isEqualTo("1984-11-02");
    assertThat(servicesCardEntity.getCity()).isEqualTo("Victoria");

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
            .thenReturn(any());
    verify(this.webClient,atMost(invocations+1)).put();
    this.mockMvc.perform(multipart("/login")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "SOAM_LOGIN")))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .params(map)
            .accept(MediaType.APPLICATION_FORM_URLENCODED)).andDo(print()).andExpect(status().isNoContent());

  }

  @Test
  public void getSoamLoginEntity_givenValidPayloadWithServicesCard_shouldReturnOk() throws Exception {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    //when(restTemplate.exchange(eq(props.getServicesCardApiURL() + "?did=" + guid.toUpperCase()), eq(HttpMethod.GET), any(),eq(ServicesCardEntity.class)))
    //    .thenReturn(createServiceCardEntity());
    //when(restTemplate.exchange(eq(props.getDigitalIdentifierApiURL() + "?identitytype=BASIC&identityvalue=" + guid.toUpperCase()), eq(HttpMethod.GET), any(), eq(DigitalIDEntity.class))).thenReturn(getDigitalIdentity());
    //doNothing().when(restTemplate).put(eq(props.getDigitalIdentifierApiURL()), any(), any(), eq(DigitalIDEntity.class));
    ServicesCardEntity servicesCardEntity= createServiceCardEntity();
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getServicesCardApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
            .thenReturn(Mono.just(servicesCardEntity));
    assertThat(servicesCardEntity.getDid()).isEqualTo(guid.toUpperCase());
    assertThat(servicesCardEntity.getBirthDate()).isEqualTo("1984-11-02");
    assertThat(servicesCardEntity.getCity()).isEqualTo("Victoria");

    DigitalIDEntity entity=getDigitalIdentity();
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(props.getDigitalIdentifierApiURL(),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any()))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
            .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
            .thenReturn(Mono.just(entity));

    assertThat(entity.getIdentityTypeCode()).isEqualTo("BASIC");
    assertThat(entity.getIdentityValue()).isEqualTo(guid);
    assertThat(entity.getLastAccessChannelCode()).isEqualTo("OSPR");

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
            .thenReturn(any());
    verify(this.webClient,atMost(invocations+1)).put();

    this.mockMvc.perform(get("/BASIC/"+guid)
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "SOAM_LOGIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andDo(print()).andExpect(status().isOk());
  }

  private DigitalIDEntity getDigitalIdentity() {
    DigitalIDEntity entity = DigitalIDEntity.builder()
        .identityTypeCode("BASIC")
        .identityValue(guid)
        .lastAccessChannelCode("OSPR")
        .lastAccessDate(LocalDateTime.now().toString())
        .build();

    return entity;
  }

  IdentityTypeCodeEntity[] getIdentityTypeCodeMap() {
    IdentityTypeCodeEntity[] identityTypeCodeEntities = new IdentityTypeCodeEntity[1];
    var identityTypes = new ArrayList<IdentityTypeCodeEntity>();
    identityTypes.add(IdentityTypeCodeEntity
        .builder()
        .effectiveDate(LocalDateTime.now().toString())
        .expiryDate(LocalDateTime.MAX.toString())
        .identityTypeCode("BASIC")
        .build());
    return identityTypes.toArray(identityTypeCodeEntities);
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

  private WebClient.RequestBodySpec returnMockBodySpec() {
    return this.requestBodyMock;
  }
}
