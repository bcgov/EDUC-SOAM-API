package ca.bc.gov.educ.api.soam.config;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SoamAPIMVCConfig implements WebMvcConfigurer {

  @Getter(AccessLevel.PRIVATE)
  private final RequestResponseInterceptor soamAPIReqRspInterceptor;

  @Autowired
  public SoamAPIMVCConfig(final RequestResponseInterceptor soamAPIReqRspInterceptor) {
    this.soamAPIReqRspInterceptor = soamAPIReqRspInterceptor;
  }

  @Override
  public void addInterceptors(final InterceptorRegistry registry) {
    registry.addInterceptor(this.soamAPIReqRspInterceptor).addPathPatterns("/**");
  }
}
