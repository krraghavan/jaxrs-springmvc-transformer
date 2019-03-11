package com.github.krr.springmvc.javassist.beans;

import javassist.CtClass;
import javassist.CtMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class CtAnnotation {

  private final CtClass annotatedClass;

  private CtMethod annotatedMethod;

  private final Annotation annotation;

}
