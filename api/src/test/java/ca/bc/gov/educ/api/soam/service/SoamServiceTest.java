package ca.bc.gov.educ.api.soam.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.bc.gov.educ.api.soam.model.SoamLoginEntity;

@SpringBootTest
@Transactional
public class SoamServiceTest {

    @Autowired
    SoamService service;

    @Test
    public void createValidDigitalIdTest(){
        

        assertNotNull(service.createDigitalID(digitalID));
    }
}
