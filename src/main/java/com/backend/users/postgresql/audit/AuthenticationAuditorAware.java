package com.backend.users.postgresql.audit;

import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.stereotype.Component;

import com.backend.core.dtos.UserDto;
import com.backend.core.security.JwtTokenAuthenticationHolder;

import reactor.core.publisher.Mono;

@Component
public class AuthenticationAuditorAware implements ReactiveAuditorAware<String> {
  @Override
  public Mono<String> getCurrentAuditor() {
    return JwtTokenAuthenticationHolder.findAuthenticatedUser().map(UserDto::getId);
  }
}
