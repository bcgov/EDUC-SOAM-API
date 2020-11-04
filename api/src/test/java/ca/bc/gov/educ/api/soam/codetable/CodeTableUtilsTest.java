package ca.bc.gov.educ.api.soam.codetable;

import ca.bc.gov.educ.api.soam.model.entity.AccessChannelCodeEntity;
import ca.bc.gov.educ.api.soam.model.entity.IdentityTypeCodeEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.rest.RestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class CodeTableUtilsTest {


  @Autowired
  ApplicationProperties props;

  @Autowired
  CodeTableUtils codeTableUtils;

  @Autowired
  RestUtils restUtils;

  @Autowired
  RestTemplate restTemplate;

  @Test
  public void testGetAllAccessChannelCodes_givenApiCallSuccess_shouldReturnMap() {
    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(restTemplate.exchange(eq(props.getDigitalIdentifierApiURL() + "/accessChannelCodes"), eq(HttpMethod.GET), any(), eq(AccessChannelCodeEntity[].class))).thenReturn(getAccessChannelMap());
    var results = codeTableUtils.getAllAccessChannelCodes();
    assertThat(results).size().isEqualTo(1);
    verify(restTemplate, atLeastOnce()).exchange(eq(props.getDigitalIdentifierApiURL() + "/accessChannelCodes"), eq(HttpMethod.GET), any(), eq(AccessChannelCodeEntity[].class));
  }

  @Test
  public void getAllIdentifierTypeCodes_givenApiCallSuccess_shouldReturnMap() {
    when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    when(restTemplate.exchange(eq(props.getDigitalIdentifierApiURL() + "/identityTypeCodes"), eq(HttpMethod.GET), any(), eq(IdentityTypeCodeEntity[].class))).thenReturn(getIdentityTypeCodeMap());
    var results = codeTableUtils.getAllIdentifierTypeCodes();
    assertThat(results).size().isEqualTo(0);
    verify(restTemplate, atLeastOnce()).exchange(eq(props.getDigitalIdentifierApiURL() + "/identityTypeCodes"), eq(HttpMethod.GET), any(), eq(IdentityTypeCodeEntity[].class));
  }

  ResponseEntity<AccessChannelCodeEntity[]> getAccessChannelMap() {
    AccessChannelCodeEntity[] accessChannelCodeEntities = new AccessChannelCodeEntity[1];
    var accessChannelCodes = new ArrayList<AccessChannelCodeEntity>();
    accessChannelCodes.add(AccessChannelCodeEntity
        .builder()
        .effectiveDate(LocalDateTime.now().toString())
        .expiryDate(LocalDateTime.MAX.toString())
        .accessChannelCode("OSPR")
        .build());
    return ResponseEntity.ok(accessChannelCodes.toArray(accessChannelCodeEntities));
  }

  ResponseEntity<IdentityTypeCodeEntity[]> getIdentityTypeCodeMap() {
    IdentityTypeCodeEntity[] identityTypeCodeEntities = new IdentityTypeCodeEntity[0];
    return ResponseEntity.ok(new ArrayList<IdentityTypeCodeEntity>().toArray(identityTypeCodeEntities));
  }
}
