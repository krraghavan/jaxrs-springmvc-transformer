package com.github.krr.springmvc.support.beans;

import org.springframework.lang.NonNull;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@SuppressWarnings({"WeakerAccess", "RestResourceMethodInspection", "unused"})
@Path(BaseClassWithPathAnnotation.BASE_URI)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public abstract class BaseClassWithPathAnnotation {

  public static final String BASE_URI = "/testUri";

  public static final String METHOD_WITH_PATH_ANNOTATION_IN_BASE_CLASS_METHOD = "methodWithPathAnnotationInBaseClass";

  public static final String METHOD_WITH_PATH_ANNOTATION_IN_BASE_CLASS_PATH = "/" + METHOD_WITH_PATH_ANNOTATION_IN_BASE_CLASS_METHOD;

  public static final String METHOD_WITH_PARAMETER_ANNOTATION_IN_BASE_CLASS = "methodWithParameterAnnotationInBaseClass";

  public static final String METHOD_WITH_PARAMETER_ANNOTATION_IN_BASE_CLASS_PATH = "/" + METHOD_WITH_PARAMETER_ANNOTATION_IN_BASE_CLASS;

  public static final String PATH_PARAMETER = "/nfs_whitelist/{name:.+}";

  @Path(METHOD_WITH_PATH_ANNOTATION_IN_BASE_CLASS_PATH)
  @POST
  public String methodWithPathAnnotationInBaseClass() {
    return "hello";
  }

  @SuppressWarnings("RSReferenceInspection")
  @Path(PATH_PARAMETER)
  @GET
  public String methodWithParameterAnnotationInBaseClass(@PathParam(value = "name") String name, @NonNull String value) {
    return "xxx";
  }
}
