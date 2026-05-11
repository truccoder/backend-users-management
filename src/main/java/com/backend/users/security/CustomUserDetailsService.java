package com.backend.users.security;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.backend.users.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements ReactiveUserDetailsService {
  private final UserRepository userRepository;

  @Override
  public Mono<UserDetails> findByUsername(String email) {
    return userRepository
        .findByEmail(email)
        .cast(UserDetails.class)
        .switchIfEmpty(
            Mono.error(new UsernameNotFoundException("User not found with email: " + email)));
  }
}
