package com.github.krr.springmvc.transformer;

import com.github.krr.springmvc.javassist.beans.CtAnnotation;
import com.github.krr.springmvc.javassist.utils.CtClassUtils;
import de.icongmbh.oss.maven.plugin.javassist.ClassTransformer;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.*;
import javassist.bytecode.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.*;
import java.util.*;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static org.springframework.web.bind.annotation.RequestMethod.valueOf;

@SuppressWarnings({"WeakerAccess"})
@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class JaxrsToSpringMvcTransformer extends ClassTransformer {

  public static final String VALUE_ATTRIBUTE = "value";

  public static final String PACKAGES_TO_INCLUDE_KEY = "packagesToInclude";

  private List<String> packagesToInclude;

  private static final List<String> METHOD_PARAMETER_ANNOTATION_LIST = Arrays.asList(PathParam.class.getName(),
                                                                                     QueryParam.class.getName());

  private static final List<Class> JAXRS_HTTP_METHOD_ANNOTATIONS = Arrays.asList(GET.class, POST.class, PUT.class,
                                                                                 PATCH.class, DELETE.class);

  private static final Map<String, Class> JAXRS_SPRINGMVC_ANNOTATION_MAP = new HashMap<>();

  private final Set<CtClass> classFilesToSave = new LinkedHashSet<>();

  static {
    JAXRS_SPRINGMVC_ANNOTATION_MAP.put(PathParam.class.getName(), PathVariable.class);
    JAXRS_SPRINGMVC_ANNOTATION_MAP.put(QueryParam.class.getName(), RequestParam.class);
    JAXRS_SPRINGMVC_ANNOTATION_MAP.put(GET.class.getName(), GetMapping.class);
    JAXRS_SPRINGMVC_ANNOTATION_MAP.put(POST.class.getName(), PostMapping.class);
    JAXRS_SPRINGMVC_ANNOTATION_MAP.put(PUT.class.getName(), PutMapping.class);
    JAXRS_SPRINGMVC_ANNOTATION_MAP.put(DELETE.class.getName(), DeleteMapping.class);
    JAXRS_SPRINGMVC_ANNOTATION_MAP.put(PATCH.class.getName(), PatchMapping.class);
  }

  @SuppressWarnings("unused")
  public JaxrsToSpringMvcTransformer(List<String> packagesToInclude) {
    this.packagesToInclude = packagesToInclude;
  }

  @SuppressWarnings("unused")
  public JaxrsToSpringMvcTransformer() {
  }

  @Override
  protected void applyTransformations(CtClass ctClass) throws Exception {
    log.info("Transforming class {}", ctClass.getName());

    processClassLevelJaxrsAnnotations(ctClass);

    addMethodAnnotationsFromJaxrsAnnotations(ctClass);

    for (CtClass aClass : classFilesToSave) {
      aClass.writeFile();
    }
  }

  private void processClassLevelJaxrsAnnotations(CtClass ctClass) throws NotFoundException, ClassNotFoundException {

    log.info("Processing class level annotations {}", ctClass.getName());
    ClassFile classFile = ctClass.getClassFile();

    // Read class level annotations for JaxRs method
    CtAnnotation ctPathAnnotation = CtClassUtils.findAnnotation(ctClass, Path.class, packagesToInclude);
    CtAnnotation ctProducesAnnotation = CtClassUtils.findAnnotation(ctClass, Produces.class, packagesToInclude);
    CtAnnotation ctConsumesAnnotation = CtClassUtils.findAnnotation(ctClass, Consumes.class, packagesToInclude);

    if (ctPathAnnotation != null) {
      log.debug("Found JaxRs Path annotation on class - adding RequestMapping on implementation class");
      ConstPool constpool = classFile.getConstPool();

      AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) getVisibleRuntimeAnnotationAttr(constpool,
                                                                                                         classFile);
      // add RestController annotation to the implementation classes only.
      if (!ctClass.isInterface()) {
        Annotation rc = getAnnotation(constpool, RestController.class);
        annotationsAttribute.addAnnotation(rc);
        classFile.addAttribute(annotationsAttribute);
      }

      // create the annotation
      Path path = (Path) ctPathAnnotation.getAnnotation();
      log.trace("Processing Path annotation");
      AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
      Annotation rm = getAnnotation(constpool, RequestMapping.class);
      StringMemberValue smv = new StringMemberValue(path.value(), constpool);
      addAttributeToAnnotation(constpool, rm, VALUE_ATTRIBUTE, new MemberValue[]{smv});

      if (ctProducesAnnotation != null) {
        log.trace("Processing Produces annotation");
        Produces produces = (Produces) ctProducesAnnotation.getAnnotation();
        String[] mediaTypes = produces.value();
        List<MemberValue> memberValueList = new ArrayList<>();
        for (String mediaType : mediaTypes) {
          log.trace("Processing media type (produces) {}", mediaType);
          StringMemberValue s = new StringMemberValue(mediaType, constpool);
          memberValueList.add(s);
        }
        addAttributeToAnnotation(constpool, rm, "produces", memberValueList.toArray(new MemberValue[0]));
      }
      if (ctConsumesAnnotation != null) {
        log.trace("Processing consumes annotation");
        Consumes consumes = (Consumes) ctConsumesAnnotation.getAnnotation();
        String[] mediaTypes = consumes.value();
        List<MemberValue> memberValueList = new ArrayList<>();
        for (String mediaType : mediaTypes) {
          log.trace("Processing media type (consumes) {}", mediaType);
          StringMemberValue s = new StringMemberValue(mediaType, constpool);
          memberValueList.add(s);
        }
        addAttributeToAnnotation(constpool, rm, "consumes", memberValueList.toArray(new MemberValue[0]));
      }
      annotationsAttribute.addAnnotation(rm);
      classFilesToSave.add(ctClass);
      log.debug("classFile has {} attributes", classFile.getAttributes().size());
    }
  }

  private void addMethodAnnotationsFromJaxrsAnnotations(CtClass ctClass) {
    log.info("Processing method annotations {}", ctClass.getName());

    ConstPool constpool = ctClass.getClassFile().getConstPool();
    // method level annotations
    CtMethod[] methods = ctClass.getMethods();
    // for each method
    Arrays.stream(methods).forEach(m -> {
      // find annotations from the method or its super class method (which may contain the annotation we care about
      try {
        // process path annotation
        if (processJaxrsAnnotationOnMethod(m)) {
          // process method parameters
          processJaxrsMethodParameterAnnotations(m);
        }

      }
      catch (ClassNotFoundException | NotFoundException e) {
        log.error("Error finding annotation on method {} ", m.getName(), e);
      }
    });

  }

  private void processJaxrsMethodParameterAnnotations(CtMethod method) {

    ParameterAnnotationsAttribute attributeInfo = (ParameterAnnotationsAttribute)
        method.getMethodInfo().getAttribute(ParameterAnnotationsAttribute.visibleTag);

    if (attributeInfo == null) {
      return;
    }
    Annotation[][] parametersAnnotations = attributeInfo.getAnnotations();
    int index = 0;
    ConstPool constpool = method.getMethodInfo().getConstPool();
    log.trace("Method has {} parameters", parametersAnnotations.length, parametersAnnotations);
    for (Annotation[] parameterAnnotations : parametersAnnotations) {
      log.trace("Parameter {} has {} annotations", index, parameterAnnotations.length);
      // for each param - add all relevant annotations.
      List<Annotation> adaptedAnnotations = new ArrayList<>();

      for (Annotation parameterAnnotation : parameterAnnotations) {
        String parameterAnnotationTypeName = parameterAnnotation.getTypeName();
        if (METHOD_PARAMETER_ANNOTATION_LIST.contains(parameterAnnotationTypeName)) {
          log.trace("Parameter {} has {} annotation - adapting...", index, parameterAnnotationTypeName);
          // the springmvc ct annotation instance for this
          @SuppressWarnings("unchecked")
          Annotation adaptedParamAnnotation = getAnnotation(constpool, JAXRS_SPRINGMVC_ANNOTATION_MAP
              .get(parameterAnnotationTypeName));
          adaptedParamAnnotation.addMemberValue(VALUE_ATTRIBUTE, parameterAnnotation.getMemberValue(VALUE_ATTRIBUTE));
          adaptedAnnotations.add(adaptedParamAnnotation);
          Annotation[] newParameterAnnotations = addAnnotationToArray(parameterAnnotations, adaptedAnnotations);
          parametersAnnotations[index++] = newParameterAnnotations;
        }
      }
    }
    attributeInfo.setAnnotations(parametersAnnotations);
    classFilesToSave.add(method.getDeclaringClass());
  }

  private Annotation[] addAnnotationToArray(Annotation[] parameterAnnotations, List<Annotation> adaptedAnnotations) {
    Annotation newParamAnnotations[] = new Annotation[parameterAnnotations.length + adaptedAnnotations.size()];
    System.arraycopy(parameterAnnotations, 0, newParamAnnotations, 0, parameterAnnotations.length);
    final int[] idx = {0};
    adaptedAnnotations.forEach(a -> newParamAnnotations[parameterAnnotations.length + idx[0]++] = a);
    return newParamAnnotations;
  }

  private boolean processJaxrsAnnotationOnMethod(CtMethod method)
      throws NotFoundException, ClassNotFoundException {


    if (CtClassUtils.isInIncludedPackageOrSubpackage(packagesToInclude, method.getDeclaringClass().getPackageName())) {
      ConstPool constpool = method.getMethodInfo().getConstPool();
      Annotation requestMapping = null;

      CtAnnotation pathAnnotation = CtClassUtils.findAnnotationOnMethod(method, Path.class);
      CtAnnotation producesAnnotation = CtClassUtils.findAnnotationOnMethod(method, Produces.class);
      CtAnnotation consumesAnnotation = CtClassUtils.findAnnotationOnMethod(method, Consumes.class);

      if (pathAnnotation != null || producesAnnotation != null || consumesAnnotation != null) {
        requestMapping = getAnnotation(constpool, RequestMapping.class);
      }
      // set the value attribute of the RequestMapping
      if (requestMapping != null && pathAnnotation != null) {
        processJaxrsAnnotationsForArrayValues(constpool, requestMapping, VALUE_ATTRIBUTE,
                                              () -> new MemberValue[]
                                                  {
                                                      new StringMemberValue(
                                                          ((Path) pathAnnotation.getAnnotation()).value(),
                                                          constpool)
                                                  });
      }
      // set produces and consumes attribute of the RequestMapping
      if (requestMapping != null && producesAnnotation != null) {
        processJaxrsAnnotationsForArrayValues(constpool, requestMapping, "produces",
                                              () -> Arrays
                                                  .stream(((Produces) producesAnnotation.getAnnotation()).value())
                                                  .map(s -> new StringMemberValue(s, constpool))
                                                  .collect(toList()).toArray(new MemberValue[0]));

      }
      if (requestMapping != null && consumesAnnotation != null) {
        processJaxrsAnnotationsForArrayValues(constpool, requestMapping, "consumes",
                                              () -> Arrays
                                                  .stream(((Consumes) consumesAnnotation.getAnnotation()).value())
                                                  .map(s -> new StringMemberValue(s, constpool))
                                                  .collect(toList()).toArray(new MemberValue[0]));

      }
      // add HttpMethods to request mapping
      List<CtAnnotation> httpMethodAnnotations = new ArrayList<>();
      for (Class httpMethodClass : JAXRS_HTTP_METHOD_ANNOTATIONS) {
        CtAnnotation httpMethodAnnotation = CtClassUtils.findAnnotationOnMethod(method, httpMethodClass);
        if (httpMethodAnnotation != null && httpMethodAnnotation.getAnnotation() != null) {
          httpMethodAnnotations.add(httpMethodAnnotation);
        }
      }
      // no method specified - warn
      if (CollectionUtils.isEmpty(httpMethodAnnotations)) {
        log.warn("No http method annotation on method {}. Skipping...", method.getName());
        return false;
      }
      // Get the corresponding RequestMethod from the JAXRS method
      List<RequestMethod> httpRequestMethods = httpMethodAnnotations.stream()
                                                                    .map(h -> {
                                                                      String annotationClassName = h.getAnnotation()
                                                                                                    .annotationType()
                                                                                                    .getName();
                                                                      return valueOf(annotationClassName.substring(
                                                                          annotationClassName.lastIndexOf(".") + 1));
                                                                    })
                                                                    .collect(toList());
      if (requestMapping != null) {
        AnnotationsAttribute attr = getVisibleRuntimeAnnotationAttr(constpool, method.getMethodInfo());
        // process http method annotation
        // add http method annotations to requestMapping
        processJaxrsAnnotationsForArrayValues(constpool, requestMapping, "method",
                                              () -> {
                                                List<MemberValue> memberValueList = new ArrayList<>();
                                                for (RequestMethod requestMethod : httpRequestMethods) {
                                                  EnumMemberValue enumValue = new EnumMemberValue(constpool);
                                                  enumValue.setType(RequestMethod.class.getName());
                                                  enumValue.setValue(requestMethod.name());
                                                  memberValueList.add(enumValue);
                                                }
                                                return memberValueList.toArray(new MemberValue[0]);
                                              });
        attr.addAnnotation(requestMapping);
        classFilesToSave.add(method.getDeclaringClass());
        return true;
      }
      else {
        // request Mapping is null use the meta annotations.
        for (CtAnnotation methodAnnotation : httpMethodAnnotations) {

          AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
          java.lang.annotation.Annotation a = methodAnnotation.getAnnotation();
          Class springMvcAnnClass = JAXRS_SPRINGMVC_ANNOTATION_MAP.get(a.annotationType().getName());
          @SuppressWarnings("unchecked")
          Annotation springMvcMethodAnnotation = getAnnotation(constpool, springMvcAnnClass);
          // this goes on the method.
          attr.addAnnotation(springMvcMethodAnnotation);
          method.getMethodInfo().addAttribute(attr);
        }
        return true;
      }
    }
    return false;
  }

  private Annotation getAnnotation(ConstPool constpool, Class<? extends java.lang.annotation.Annotation> springMvcAnnClass) {
    return new Annotation(springMvcAnnClass.getName(), constpool);
  }

  private void processJaxrsAnnotationsForArrayValues(ConstPool constpool,
                                                     Annotation annotation,
                                                     String attrName,
                                                     Supplier<MemberValue[]> memberValueSupplier) {

    log.trace("Processing array valued annotation attribute {} for annotation {}", attrName, annotation.getTypeName());
    MemberValue[] memberValues = memberValueSupplier.get();
    addAttributeToAnnotation(constpool, annotation, attrName, memberValues);
  }

  private AnnotationsAttribute getVisibleRuntimeAnnotationAttr(ConstPool constpool, MethodInfo methodInfo) {
    AttributeInfo attributeInfo = getAttributeInfo(constpool, methodInfo.getAttribute(AnnotationsAttribute.visibleTag));
    methodInfo.addAttribute(attributeInfo);
    return (AnnotationsAttribute) attributeInfo;
  }

  private AnnotationsAttribute getVisibleRuntimeAnnotationAttr(ConstPool constpool, ClassFile classFile) {

    AttributeInfo attributeInfo = getAttributeInfo(constpool, classFile.getAttribute(AnnotationsAttribute.visibleTag));
    classFile.addAttribute(attributeInfo);
    return (AnnotationsAttribute) attributeInfo;
  }

  private AttributeInfo getAttributeInfo(ConstPool constpool, AttributeInfo attribute) {
    if (attribute == null) {
      attribute = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
    }
    return attribute;
  }

  private void addAttributeToAnnotation(ConstPool constpool, Annotation rm, String attrName, MemberValue[] smv) {
    ArrayMemberValue arrayMemberValue = new ArrayMemberValue(constpool);
    arrayMemberValue.setValue(smv);
    rm.addMemberValue(attrName, arrayMemberValue);
  }

  @Override
  public void configure(Properties properties) {
    if (properties == null) {
      return;
    }
    String propertiesToIncludeStr = properties.getProperty(PACKAGES_TO_INCLUDE_KEY);
    if (StringUtils.isNotEmpty(propertiesToIncludeStr)) {
      packagesToInclude = Arrays.asList(StringUtils.split(propertiesToIncludeStr, ","));
    }
  }

}
