package com.ntnx.springmvc.transformer;

import com.ntnx.springmvc.javassist.beans.CtAnnotation;
import com.ntnx.springmvc.javassist.utils.CtClassUtils;
import de.icongmbh.oss.maven.plugin.javassist.ClassTransformer;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@SuppressWarnings("WeakerAccess")
@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class JaxrsToSpringMvcTransformer extends ClassTransformer {

  private List<String> packagesToInclude;

  private static final List<Class<?>> METHOD_ANNOTATION_LIST = Arrays.asList(Path.class, GET.class, POST.class);

  private static final List<Class<?>> METHOD_PARAMETER_ANNOTATION_LIST = Arrays.asList(PathParam.class,
                                                                                       QueryParam.class);


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
  }

  private void addMethodAnnotationsFromJaxrsAnnotations(CtClass ctClass) {
    log.info("Processing method annotations {}", ctClass.getName());
    // method level annotations
    CtMethod [] methods = ctClass.getMethods();
    // for each method
    Arrays.stream(methods).forEach(m -> {
      // find annotations from the method or its super class method (which may contain the annotation we care about
      METHOD_ANNOTATION_LIST.forEach(ma -> {
        CtAnnotation annotation = CtClassUtils.findAnnotationOnMethodOrSuperclass(m, ma);
      });

    });
    // parameter annotations.
  }

  private void processClassLevelJaxrsAnnotations(CtClass ctClass) throws NotFoundException, ClassNotFoundException {

    log.info("Processing class level annotations {}", ctClass.getName());

    // Read class level annotations for JaxRs method
    CtAnnotation ctPathAnnotation = CtClassUtils.findAnnotation(ctClass, Path.class, packagesToInclude);
    CtAnnotation ctProducesAnnotation = CtClassUtils.findAnnotation(ctClass, Produces.class, packagesToInclude);
    CtAnnotation ctConsumesAnnotation = CtClassUtils.findAnnotation(ctClass, Consumes.class, packagesToInclude);

    if(ctPathAnnotation != null) {
      log.debug("Found JaxRs Path annotation on class - adding RequestMapping on implementation class");
      ClassFile ccFile = ctClass.getClassFile();
      ConstPool constpool = ccFile.getConstPool();

      // add RestController annotation
      Annotation rc = new Annotation(RestController.class.getName(), constpool);
      AnnotationsAttribute rcAttr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
      rcAttr.addAnnotation(rc);
      ccFile.addAttribute(rcAttr);

      // create the annotation
      Path path = (Path) ctPathAnnotation.getAnnotation();
      log.trace("Processing Path annotation");
      AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
      Annotation rm = new Annotation(RequestMapping.class.getName(), constpool);
      StringMemberValue smv = new StringMemberValue(path.value(), constpool);
      addAttributeToAnnotation(constpool, rm, "value", new MemberValue[]{smv});

      if(ctProducesAnnotation != null) {
        log.trace("Processing Produces annotation");
        Produces produces = (Produces) ctProducesAnnotation.getAnnotation();
        String [] mediaTypes = produces.value();
        List<MemberValue> memberValueList = new ArrayList<>();
        for(String mediaType : mediaTypes) {
          log.trace("Processing media type (produces) {}", mediaType);
          StringMemberValue s = new StringMemberValue(mediaType, constpool);
          memberValueList.add(s);
        }
        addAttributeToAnnotation(constpool, rm, "produces", memberValueList.toArray(new MemberValue[0]));
      }
      if(ctConsumesAnnotation != null) {
        log.trace("Processing consumes annotation");
        Consumes consumes = (Consumes) ctConsumesAnnotation.getAnnotation();
        String [] mediaTypes = consumes.value();
        List<MemberValue> memberValueList = new ArrayList<>();
        for(String mediaType : mediaTypes) {
          log.trace("Processing media type (consumes) {}", mediaType);
          StringMemberValue s = new StringMemberValue(mediaType, constpool);
          memberValueList.add(s);
        }
        addAttributeToAnnotation(constpool, rm, "consumes", memberValueList.toArray(new MemberValue[0]));
      }
      attr.addAnnotation(rm);
      ccFile.addAttribute(attr);
    }
  }

  private void addAttributeToAnnotation(ConstPool constpool, Annotation rm, String attrName, MemberValue [] smv) {
    ArrayMemberValue arrayMemberValue = new ArrayMemberValue(constpool);
    arrayMemberValue.setValue(smv);
    rm.addMemberValue(attrName, arrayMemberValue);
  }

  @Override
  public void configure(Properties properties) throws Exception {
    super.configure(properties);
  }

  @Override
  protected boolean shouldTransform(CtClass candidateClass) throws Exception {
    return super.shouldTransform(candidateClass);
  }
}
