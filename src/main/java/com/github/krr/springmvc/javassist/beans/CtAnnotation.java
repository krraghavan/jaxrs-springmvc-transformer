package com.github.krr.springmvc.javassist.beans;

import javassist.CtClass;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.annotation.Annotation;

@Data
@AllArgsConstructor
public class CtAnnotation {

  private CtClass annotatedClass;

  private Annotation annotation;
}
