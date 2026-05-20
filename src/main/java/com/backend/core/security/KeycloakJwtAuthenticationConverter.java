package com.backend.core.security;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.backend.core.dtos.UserDto;

import reactor.core.publisher.Mono;

@Component
public class KeycloakJwtAuthenticationConverter
    implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

  @Override
  public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
    String id = jwt.getSubject();
    String email = jwt.getClaimAsString("email");
    String fullName = jwt.getClaimAsString("fullName");
    String profilePictureUrl = jwt.getClaimAsString("profilePictureUrl");
    UserDto userDto = new UserDto(id, email, fullName, profilePictureUrl);
    return Mono.just(new KeycloakAuthenticationToken(userDto, jwt, List.of()));
  }
}
