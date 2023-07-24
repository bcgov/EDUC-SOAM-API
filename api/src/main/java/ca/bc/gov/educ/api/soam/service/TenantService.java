package ca.bc.gov.educ.api.soam.service;

import ca.bc.gov.educ.api.soam.exception.InvalidParameterException;
import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.rest.RestUtils;
import ca.bc.gov.educ.api.soam.struct.v1.tenant.TenantAccess;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TenantService {

    private final RestUtils restUtils;

    @Autowired
    public TenantService(final RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    @RateLimiter(name = "tenantAccess")
    public TenantAccess determineTenantAccess(final String clientID, final String tenantID, final String correlationID) {
        this.validateSearchParameters(clientID, tenantID);
        val didResponseFromAPI = this.restUtils.getTenantAccess(clientID, tenantID, correlationID);
        if (didResponseFromAPI.isPresent()) {
            return didResponseFromAPI.get();
        }
        throw new SoamRuntimeException("Could not retrieve tenant access for tenantID :: " + tenantID + " :: and clientID :: " +  clientID + " :: and correlationID :: " +  correlationID);
    }

    private void validateSearchParameters(final String clientID, final String tenantID) {
        if (StringUtils.isBlank(clientID)) {
            log.error("Invalid clientID :: {}", clientID);
            throw new InvalidParameterException("clientID");
        } else if (StringUtils.isBlank(tenantID)) {
            throw new InvalidParameterException("tenantID");
        }
    }
}
