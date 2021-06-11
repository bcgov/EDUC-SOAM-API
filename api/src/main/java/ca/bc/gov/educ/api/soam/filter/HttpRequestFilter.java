package ca.bc.gov.educ.api.soam.filter;

import ca.bc.gov.educ.api.soam.config.MutableHttpServletRequest;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

@Component
public class HttpRequestFilter implements Filter {

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    Filter.super.init(filterConfig);
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest req = (HttpServletRequest) request;
    val mutableRequest = new MutableHttpServletRequest(req);
    if (StringUtils.isBlank(mutableRequest.getHeader("correlationID"))) {
      mutableRequest.putHeader("correlationID", UUID.randomUUID().toString());
    }
    chain.doFilter(mutableRequest, response);
  }

  @Override
  public void destroy() {
    Filter.super.destroy();
  }
}
