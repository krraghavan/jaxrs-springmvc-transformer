package com.github.krr.springmvc.javassist.utils;

import com.github.krr.springmvc.support.api.ClusterAdministration;
import com.github.krr.springmvc.support.api.InterfaceOnSuperclass;
import com.github.krr.springmvc.support.beans.BaseClassWithPathAnnotation;
import com.github.krr.springmvc.javassist.beans.CtAnnotation;
import com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclass;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.springframework.core.annotation.AnnotationUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.ws.rs.Path;
import java.util.*;

import static com.github.krr.springmvc.support.api.ClusterAdministration.*;
import static com.github.krr.springmvc.support.api.InterfaceOnSuperclass.METHOD_WITH_PATH_ANNOTATION_IN_INTERFACE_METHOD;
import static com.github.krr.springmvc.support.api.InterfaceOnSuperclass.METHOD_WITH_PATH_ANNOTATION_IN_INTERFACE_PATH;
import static com.github.krr.springmvc.support.beans.BaseClassWithPathAnnotation.*;
import static com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclass.METHOD1_METHOD_NAME;
import static com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclass.METHOD1_PATH_ANNOTATION_PATH;

public class CtClassUtilsTest {

  private static final List<String> PACKAGES_TO_SEARCH = Collections.singletonList("com.github.krr.springmvc");

  private ClassPool pool = ClassPool.getDefault();

  @Test
  public void mustFindJaxRsAnnotationOnInterface() throws Exception {

    CtClass cc = pool.get("com.github.krr.springmvc.support.impl.ClusterAdministrationImpl");
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
    CtClass cc = pool.get("com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclass");
    CtAnnotation annotation = CtClassUtils.findAnnotation(cc, Path.class, PACKAGES_TO_SEARCH);
    validateAnnotation(annotation, BaseClassWithPathAnnotation.class.getName(), BASE_URI);
  }

  @Test
  public void mustFindJaxrsAnnotationInInterfaceOnSuperclass() throws Exception {
    CtClass cc = pool.get("com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclassesInterface");
    CtAnnotation annotation = CtClassUtils.findAnnotation(cc, Path.class, PACKAGES_TO_SEARCH);
    validateAnnotation(annotation, InterfaceOnSuperclass.class.getName(), InterfaceOnSuperclass.BASE_URI);
  }

  @Test
  public void mustFindJaxrsAnnotationInInterfaceOnSuperclassWhenCallingInterfaceMethod() throws Exception {
    CtClass cc = pool.get("com.github.krr.springmvc.support.beans.BaseClassWithPathAnnotationInInterface");
    CtAnnotation annotation = CtClassUtils.findAnnotationInInterfaces(cc, Path.class, PACKAGES_TO_SEARCH);
    validateAnnotation(annotation, InterfaceOnSuperclass.class.getName(), InterfaceOnSuperclass.BASE_URI);
  }

  @Test
  public void mustFindJaxrsAnnotationInInterfaceOnSuperclassWhenCallingInterfaceMethodAcrossAllPackages() throws Exception {
    CtClass cc = pool.get("com.github.krr.springmvc.support.beans.BaseClassWithPathAnnotationInInterface");
    CtAnnotation annotation = CtClassUtils.findAnnotationInInterfaces(cc, Path.class);
    validateAnnotation(annotation, InterfaceOnSuperclass.class.getName(), InterfaceOnSuperclass.BASE_URI);
  }

  @Test
  public void mustFindJaxrsAnnotationWhenNoInterfacesArePresent() throws Exception {
    CtClass cc = pool.get("com.github.krr.springmvc.support.beans.ClassWithNoJaxrsAnnotationsAndNoInterfaces");
    CtAnnotation annotation = CtClassUtils.findAnnotation(cc, Path.class, PACKAGES_TO_SEARCH);
    Assert.assertNull(annotation);
  }

  @Test
  public void mustFindJaxrsAnnotationWhenNoInterfacesArePresentOnSuperclassOrBaseClass() throws Exception {
    CtClass cc = pool.get("com.github.krr.springmvc.support.beans.SuperClassWithNoJaxrsAnnotationsAndNoInterfaces");
    CtAnnotation annotation = CtClassUtils.findAnnotation(cc, Path.class, PACKAGES_TO_SEARCH);
    Assert.assertNull(annotation);
  }

  @Test(expectedExceptions = NotFoundException.class)
  public void mustHandleClassesNotPresent() throws Exception {
    pool.get("com.github.krr.springmvc.support.beans.XYZ");
  }

  @Test
  public void mustWorkEvenIfWhiteListIsNull() throws Exception {
    CtClass cc = pool.get("com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclassesInterface");
    CtAnnotation annotation = CtClassUtils.findAnnotation(cc, Path.class);
    validateAnnotation(annotation, InterfaceOnSuperclass.class.getName(), InterfaceOnSuperclass.BASE_URI);
  }

  @Test
  public void mustFindJaxrsAnnotationOnMethodInInterface() throws Exception {
    CtClass cc = pool.get("com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclassesInterface");
    CtMethod [] methods = cc.getMethods();
    Optional<CtMethod> first = Arrays.stream(methods)
                                     .filter(m -> METHOD_WITH_PATH_ANNOTATION_IN_INTERFACE_METHOD.equals(m.getName()))
                                     .findFirst();
    Assert.assertTrue(first.isPresent());
    CtMethod methodToTest = first.get();
    Assert.assertNotNull(methodToTest, "Expecting non-null method: methodWithPathAnnotation");
    CtAnnotation annotation = CtClassUtils.findAnnotationOnMethod(methodToTest, Path.class);
    validateAnnotation(annotation, InterfaceOnSuperclass.class.getName(), METHOD_WITH_PATH_ANNOTATION_IN_INTERFACE_PATH);
    Assert.assertNotNull(annotation.getAnnotatedMethod());
    Assert.assertEquals(annotation.getAnnotatedMethod(), methodToTest);
  }

  @DataProvider
  public Object[][] methodDataProviders() {
    return new String[][] {
        new String[] {"com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclass",
                      METHOD_WITH_PATH_ANNOTATION_IN_BASE_CLASS_METHOD, METHOD_WITH_PATH_ANNOTATION_IN_BASE_CLASS_PATH,
                      "com.github.krr.springmvc.support.beans.BaseClassWithPathAnnotation"},
        new String[] {"com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclass",
                      METHOD1_METHOD_NAME, METHOD1_PATH_ANNOTATION_PATH, "com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclass"},
        new String[] {"com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclassesInterface",
                      METHOD_WITH_PATH_ANNOTATION_IN_INTERFACE_METHOD, METHOD_WITH_PATH_ANNOTATION_IN_INTERFACE_PATH,
                      "com.github.krr.springmvc.support.api.InterfaceOnSuperclass"}
    };
  }

  @Test(dataProvider = "methodDataProviders")
  public void mustFindAnnotationsOnMethod(String className, String methodName, String methodPath,
                                          String methodClassName) throws Exception {
    CtClass cc = pool.get(className);
    CtMethod [] methods = cc.getMethods();
    Optional<CtMethod> first = Arrays.stream(methods)
                                     .filter(m -> methodName.equals(m.getName()))
                                     .findFirst();
    Assert.assertTrue(first.isPresent());
    CtMethod methodToTest = first.get();
    Assert.assertNotNull(methodToTest, "Expecting non-null method: methodWithPathAnnotation");
    CtAnnotation annotation = CtClassUtils.findAnnotationOnMethod(methodToTest, Path.class);
    validateAnnotation(annotation, methodClassName, methodPath);
    Assert.assertNotNull(annotation.getAnnotatedMethod());
    Assert.assertEquals(annotation.getAnnotatedMethod(), methodToTest);

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