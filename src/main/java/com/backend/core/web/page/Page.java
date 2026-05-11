package com.backend.core.web.page;

import static java.util.Collections.emptyList;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Page<T> {
  private List<T> items;
  private Long totalElements;

  public static <T> Page<T> empty() {
    return new Page<>(emptyList(), 0L);
  }
}
