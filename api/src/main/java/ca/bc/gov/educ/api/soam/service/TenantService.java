package ca.bc.gov.educ.api.soam.service;

import ca.bc.gov.educ.api.soam.codetable.CodeTableUtils;
import ca.bc.gov.educ.api.soam.exception.InvalidParameterException;
import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.model.entity.*;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.rest.RestUtils;
import ca.bc.gov.educ.api.soam.struct.v1.penmatch.PenMatchResult;
import ca.bc.gov.educ.api.soam.struct.v1.penmatch.PenMatchStudent;
import ca.bc.gov.educ.api.soam.struct.v1.tenant.TenantAccessEntity;
import ca.bc.gov.educ.api.soam.util.JsonUtil;
import ca.bc.gov.educ.api.soam.util.SoamUtil;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TenantService {

    private final RestUtils restUtils;

    @Autowired
    public TenantService(final RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    @RateLimiter(name = "tenantAccess")
    public TenantAccessEntity determineTenantAccess(final String clientID, final String tenantID, final String correlationID) {
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
