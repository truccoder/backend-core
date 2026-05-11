package com.backend.core.tsid;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TsidGenerator {
  private final TsidFactory tsidFactory;

  public String generate() {
    return tsidFactory.create().toString();
  }

  public OffsetDateTime extractCreatedAt(String tsidString) {
    return OffsetDateTime.ofInstant(Tsid.from(tsidString).getInstant(), ZoneOffset.UTC);
  }
}
