package com.ntnx.springmvc.transformer;

import javassist.ClassPool;
import javassist.CtClass;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.util.function.Consumer;

import static com.ntnx.springmvc.support.api.ClusterAdministration.CLUSTER_URI_BASE_PATH;
import static java.util.Collections.singletonList;
import static org.testng.Assert.assertEquals;

public class JaxrsToSpringMvcTransformerTest {

  @Test
  public void mustAnnotateJaxrsInterfaceToSpringMvcInImplementation() throws Exception {

    JaxrsToSpringMvcTransformer transformer = new JaxrsToSpringMvcTransformer(singletonList("com.ntnx.springmvc"));
    ClassPool pool = ClassPool.getDefault();
    CtClass cc = pool.get("com.ntnx.springmvc.support.impl.ClusterAdministrationImpl");
    transformer.applyTransformations(cc);
    //assertions.
    Class clazz = cc.toClass();
    RequestMapping requestMapping = AnnotationUtils.findAnnotation(clazz, RequestMapping.class);
    Assert.assertNotNull(requestMapping);
    String[] paths = requestMapping.path();
    validateClassAnnotations(paths, 1, (p) -> assertEquals(p[0], CLUSTER_URI_BASE_PATH));
    String[] produces = requestMapping.produces();
    Assert.assertNotNull(produces);
    validateClassAnnotations(produces, 1, (p) -> assertEquals(p[0], MediaType.APPLICATION_JSON));
    String[] consumes = requestMapping.consumes();
    Assert.assertNotNull(consumes);
    validateClassAnnotations(consumes, 1, (p) -> assertEquals(p[0], MediaType.APPLICATION_JSON));
  }

  private void validateClassAnnotations(String [] attrs, int length, Consumer<String[]> consumerFn) {
    assertEquals(attrs.length, length);
    consumerFn.accept(attrs);
  }

}