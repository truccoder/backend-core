package com.backend.core.annotations;

import java.beans.PropertyDescriptor;
import java.util.Objects;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConditionalNotNullValidator
    implements ConstraintValidator<ConditionalNotNull, Object> {
  private String fieldA;
  private String fieldB;

  @Override
  public void initialize(ConditionalNotNull annotation) {
    this.fieldA = annotation.fieldA();
    this.fieldB = annotation.fieldB();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    try {
      Object fieldAValue =
          new PropertyDescriptor(fieldA, value.getClass()).getReadMethod().invoke(value);
      Object fieldBValue =
          new PropertyDescriptor(fieldB, value.getClass()).getReadMethod().invoke(value);

      boolean case1 = Objects.nonNull(fieldAValue) && Objects.isNull(fieldBValue);
      boolean case2 = Objects.isNull(fieldAValue) && Objects.nonNull(fieldBValue);

      if (case1 || case2) {
        context
            .buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
            .addPropertyNode(fieldB)
            .addConstraintViolation();
        return false;
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
