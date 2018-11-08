package com.github.krr.springmvc.support.beans;

import javax.ws.rs.Path;

@SuppressWarnings({"WeakerAccess", "RestResourceMethodInspection"})
@Path(BaseClassWithPathAnnotation.BASE_URI)
public abstract class BaseClassWithPathAnnotation {

  public static final String BASE_URI = "/testUri";
}
