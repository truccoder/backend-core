package com.backend.core.security;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.backend.core.dtos.UserDto;

public class KeycloakAuthenticationToken extends AbstractAuthenticationToken {
  private final UserDto principal;
  private final Jwt jwt;

  public KeycloakAuthenticationToken(
      UserDto principal, Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    this.jwt = jwt;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return jwt.getTokenValue();
  }

  @Override
  public UserDto getPrincipal() {
    return principal;
  }
}
