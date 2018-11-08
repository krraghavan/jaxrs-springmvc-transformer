package com.ntnx.springmvc.javassist.beans;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.lang.annotation.Annotation;
import java.util.List;

@Slf4j
@Data
public class MethodAnnotationContainer {

  private Path pathAnnotation;

  private Produces producesAnnotation;

  private Consumes consumesAnnotation;

  private HttpMethod method;

  /**
   * The annotations associated with each parameter of the method.
   */
  private List<List<Annotation>> paramAnnotation;

  public Class getSpringMvcMethodMapping(HttpMethod method) {
    if(HttpMethod.GET.equals(method.value())) {
      return GetMapping.class;
    }
    if(HttpMethod.PUT.equals(method.value())) {
      return PutMapping.class;
    }
    if(HttpMethod.POST.equals(method.value())) {
      return PostMapping.class;
    }
    if(HttpMethod.PATCH.equals(method.value())) {
      return PatchMapping.class;
    }
    if(HttpMethod.DELETE.equals(method.value())) {
      return DeleteMapping.class;
    }
    throw new IllegalArgumentException("Unsupported HttpMethod " + method);
  }

}
