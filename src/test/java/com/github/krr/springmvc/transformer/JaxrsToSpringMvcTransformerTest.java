package com.github.krr.springmvc.transformer;

import com.github.krr.springmvc.support.api.InterfaceOnSuperclass;
import com.github.krr.springmvc.support.beans.BaseClassWithPathAnnotation;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.Loader;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import static com.github.krr.springmvc.support.api.ClusterAdministration.CLUSTER_URI_BASE_PATH;
import static com.github.krr.springmvc.support.beans.BaseClassWithPathAnnotation.*;
import static com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclass.METHOD1_METHOD_NAME;
import static com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclass.METHOD1_PATH_ANNOTATION_PATH;
import static java.util.Collections.singletonList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * The Java class loader will not let a class to be loaded twice.  Since
 * the transformer changes a class that has been loaded, all the assertions
 * in this class are done with a separate class loader and reflection to ensure
 * that the values have been transformed correctly.
 *
 */
@SuppressWarnings("SameParameterValue")
public class JaxrsToSpringMvcTransformerTest {

  private static final String BASE_PACKAGE_NAME = "com.github.krr";

  @Test
  public void mustAnnotateJaxrsInterfaceToSpringMvcInImplementation() throws Exception {

    // create a new classloader for each test to allow the tests to run in parallel and avoid
    // duplicate class loading issues for assertions.
    ClassPool pool = ClassPool.getDefault();
    ClassLoader testClassLoader = new Loader(ClassLoader.getSystemClassLoader(), pool);

    JaxrsToSpringMvcTransformer transformer = new JaxrsToSpringMvcTransformer(singletonList(BASE_PACKAGE_NAME));
    CtClass cc = pool.get("com.github.krr.springmvc.support.impl.ClusterAdministrationImpl");
    transformer.applyTransformations(cc);
    //assertions.
    Class clazz = cc.toClass(testClassLoader, null);
    Class<?> rmClass = testClassLoader.loadClass(RequestMapping.class.getName());
    @SuppressWarnings("unchecked")
    Annotation annotation = AnnotationUtils.findAnnotation(clazz, (Class)rmClass);
    Assert.assertNotNull(annotation);
    Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(annotation);
    validateAnnotationAttributes(annotationAttributes, "value", 1,
                                 (p) -> Assert.assertEquals(p[0], CLUSTER_URI_BASE_PATH));
    validateAnnotationAttributes(annotationAttributes, "produces", 1,
                                 (p) -> Assert.assertEquals(p[0], MediaType.APPLICATION_JSON));
    validateAnnotationAttributes(annotationAttributes, "consumes", 1,
                                 (p) -> Assert.assertEquals(p[0], MediaType.APPLICATION_JSON));
  }

  private void validateAnnotationAttributes(Map<String, Object> annotationAttributes, String attrName, int expectedLen,
                                            Consumer<String[]> expectedValue) {
    String[] values = (String[]) annotationAttributes.get(attrName);
    assertNotNull(values);
    validateClassAnnotations(values, expectedLen, expectedValue);
  }

  @Test
  public void mustProcessMethodAnnotations() throws Exception {
    CtClass.debugDump = "target/javassist.debug";

    JaxrsToSpringMvcTransformer transformer = new JaxrsToSpringMvcTransformer(singletonList(BASE_PACKAGE_NAME));
    ClassPool pool = ClassPool.getDefault();
    CtClass cc = pool.get("com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclass");
    Properties properties = new Properties();
    properties.setProperty(JaxrsToSpringMvcTransformer.PACKAGES_TO_INCLUDE_KEY, BASE_PACKAGE_NAME);
    transformer.configure(properties);
    transformer.applyTransformations(cc);

    ClassLoader loader = new Loader(ClassLoader.getSystemClassLoader(), pool);
    //assertions.
    CtClass baseCtClass = pool.get("com.github.krr.springmvc.support.beans.BaseClassWithPathAnnotation");
    Class baseClass = baseCtClass.toClass(loader, null);

    Class clazz = cc.toClass(loader, null);
    Class rmClass = loader.loadClass(RequestMapping.class.getName());
    @SuppressWarnings("unchecked")
    Annotation requestMapping = AnnotationUtils.findAnnotation(clazz, rmClass);
    Assert.assertNotNull(requestMapping);
    Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(requestMapping);
    validateAnnotationAttributes(annotationAttributes, "value", 1,
                                 (p) -> Assert.assertEquals(p[0], BaseClassWithPathAnnotation.BASE_URI));
    validateAnnotationAttributes(annotationAttributes, "produces", 1,
                                 (p) -> assertEquals(p[0], MediaType.APPLICATION_JSON));
    validateAnnotationAttributes(annotationAttributes, "produces", 1,
                                 (p) -> assertEquals(p[0], MediaType.APPLICATION_JSON));

    // get the class and check the method annotations.
    validateRequestMappingAnnotationOnMethod(clazz, METHOD1_METHOD_NAME,
                                             METHOD1_PATH_ANNOTATION_PATH, RequestMethod.PATCH);

    validateRequestMappingAnnotationOnMethod(baseClass, METHOD_WITH_PATH_ANNOTATION_IN_BASE_CLASS_METHOD,
                                             METHOD_WITH_PATH_ANNOTATION_IN_BASE_CLASS_PATH, RequestMethod.POST);

    validateRequestMappingAnnotationOnMethod(baseClass, METHOD_WITH_PARAMETER_ANNOTATION_IN_BASE_CLASS,
                                             PATH_PARAMETER, RequestMethod.GET,
                                             String.class, String.class);

    // validate method params
    Method method = ReflectionUtils.findMethod(baseClass, METHOD_WITH_PARAMETER_ANNOTATION_IN_BASE_CLASS, String.class,
                                               String.class);
    assertNotNull(method);
    Annotation[][] pathVariableAnnotations = method.getParameterAnnotations();
    // two params - first one should have PathVariable annotation
    assertNotNull(pathVariableAnnotations);
    assertEquals(pathVariableAnnotations.length, 2);
    Annotation [] firstParameterAnnotation = pathVariableAnnotations[0];
    assertNotNull(firstParameterAnnotation);
    assertEquals(firstParameterAnnotation.length, 2);
    // second annotation is PathVariable
    Annotation pathVariableAnnotation = firstParameterAnnotation[1];
    Class pvClass = Class.forName(PathVariable.class.getName(), true, loader);
    assertEquals(pathVariableAnnotation.annotationType(), pvClass);
    Map<String, Object> pvAnnotationAttrs = AnnotationUtils.getAnnotationAttributes(pathVariableAnnotation);
    assertEquals(pvAnnotationAttrs.get("value"), "name");

  }

  @SuppressWarnings("unchecked")
  @Test
  public void mustProcessJaxrsAnnotationsOnInterfaceClasses() throws Exception {
    CtClass.debugDump = "target/javassist.debug";
    JaxrsToSpringMvcTransformer transformer = new JaxrsToSpringMvcTransformer(singletonList(BASE_PACKAGE_NAME));
    ClassPool pool = ClassPool.getDefault();
    ClassLoader loader = new Loader(ClassLoader.getSystemClassLoader(), pool);

    CtClass cc = pool.get("com.github.krr.springmvc.support.beans.SubclassWithPathAnnotationInSuperclassesInterface");
    Properties properties = new Properties();
    properties.setProperty(JaxrsToSpringMvcTransformer.PACKAGES_TO_INCLUDE_KEY, BASE_PACKAGE_NAME);
    transformer.configure(properties);
    transformer.applyTransformations(cc);

    //assertions.
    Class clazz = cc.toClass(loader, null);

    Class rcClass = loader.loadClass(RestController.class.getName());
    Annotation restController = AnnotationUtils.findAnnotation(clazz, rcClass);
    Assert.assertNotNull(restController);
    Class rmClass = loader.loadClass(RequestMapping.class.getName());

    @SuppressWarnings("unchecked")
    Annotation requestMapping = AnnotationUtils.findAnnotation(clazz, rmClass);
    assertNotNull(requestMapping);
    Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(requestMapping);
    assertNotNull(requestMapping);
    validateAnnotationAttributes(annotationAttributes, "value", 1,
                                 (p) -> Assert.assertEquals(p[0], InterfaceOnSuperclass.BASE_URI));

    Class ifClass = loader.loadClass(InterfaceOnSuperclass.class.getName());
    // get the class and check the method annotations.
    Method methodInInterface = ReflectionUtils.findMethod(ifClass, "methodWithPathAnnotationInInterface");
    assertNotNull(methodInInterface);
    @SuppressWarnings("unchecked")
    Annotation requestMappingOnMethod = AnnotationUtils.findAnnotation(methodInInterface, rmClass);
    assertNotNull(requestMappingOnMethod);
    @SuppressWarnings("unchecked")
    Map<String, Object> methodAnnotationAttributes = AnnotationUtils.getAnnotationAttributes(requestMappingOnMethod);
    String[] pathValues = (String[]) methodAnnotationAttributes.get("value");
    assertEquals(1, pathValues.length);
    assertEquals(pathValues[0], InterfaceOnSuperclass.METHOD_WITH_PATH_ANNOTATION_IN_INTERFACE_PATH);
    Object methods = methodAnnotationAttributes.get("method");
    checkHttpMethod(loader, methods, RequestMethod.GET.name());
  }

  private void checkHttpMethod(ClassLoader loader, Object method, String expectedMethod) throws ClassNotFoundException {
    Class rmethodClass = Class.forName("[L".concat(RequestMethod.class.getName()).concat(";"), true, loader);
    assertNotNull(method);
    assertNotNull(rmethodClass);
    Object [] methods = (Object[]) rmethodClass.cast(method);

    assertEquals(methods.length, 1);
    assertEquals(methods[0].toString(), expectedMethod);
  }

  @SuppressWarnings("unchecked")
  private void validateRequestMappingAnnotationOnMethod(Class clazz, String actualMethod,
                                                        String expectedMethod, RequestMethod requestMethod,
                                                        Class... args) throws ClassNotFoundException {
    Annotation requestMappingOnMethod;
    Method method = ReflectionUtils.findMethod(clazz, actualMethod, args);
    assertNotNull(method);
    ClassLoader testClassLoader = clazz.getClassLoader();
    Class rmClass = Class.forName(RequestMapping.class.getName(), true, testClassLoader);
    requestMappingOnMethod = AnnotationUtils.findAnnotation(method, rmClass);
    Assert.assertNotNull(requestMappingOnMethod);
    Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(requestMappingOnMethod);
    Class stringArrayClass = Class.forName("[Ljava.lang.String;", true, testClassLoader);
    String [] paths = (String[]) stringArrayClass.cast(annotationAttributes.get("value"));
    assertEquals(1, paths.length);
    assertEquals(paths[0], expectedMethod);
    checkHttpMethod(testClassLoader, annotationAttributes.get("method"), requestMethod.toString());
  }

  private void validateClassAnnotations(String [] attrs, int length, Consumer<String[]> consumerFn) {
    assertEquals(attrs.length, length);
    consumerFn.accept(attrs);
  }

}