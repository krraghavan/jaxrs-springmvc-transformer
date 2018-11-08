package com.ntnx.springmvc.javassist.utils;

import com.ntnx.springmvc.support.api.ClusterAdministration;
import com.ntnx.springmvc.support.api.InterfaceOnSuperclass;
import com.ntnx.springmvc.support.beans.BaseClassWithPathAnnotation;
import com.ntnx.springmvc.javassist.beans.CtAnnotation;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.springframework.core.annotation.AnnotationUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.ntnx.springmvc.support.api.ClusterAdministration.*;
import static com.ntnx.springmvc.support.beans.BaseClassWithPathAnnotation.*;

public class CtClassUtilsTest {

  private static final List<String> PACKAGES_TO_SEARCH = Collections.singletonList("com.ntnx.springmvc");

  private ClassPool pool = ClassPool.getDefault();

  @Test
  public void mustFindJaxRsAnnotationOnInterface() throws Exception {

    CtClass cc = pool.get("com.ntnx.springmvc.support.impl.ClusterAdministrationImpl");
    CtAnnotation annotation = CtClassUtils.findAnnotation(cc, Path.class, PACKAGES_TO_SEARCH);
    validateAnnotation(annotation, ClusterAdministration.class.getName(), CLUSTER_URI_BASE_PATH);
  }

  @Test
  public void mustReturnNullIfClassToAnalyzeIsNotInWhiteList() throws Exception {

    CtClass cc = pool.get("java.lang.Object");
    CtAnnotation annotation = CtClassUtils.findAnnotation(cc, Path.class, PACKAGES_TO_SEARCH);
    Assert.assertNull(annotation);
  }

  @Test
  public void mustReturnNullIfClassOrSuperClassToAnalyzeIsNotInWhiteList() throws Exception {

    CtClass cc = pool.get("java.lang.String");
    CtAnnotation annotation = CtClassUtils.findAnnotation(cc, Path.class, PACKAGES_TO_SEARCH);
    Assert.assertNull(annotation);
  }

  @Test
  public void mustFindJaxrsAnnotationOnSuperclass() throws Exception {
    CtClass cc = pool.get("com.ntnx.springmvc.support.beans.SubclassWithPathAnnotationInSuperclass");
    CtAnnotation annotation = CtClassUtils.findAnnotation(cc, Path.class, PACKAGES_TO_SEARCH);
    validateAnnotation(annotation, BaseClassWithPathAnnotation.class.getName(), BASE_URI);
  }

  @Test
  public void mustFindJaxrsAnnotationInInterfaceOnSuperclass() throws Exception {
    CtClass cc = pool.get("com.ntnx.springmvc.support.beans.SubclassWithPathAnnotationInSuperclassesInterface");
    CtAnnotation annotation = CtClassUtils.findAnnotation(cc, Path.class, PACKAGES_TO_SEARCH);
    validateAnnotation(annotation, InterfaceOnSuperclass.class.getName(), InterfaceOnSuperclass.BASE_URI);
  }

  @Test
  public void mustFindJaxrsAnnotationInInterfaceOnSuperclassWhenCallingInterfaceMethod() throws Exception {
    CtClass cc = pool.get("com.ntnx.springmvc.support.beans.BaseClassWithPathAnnotationInInterface");
    CtAnnotation annotation = CtClassUtils.findAnnotationInInterfaces(cc, Path.class, PACKAGES_TO_SEARCH);
    validateAnnotation(annotation, InterfaceOnSuperclass.class.getName(), InterfaceOnSuperclass.BASE_URI);
  }

  @Test
  public void mustFindJaxrsAnnotationInInterfaceOnSuperclassWhenCallingInterfaceMethodAcrossAllPackages() throws Exception {
    CtClass cc = pool.get("com.ntnx.springmvc.support.beans.BaseClassWithPathAnnotationInInterface");
    CtAnnotation annotation = CtClassUtils.findAnnotationInInterfaces(cc, Path.class);
    validateAnnotation(annotation, InterfaceOnSuperclass.class.getName(), InterfaceOnSuperclass.BASE_URI);
  }

  @Test
  public void mustFindJaxrsAnnotationWhenNoInterfacesArePresent() throws Exception {
    CtClass cc = pool.get("com.ntnx.springmvc.support.beans.ClassWithNoJaxrsAnnotationsAndNoInterfaces");
    CtAnnotation annotation = CtClassUtils.findAnnotation(cc, Path.class, PACKAGES_TO_SEARCH);
    Assert.assertNull(annotation);
  }

  @Test
  public void mustFindJaxrsAnnotationWhenNoInterfacesArePresentOnSuperclassOrBaseClass() throws Exception {
    CtClass cc = pool.get("com.ntnx.springmvc.support.beans.SuperClasWithNoJaxrsAnnotationsAndNoInterfaces");
    CtAnnotation annotation = CtClassUtils.findAnnotation(cc, Path.class, PACKAGES_TO_SEARCH);
    Assert.assertNull(annotation);
  }

  @Test(expectedExceptions = NotFoundException.class)
  public void mustHandleClassesNotPresent() throws Exception {
    pool.get("com.ntnx.springmvc.support.beans.XYZ");
  }

  @Test
  public void mustWorkEvenIfWhiteListIsNull() throws Exception {
    CtClass cc = pool.get("com.ntnx.springmvc.support.beans.SubclassWithPathAnnotationInSuperclassesInterface");
    CtAnnotation annotation = CtClassUtils.findAnnotation(cc, Path.class);
    validateAnnotation(annotation, InterfaceOnSuperclass.class.getName(), InterfaceOnSuperclass.BASE_URI);
  }

  private void validateAnnotation(CtAnnotation annotation, String expectedAnnotatedClass, String annotationValue) {
    Assert.assertNotNull(annotation);
    Assert.assertEquals(annotation.getAnnotatedClass().getName(), expectedAnnotatedClass);
    Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(annotation.getAnnotation());
    Assert.assertNotNull(annotationAttributes);
    Assert.assertTrue(annotationAttributes.containsKey("value"));
    Assert.assertEquals(annotationAttributes.get("value"), annotationValue);
  }


}