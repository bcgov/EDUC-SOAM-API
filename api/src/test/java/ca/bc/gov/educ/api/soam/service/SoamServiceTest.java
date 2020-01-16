package ca.bc.gov.educ.api.soam.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.bc.gov.educ.api.soam.codetable.CodeTableUtils;

@SpringBootTest
public class SoamServiceTest {

    @Autowired
    SoamService service;

    @Autowired
    CodeTableUtils codeTableUtils;

    @Test
    public void createValidDigitalIdTest(){
		service.performLogin("BASIC","12345","TESTMARCO");
    }
    
    @Test
    public void loginAndGetSoamEntity(){
		service.performLogin("BASIC","123456","TESTMARCO");
		
		assertNotNull(service.getSoamLoginEntity("BASIC","123456"));
    }
    
    @Test
    public void testCodeTableGet(){
        assertNotNull(codeTableUtils.getAllAccessChannelCodes());
    }
}
