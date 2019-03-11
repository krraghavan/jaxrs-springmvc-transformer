package com.github.krr.springmvc.support.beans;

import javax.ws.rs.PATCH;
import javax.ws.rs.Path;

@SuppressWarnings({"RestResourceMethodInspection", "unused", "WeakerAccess"})
public class SubclassWithPathAnnotationInSuperclass extends BaseClassWithPathAnnotation {

  public static final String METHOD1_METHOD_NAME = "methodWithPathAnnotation";

  public static final String METHOD1_PATH_ANNOTATION_PATH = "/" + METHOD1_METHOD_NAME;

  @Path(METHOD1_PATH_ANNOTATION_PATH)
  @PATCH
  public String methodWithPathAnnotation() {
    return "hello";
  }
}
