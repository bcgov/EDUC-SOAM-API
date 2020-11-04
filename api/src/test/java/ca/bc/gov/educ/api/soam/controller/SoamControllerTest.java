package ca.bc.gov.educ.api.soam.controller;

import ca.bc.gov.educ.api.soam.model.entity.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.model.entity.IdentityTypeCodeEntity;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.rest.RestUtils;
import ca.bc.gov.educ.api.soam.support.WithMockOAuth2Scope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class SoamControllerTest {

  /**
   * The Mock mvc.
   */
  private MockMvc mockMvc;


  @Autowired
  SoamController controller;

  @Autowired
  RestUtils restUtils;

  @Autowired
  RestTemplate restTemplate;

  @Autowired
  ApplicationProperties props;

  private final String guid = UUID.randomUUID().toString();

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(restTemplate.exchange(eq(props.getDigitalIdentifierApiURL() + "/identityTypeCodes"), eq(HttpMethod.GET), any(), eq(IdentityTypeCodeEntity[].class))).thenReturn(getIdentityTypeCodeMap());
  }

  @Test
  @WithMockOAuth2Scope(scope = "SOAM_LOGIN")
  public void performLogin_givenValidPayload_shouldReturnNoContent() throws Exception {

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("identifierType", "BASIC");
    map.add("identifierValue", guid);
    when(restTemplate.exchange(eq(props.getDigitalIdentifierApiURL() + "?identitytype=BASIC&identityvalue=" + guid.toUpperCase()), eq(HttpMethod.GET), any(), eq(DigitalIDEntity.class))).thenReturn(getDigitalIdentity());
    doNothing().when(restTemplate).put(eq(props.getDigitalIdentifierApiURL()), any(), any(), eq(DigitalIDEntity.class));

    this.mockMvc.perform(multipart("/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .params(map)
        .accept(MediaType.APPLICATION_FORM_URLENCODED)).andDo(print()).andExpect(status().isNoContent());

  }

  @Test
  @WithMockOAuth2Scope(scope = "SOAM_LOGIN")
  public void performLogin_givenValidPayloadWithServicesCard_shouldReturnNoContent() throws Exception {

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

    when(restTemplate.exchange(eq(props.getServicesCardApiURL() + "?did=" + guid.toUpperCase()), eq(HttpMethod.GET), any(),eq(ServicesCardEntity.class)))
        .thenReturn(createServiceCardEntity());
    when(restTemplate.exchange(eq(props.getDigitalIdentifierApiURL() + "?identitytype=BASIC&identityvalue=" + guid.toUpperCase()), eq(HttpMethod.GET), any(), eq(DigitalIDEntity.class))).thenReturn(getDigitalIdentity());
    doNothing().when(restTemplate).put(eq(props.getDigitalIdentifierApiURL()), any(), any(), eq(DigitalIDEntity.class));

    this.mockMvc.perform(multipart("/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .params(map)
        .accept(MediaType.APPLICATION_FORM_URLENCODED)).andDo(print()).andExpect(status().isNoContent());

  }

  @Test
  @WithMockOAuth2Scope(scope = "SOAM_LOGIN")
  public void getSoamLoginEntity_givenValidPayloadWithServicesCard_shouldReturnOk() throws Exception {

    when(restTemplate.exchange(eq(props.getServicesCardApiURL() + "?did=" + guid.toUpperCase()), eq(HttpMethod.GET), any(),eq(ServicesCardEntity.class)))
        .thenReturn(createServiceCardEntity());
    when(restTemplate.exchange(eq(props.getDigitalIdentifierApiURL() + "?identitytype=BASIC&identityvalue=" + guid.toUpperCase()), eq(HttpMethod.GET), any(), eq(DigitalIDEntity.class))).thenReturn(getDigitalIdentity());
    doNothing().when(restTemplate).put(eq(props.getDigitalIdentifierApiURL()), any(), any(), eq(DigitalIDEntity.class));

    this.mockMvc.perform(get("/BASIC/"+guid).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk());

  }

  private ResponseEntity<DigitalIDEntity> getDigitalIdentity() {
    DigitalIDEntity entity = DigitalIDEntity.builder()
        .identityTypeCode("BASIC")
        .identityValue(guid)
        .lastAccessChannelCode("OSPR")
        .lastAccessDate(LocalDateTime.now().toString())
        .build();

    return ResponseEntity.ok(entity);
  }

  ResponseEntity<IdentityTypeCodeEntity[]> getIdentityTypeCodeMap() {
    IdentityTypeCodeEntity[] identityTypeCodeEntities = new IdentityTypeCodeEntity[1];
    var identityTypes = new ArrayList<IdentityTypeCodeEntity>();
    identityTypes.add(IdentityTypeCodeEntity
        .builder()
        .effectiveDate(LocalDateTime.now().toString())
        .expiryDate(LocalDateTime.MAX.toString())
        .identityTypeCode("BASIC")
        .build());
    return ResponseEntity.ok(identityTypes.toArray(identityTypeCodeEntities));
  }
  private ResponseEntity<ServicesCardEntity> createServiceCardEntity() {
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
    return ResponseEntity.ok(serviceCard);
  }
}
