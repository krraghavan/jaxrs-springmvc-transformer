package com.github.krr.springmvc.support.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@SuppressWarnings("RestResourceMethodInspection")
@Path(InterfaceOnSuperclass.BASE_URI)
public interface InterfaceOnSuperclass {

  String BASE_URI = "/abc";

  String METHOD_WITH_PATH_ANNOTATION_IN_INTERFACE_METHOD = "methodWithPathAnnotationInInterface";

  String METHOD_WITH_PATH_ANNOTATION_IN_INTERFACE_PATH = "/" + METHOD_WITH_PATH_ANNOTATION_IN_INTERFACE_METHOD;

  @Path(METHOD_WITH_PATH_ANNOTATION_IN_INTERFACE_PATH)
  @GET
  default String methodWithPathAnnotationInInterface() {
    return "hello";
  }
}
