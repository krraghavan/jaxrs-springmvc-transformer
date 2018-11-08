package com.github.krr.springmvc.support.api;

import javax.ws.rs.Path;

@Path(InterfaceOnSuperclass.BASE_URI)
public interface InterfaceOnSuperclass {

  String BASE_URI = "/abc";
}
