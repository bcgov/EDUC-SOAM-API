package ca.bc.gov.educ.api.soam.service;

import ca.bc.gov.educ.api.soam.codetable.CodeTableUtils;
import ca.bc.gov.educ.api.soam.exception.InvalidParameterException;
import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.model.SoamServicesCard;
import ca.bc.gov.educ.api.soam.model.SoamStudent;
import ca.bc.gov.educ.api.soam.model.entity.*;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.rest.RestUtils;
import ca.bc.gov.educ.api.soam.util.SoamUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class SoamServiceTest {

  @Autowired
  SoamService service;


  @Autowired
  ApplicationProperties props;

  @MockBean
  CodeTableUtils codeTableUtils;


  @MockBean
  RestUtils restUtils;

  @Autowired
  SoamUtil soamUtil;


  @Before
  public void setUp() {
    openMocks(this);
  }

  @Test
  public void testPerformLogin_GivenIdentifierTypeNull_ThrowsInvalidParameterException() {
    assertThrows(InvalidParameterException.class, () -> this.service.performLogin(null, "12345", null));
  }

  @Test
  public void testPerformLogin_GivenIdentifierValueNull_ThrowsInvalidParameterException() {
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    assertThrows(InvalidParameterException.class, () -> this.service.performLogin("BCeId", null, null));
  }

  @Test
  public void testPerformLogin_GivenIdentifierValueBlank_ThrowsInvalidParameterException() {
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    assertThrows(InvalidParameterException.class, () -> this.service.performLogin("BCeId", "", null));
  }

  @Test
  public void testPerformLogin_GivenIdentifierTypeNotInCodeTable_ThrowsInvalidParameterException() {
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createBlankDummyIdentityTypeMap());
    assertThrows(InvalidParameterException.class, () -> this.service.performLogin("BCS", "12345", null));
  }

  @Test
  public void testPerformLogin_GivenDigitalIdGetCallFailed_ShouldThrowSoamRuntimeException() {
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenThrow(new SoamRuntimeException("503 SERVICE " +
        "UNAVAILABLE"));
    assertThrows(SoamRuntimeException.class, () -> this.service.performLogin("BCeId", "12345", null));
    verify(this.restUtils, atLeast(1)).getDigitalID("BCeId", "12345");
  }


  @Test
  public void testPerformLogin_GivenDigitalIdPostCallFailed_ShouldThrowSoamRuntimeException() {
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.empty());
    when(this.restUtils.createDigitalID(anyString(), anyString())).thenThrow(new SoamRuntimeException("503 SERVICE " +
        "UNAVAILABLE"));
    assertThrows(SoamRuntimeException.class, () -> this.service.performLogin("BCeId", "12345", null));
    verify(this.restUtils, atLeast(1)).getDigitalID("BCeId", "12345");
    verify(this.restUtils, atLeast(1)).createDigitalID("BCeId", "12345");
  }


  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardCreationFailed_ShouldThrowSoamRuntimeException() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    final ServicesCardEntity servicesCardEntity = this.createServiceCardEntity();
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.of(responseEntity));
    when(this.restUtils.getServicesCard(anyString())).thenReturn(Optional.empty());
    doNothing().when(this.restUtils).updateDigitalID(any());
    doThrow(new SoamRuntimeException("503 SERVICE " +
        "UNAVAILABLE")).when(this.restUtils).createServicesCard(any());
    assertThrows(SoamRuntimeException.class, () -> this.service.performLogin("BCeId", "12345", servicesCardEntity));
    verify(this.restUtils, atLeast(1)).getServicesCard("DIGITALID");
    verify(this.restUtils, atLeast(1)).getDigitalID("BCeId", "12345");
    verify(this.restUtils, atLeastOnce()).updateDigitalID(any());
    verify(this.restUtils, atLeast(1)).createServicesCard(any());
  }


  @Test
  public void testPerformLogin_GivenDigitalIdDoesNotExistAndServiceCardIsNull_ShouldCreateDigitalId() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.empty());
    when(this.restUtils.createDigitalID(anyString(), anyString())).thenReturn(entity);
    this.service.performLogin("BCeId", "12345", null);
    verify(this.restUtils, atMostOnce()).createDigitalID("BCeId", "12345");
    verify(this.restUtils, atMostOnce()).getDigitalID("BCeId", "12345");
    verify(this.restUtils, never()).updateDigitalID(any());
  }

  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardIsNull_ShouldUpdateDigitalId() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.of(responseEntity));
    doNothing().when(this.restUtils).updateDigitalID(any());
    this.service.performLogin("BCeId", "12345", null);
    verify(this.restUtils, never()).createDigitalID("BCeId", "12345");
    verify(this.restUtils, atMostOnce()).getDigitalID("BCeId", "12345");
    verify(this.restUtils, atMostOnce()).updateDigitalID(any());
  }


  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardDoesNot_ShouldUpdateDigitalIdAndCreateServicesCard() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    final ServicesCardEntity servicesCardEntity = this.createServiceCardEntity();
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.of(responseEntity));
    doNothing().when(this.restUtils).updateDigitalID(any());
    when(this.restUtils.getServicesCard(anyString())).thenReturn(Optional.empty());
    doNothing().when(this.restUtils).createServicesCard(any());
    this.service.performLogin("BCeId", "12345", servicesCardEntity);
    verify(this.restUtils, never()).createDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).getDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).updateDigitalID(any());
    verify(this.restUtils, times(1)).createServicesCard(any());
    verify(this.restUtils, times(1)).getServicesCard("DIGITALID");
  }


  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardExist_ShouldUpdateDigitalIdAndServicesCard() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    final ServicesCardEntity servicesCardEntity = this.createServiceCardEntity();
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.of(responseEntity));
    doNothing().when(this.restUtils).updateDigitalID(any());
    when(this.restUtils.getServicesCard(anyString())).thenReturn(Optional.of(servicesCardEntity));
    doNothing().when(this.restUtils).updateServicesCard(any());

    this.service.performLogin("BCeId", "12345", servicesCardEntity);
    verify(this.restUtils, never()).createDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).getDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).updateDigitalID(any());
    verify(this.restUtils, never()).createServicesCard(any());
    verify(this.restUtils, times(1)).getServicesCard("DIGITALID");
    verify(this.restUtils, times(1)).updateServicesCard(any());
  }


  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardGetCallFailed_ShouldThrowSoamRuntimeException() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    final ServicesCardEntity servicesCardEntity = this.createServiceCardEntity();
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.of(responseEntity));
    doNothing().when(this.restUtils).updateDigitalID(any());
    doThrow(new SoamRuntimeException("503 SERVICE " +
        "UNAVAILABLE")).when(this.restUtils).getServicesCard(anyString());

    assertThrows(SoamRuntimeException.class, () -> this.service.performLogin("BCeId", "12345", servicesCardEntity));
    verify(this.restUtils, never()).createDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).getDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).updateDigitalID(any());
    verify(this.restUtils, never()).createServicesCard(any());
    verify(this.restUtils, times(1)).getServicesCard("DIGITALID");
    verify(this.restUtils, never()).updateServicesCard(any());

  }


  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardPostCallFailed_ShouldThrowSoamRuntimeException() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    final ServicesCardEntity servicesCardEntity = this.createServiceCardEntity();
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.of(responseEntity));
    doNothing().when(this.restUtils).updateDigitalID(any());
    when(this.restUtils.getServicesCard(anyString())).thenReturn(Optional.empty());
    doThrow(new SoamRuntimeException("503 SERVICE " +
        "UNAVAILABLE")).when(this.restUtils).createServicesCard(any());
    assertThrows(SoamRuntimeException.class, () -> this.service.performLogin("BCeId", "12345", servicesCardEntity));
    verify(this.restUtils, never()).createDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).getDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).updateDigitalID(any());
    verify(this.restUtils, times(1)).createServicesCard(any());
    verify(this.restUtils, times(1)).getServicesCard("DIGITALID");
    verify(this.restUtils, never()).updateServicesCard(any());

  }


  @Test
  public void testPerformLogin_GivenDigitalIdExistAndServiceCardUpdateCallFailed_ShouldThrowSoamRuntimeException() {
    final DigitalIDEntity entity = this.createDigitalIdentity();
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    final ServicesCardEntity servicesCardEntity = this.createServiceCardEntity();
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.of(responseEntity));
    doNothing().when(this.restUtils).updateDigitalID(any());
    when(this.restUtils.getServicesCard(anyString())).thenReturn(Optional.of(servicesCardEntity));
    doThrow(new SoamRuntimeException("503 SERVICE " +
        "UNAVAILABLE")).when(this.restUtils).updateServicesCard(any());
    assertThrows(SoamRuntimeException.class, () -> this.service.performLogin("BCeId", "12345", servicesCardEntity));
    verify(this.restUtils, never()).createDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).getDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).updateDigitalID(any());
    verify(this.restUtils, never()).createServicesCard(any());
    verify(this.restUtils, times(1)).getServicesCard("DIGITALID");
    verify(this.restUtils, times(1)).updateServicesCard(any());
  }

  @Test
  public void testPerformLogin_GivenDigitalIdAndServiceCardDoesNotExist_ShouldCreateBothRecords() {
    final ServicesCardEntity servicesCardEntity = this.createServiceCardEntity();
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.empty());
    when(this.restUtils.createDigitalID(anyString(), anyString())).thenReturn(this.createDigitalIdentity());
    when(this.restUtils.getServicesCard(anyString())).thenReturn(Optional.empty());
    doNothing().when(this.restUtils).createServicesCard(any());
    this.service.performLogin("BCeId", "12345", servicesCardEntity);
    verify(this.restUtils, times(1)).createDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).getDigitalID("BCeId", "12345");
    verify(this.restUtils, never()).updateDigitalID(any());
    verify(this.restUtils, times(1)).createServicesCard(any());
    verify(this.restUtils, times(1)).getServicesCard("DIGITALID");
    verify(this.restUtils, never()).updateServicesCard(any());
  }


  @Test
  public void testGetSoamLoginEntity_GivenDigitalIdGetCallNotFound_ShouldThrowSoamRuntimeException() {
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.empty());
    assertThrows(SoamRuntimeException.class, () -> this.service.getSoamLoginEntity("BCeId", "12345"));
    verify(this.restUtils, times(1)).getDigitalID("BCeId", "12345");
  }


  @Test
  public void testGetSoamLoginEntity_GivenDigitalIdGetCallReturnsBlankResponse_ShouldThrowSoamRuntimeException() {
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    doThrow(new SoamRuntimeException("Unexpected HTTP return code: 500 error message: null body from digitalID get " +
        "call.")).when(this.restUtils).getDigitalID(anyString(), anyString());
    assertThrows(SoamRuntimeException.class, () -> this.service.getSoamLoginEntity("BCeId", "12345"));
    verify(this.restUtils, times(1)).getDigitalID("BCeId", "12345");

  }


  @Test
  public void testGetSoamLoginEntity_GivenDigitalIdGetCallFailed_ShouldThrowSoamRuntimeException() {
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    doThrow(new SoamRuntimeException("Unexpected HTTP return code: 503 error message: SERVICE UNAVAILABLE ")).when(this.restUtils).getDigitalID(anyString(), anyString());
    assertThrows(SoamRuntimeException.class, () -> this.service.getSoamLoginEntity("BCeId", "12345"));
    verify(this.restUtils, times(1)).getDigitalID("BCeId", "12345");
  }


  @Test
  public void testGetSoamLoginEntity_GivenDigitalIdGetCallSuccessForBCeIdWithoutStudentId_ShouldReturnEntityWithoutStudentAndServicesCard() {
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    final UUID digitalId = UUID.randomUUID();
    final DigitalIDEntity entity = this.createDigitalIdentity();
    entity.setDigitalID(digitalId);
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.of(responseEntity));

    final SoamLoginEntity soamLoginEntity = this.service.getSoamLoginEntity("BCeId", "12345");
    verify(this.restUtils, times(1)).getDigitalID("BCeId", "12345");
    assertNotNull(soamLoginEntity.getDigitalIdentityID());
    assertThat(soamLoginEntity.getDigitalIdentityID()).isEqualTo(digitalId);
    assertNull(soamLoginEntity.getServiceCard());
    assertNull(soamLoginEntity.getStudent());

  }

  @Test
  public void testGetSoamLoginEntity_GivenBCeIdAssociatedToStudent_ShouldReturnEntityWithStudent() {
    final UUID digitalId = UUID.randomUUID();
    final UUID studentId = UUID.randomUUID();
    final DigitalIDEntity entity = this.createDigitalIdentity();
    entity.setDigitalID(digitalId);
    entity.setStudentID(studentId.toString());
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    final StudentEntity studentEntity = this.createStudentEntity(studentId);
    final StudentEntity studentResponseEntity = this.createStudentResponseEntity(studentEntity);
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    responseEntity.setStudentID(studentId.toString());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.of(responseEntity));
    when(this.restUtils.getStudentByStudentID(anyString())).thenReturn(studentResponseEntity);
    final SoamLoginEntity soamLoginEntity = this.service.getSoamLoginEntity("BCeId", "12345");
    verify(this.restUtils, times(1)).getDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).getStudentByStudentID(studentId.toString());
    assertNotNull(soamLoginEntity.getDigitalIdentityID());
    assertThat(soamLoginEntity.getDigitalIdentityID()).isEqualTo(digitalId);
    assertNull(soamLoginEntity.getServiceCard());
    assertNotNull(soamLoginEntity.getStudent());
    assertNotNull(soamLoginEntity.getStudent().getDob());
    assertThat(soamLoginEntity.getStudent().getLegalLastName()).isEqualTo("test");
  }

  @Test
  public void testGetSoamLoginEntity_GivenBCeIdNotAssociatedToStudent_ShouldThrowSoamRuntimeException() {
    final UUID digitalId = UUID.randomUUID();
    final UUID studentId = UUID.randomUUID();
    final DigitalIDEntity entity = this.createDigitalIdentity();
    entity.setDigitalID(digitalId);
    entity.setStudentID(studentId.toString());
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    responseEntity.setStudentID(studentId.toString());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.of(responseEntity));
    doThrow(new SoamRuntimeException("Unexpected HTTP return code: 404 error message: NOT FOUND ")).when(this.restUtils).getStudentByStudentID(anyString());
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    assertThrows(SoamRuntimeException.class, () -> this.service.getSoamLoginEntity("BCeId", "12345"));
    verify(this.restUtils, times(1)).getDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).getStudentByStudentID(studentId.toString());
  }

  @Test
  public void testGetSoamLoginEntity_GivenStudentAPICallFailed_ShouldThrowSoamRuntimeException() {
    final UUID digitalId = UUID.randomUUID();
    final UUID studentId = UUID.randomUUID();
    final DigitalIDEntity entity = this.createDigitalIdentity();
    entity.setDigitalID(digitalId);
    entity.setStudentID(studentId.toString());
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());


    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.of(responseEntity));
    doThrow(new SoamRuntimeException("Unexpected HTTP return code: 503 error message: SERVICE UNAVAILABLE ")).when(this.restUtils).getStudentByStudentID(anyString());
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    assertThrows(SoamRuntimeException.class, () -> this.service.getSoamLoginEntity("BCeId", "12345"));
    verify(this.restUtils, times(1)).getDigitalID("BCeId", "12345");
    verify(this.restUtils, times(1)).getStudentByStudentID(studentId.toString());


  }

  @Test
  public void testGetSoamLoginEntity_GivenBCSCAssociatedToStudent_ShouldReturnEntityWithStudentAndServiceCard() {
    final UUID digitalId = UUID.randomUUID();
    final UUID studentId = UUID.randomUUID();
    final DigitalIDEntity entity = this.createDigitalIdentity();
    entity.setDigitalID(digitalId);
    entity.setStudentID(studentId.toString());
    entity.setIdentityTypeCode("BCSC");
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    final StudentEntity studentEntity = this.createStudentEntity(studentId);
    final StudentEntity studentResponseEntity = this.createStudentResponseEntity(studentEntity);
    final ServicesCardEntity servicesCardEntity = this.createServiceCardEntity();
    final ServicesCardEntity servicesCardResponseEntity = this.createServicesCardResponseEntity(servicesCardEntity);
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    responseEntity.setStudentID(studentId.toString());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.of(responseEntity));
    when(this.restUtils.getStudentByStudentID(anyString())).thenReturn(studentResponseEntity);
    when(this.restUtils.getServicesCard(anyString())).thenReturn(Optional.of(servicesCardResponseEntity));
    final SoamLoginEntity soamLoginEntity = this.service.getSoamLoginEntity("BCSC", "12345");
    verify(this.restUtils, times(1)).getDigitalID("BCSC", "12345");
    verify(this.restUtils, times(1)).getStudentByStudentID(studentId.toString());
    verify(this.restUtils, times(1)).getServicesCard("12345");
    assertNotNull(soamLoginEntity.getDigitalIdentityID());
    assertThat(digitalId).isEqualTo(soamLoginEntity.getDigitalIdentityID());
    assertNotNull(soamLoginEntity.getServiceCard());
    assertNotNull(soamLoginEntity.getStudent());
    assertNotNull(soamLoginEntity.getStudent().getDob());
    assertThat(soamLoginEntity.getStudent().getLegalLastName()).isEqualTo("test");
  }

  @Test
  public void testGetSoamLoginEntity_GivenServicesCardAPICallFailed_ShouldThrowSoamRuntimeException() {
    final UUID digitalId = UUID.randomUUID();
    final UUID studentId = UUID.randomUUID();
    final DigitalIDEntity entity = this.createDigitalIdentity();
    entity.setDigitalID(digitalId);
    entity.setStudentID(studentId.toString());
    entity.setIdentityTypeCode("BCSC");
    final DigitalIDEntity responseEntity = this.createResponseEntity(entity);
    final StudentEntity studentEntity = this.createStudentEntity(studentId);
    final StudentEntity studentResponseEntity = this.createStudentResponseEntity(studentEntity);
    when(this.codeTableUtils.getAllIdentifierTypeCodes()).thenReturn(this.createDummyIdentityTypeMap());
    when(this.restUtils.getDigitalID(anyString(), anyString())).thenReturn(Optional.of(responseEntity));
    when(this.restUtils.getStudentByStudentID(anyString())).thenReturn(studentResponseEntity);
    doThrow(new SoamRuntimeException("Unexpected HTTP return code: 503 error message: SERVICE UNAVAILABLE ")).when(this.restUtils).getServicesCard(anyString());

    assertThrows(SoamRuntimeException.class, () -> this.service.getSoamLoginEntity("BCSC", "12345"));
    verify(this.restUtils, times(1)).getDigitalID("BCSC", "12345");
    verify(this.restUtils, times(1)).getServicesCard("12345");
  }

  private ServicesCardEntity createServicesCardResponseEntity(final ServicesCardEntity servicesCardEntity) {
    return servicesCardEntity;
  }

  private StudentEntity createStudentEntity(final UUID studentId) {
    final StudentEntity.StudentEntityBuilder builder = StudentEntity.builder();
    builder.studentID(Objects.requireNonNullElseGet(studentId, UUID::randomUUID));
    builder.dob(LocalDate.now().toString());
    builder.legalFirstName("test").legalLastName("test").email("test@abc.com").genderCode('M').pen("123456789").sexCode('M').dataSourceCode("MYED");
    return builder.build();
  }

  private StudentEntity createStudentResponseEntity(final StudentEntity entity) {
    return entity;
  }

  private DigitalIDEntity createResponseEntity(final DigitalIDEntity entity) {
    final DigitalIDEntity responseEntity = new DigitalIDEntity();
    BeanUtils.copyProperties(entity, responseEntity);
    if (responseEntity.getDigitalID() == null) {
      responseEntity.setDigitalID(UUID.randomUUID());
    }
    return responseEntity;
  }


  private Map<String, IdentityTypeCodeEntity> createDummyIdentityTypeMap() {
    final Map<String, IdentityTypeCodeEntity> identityTypeCodeEntityMap = new HashMap<>();
    identityTypeCodeEntityMap.put("BCeId", IdentityTypeCodeEntity.builder().identityTypeCode("BCeId").build());
    identityTypeCodeEntityMap.put("BCSC", IdentityTypeCodeEntity.builder().identityTypeCode("BCSC").build());
    return identityTypeCodeEntityMap;
  }


  private Map<String, IdentityTypeCodeEntity> createBlankDummyIdentityTypeMap() {
    return new HashMap<>();
  }

  protected DigitalIDEntity createDigitalIdentity() {
    final DigitalIDEntity entity = new DigitalIDEntity();
    entity.setIdentityTypeCode("BCeId");
    entity.setIdentityValue("12345");
    entity.setLastAccessChannelCode("OSPR");
    entity.setLastAccessDate(LocalDateTime.now().toString());
    entity.setCreateUser("TESTMARCO");
    entity.setUpdateUser("TESTMARCO");
    return entity;
  }

  private ServicesCardEntity createServiceCardEntity() {
    final ServicesCardEntity serviceCard = new ServicesCardEntity();
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

  public SoamLoginEntity createSoamLoginEntity(final StudentEntity student, final UUID digitalIdentifierID, final ServicesCardEntity serviceCardEntity) {
    final SoamLoginEntity entity = new SoamLoginEntity();

    this.setStudentEntity(student, entity);

    this.setServicesCard(digitalIdentifierID, serviceCardEntity, entity);

    entity.setDigitalIdentityID(digitalIdentifierID);

    return entity;
  }

  private void setServicesCard(final UUID digitalIdentifierID, final ServicesCardEntity serviceCardEntity, final SoamLoginEntity entity) {
    if (serviceCardEntity != null) {
      final SoamServicesCard serviceCard = new SoamServicesCard();
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

  private void setStudentEntity(final StudentEntity student, final SoamLoginEntity entity) {
    if (student != null) {
      final SoamStudent soamStudent = new SoamStudent();

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
}
