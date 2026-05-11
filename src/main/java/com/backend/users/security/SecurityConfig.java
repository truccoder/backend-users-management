package com.backend.users.security;

import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import com.backend.core.annotations.Anonymous;
import com.backend.core.security.CustomAccessDeniedHandler;
import com.backend.core.security.CustomAuthenticationEntryPoint;
import com.backend.core.security.JwtTokenAuthenticationFilter;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {
  private final JwtTokenAuthenticationFilter jwtTokenAuthenticationFilter;
  private final ReactiveUserDetailsService userDetailsService;
  private final CustomAuthenticationEntryPoint authenticationEntryPoint;
  private final CustomAccessDeniedHandler accessDeniedHandler;
  private final RequestMappingHandlerMapping handlerMapping;

  public SecurityConfig(
      JwtTokenAuthenticationFilter jwtTokenAuthenticationFilter,
      ReactiveUserDetailsService userDetailsService,
      CustomAuthenticationEntryPoint authenticationEntryPoint,
      CustomAccessDeniedHandler accessDeniedHandler,
      @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
    this.jwtTokenAuthenticationFilter = jwtTokenAuthenticationFilter;
    this.userDetailsService = userDetailsService;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
    this.handlerMapping = handlerMapping;
  }

  @Bean
  public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
    String[] anonymousPaths = resolveAnonymousPaths();

    http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .authorizeExchange(
            exchange -> {
              exchange
                  .pathMatchers(
                      "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**")
                  .permitAll();

              if (anonymousPaths.length > 0) {
                exchange.pathMatchers(anonymousPaths).permitAll();
              }

              exchange.anyExchange().authenticated();
            })
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterAt(jwtTokenAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);

    return http.build();
  }

  @Bean
  public ReactiveAuthenticationManager authenticationManager() {
    UserDetailsRepositoryReactiveAuthenticationManager authManager =
        new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
    authManager.setPasswordEncoder(passwordEncoder());
    return authManager;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  private String[] resolveAnonymousPaths() {
    return handlerMapping.getHandlerMethods().entrySet().stream()
        .filter(entry -> hasAnonymousAnnotation(entry.getValue()))
        .flatMap(
            entry -> {
              Set<PathPattern> patterns = entry.getKey().getPatternsCondition().getPatterns();
              return patterns.stream().map(PathPattern::getPatternString);
            })
        .distinct()
        .toArray(String[]::new);
  }

  private boolean hasAnonymousAnnotation(HandlerMethod method) {
    return method.hasMethodAnnotation(Anonymous.class)
        || method.getBeanType().isAnnotationPresent(Anonymous.class);
  }
}
