package com.github.krr.springmvc.javassist.utils;

import com.github.krr.springmvc.javassist.beans.CtAnnotation;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.util.List;

@SuppressWarnings("WeakerAccess")
@Slf4j
public class CtClassUtils {

  /**
   * Finds the ctAnnotation by traversing the specified class, its superclass and all implemented
   * interfaces to find a specific ctAnnotation.  Returns the first instance of the ctAnnotation
   * that is found.
   *
   * @param ctClass           - the ctClass on which to find the annotations
   * @param annotationClass   - the class of the ctAnnotation to find
   * @param packagesToInclude - The list of packages to check for matches.  All subpackages of these
   *                          packages will be searched.  If null or empty, all packages are searched.
   * @return - The ctAnnotation class if found or null if not found.
   * @throws ClassNotFoundException
   */
  public static <T extends Annotation> CtAnnotation findAnnotation(CtClass ctClass,
                                                                   Class<T> annotationClass,
                                                                   List<String> packagesToInclude)
      throws ClassNotFoundException, NotFoundException {
    log.trace("Searching for packages in {}", packagesToInclude);

    String className = ctClass.getName();
    log.debug("Finding ctAnnotation {} in class", annotationClass, className);

    // analyze this class only if it is in the white list.
    if (isInIncludedPackageOrSubpackage(packagesToInclude, ctClass.getPackageName())) {
      log.trace("Class {} is included in package - searching...", className);
      Object annotation = ctClass.getAnnotation(annotationClass);
      if (annotation == null) {
        log.debug("Annotation {} not found in class {}, searching in superclass", annotationClass, className);
        // find in superclass.
        CtClass superClass = ctClass.getSuperclass();
        if (superClass != null && isInIncludedPackageOrSubpackage(packagesToInclude, superClass.getPackageName())) {
          String superClassName = superClass.getName();
          log.trace("Searching for ctAnnotation {} in superclass {}", annotationClass, superClassName);

          // the super class is in a package we want to analyze - try to get it from there.
          CtAnnotation superAnnotation = findAnnotation(superClass, annotationClass, packagesToInclude);
          if (superAnnotation == null) {
            // none of the super classes have this ctAnnotation - check the interfaces
            log.trace("Annotation {} not found on class - searching interfaces of {}", annotationClass, superClassName);
            return findAnnotationInInterfaces(superClass, annotationClass, packagesToInclude);
          }
          return superAnnotation;
        }
        else {
          // superclass is not part of scanned packages so find in interfaces of this class.
          return findAnnotationInInterfaces(ctClass, annotationClass, packagesToInclude);
        }
      }
      return new CtAnnotation(ctClass, (Annotation) annotation);
    }
    return null;
  }

  /**
   * Overloaded method that finds annotations across all packages
   */
  public static <T extends Annotation> CtAnnotation findAnnotation(CtClass ctClass,
                                                                   Class<T> annotationClass)
      throws NotFoundException, ClassNotFoundException {

    log.warn("No search package specified - will search in all packages.  This could have a performance impact");
    return findAnnotation(ctClass, annotationClass, null);
  }

  /**
   * Searches for annotations in the interfaces of the CtClass
   *
   * @param ctClass           - the ctClass whose interfaces are searched
   * @param annotationClass   - the ctAnnotation to search for
   * @param packagesToInclude - a list of packages and subpackages to include in the search.  A null value implies all
   *                          packages are searched
   * @param <T>               the class of the ctAnnotation to search for
   * @return the Annotation wrapped in a {@link CtAnnotation} object
   * @throws NotFoundException      - if interfaces were not found.
   * @throws ClassNotFoundException - if the interface class was not found in the classpath.
   */
  public static <T extends Annotation> CtAnnotation findAnnotationInInterfaces(CtClass ctClass, Class<T> annotationClass,
                                                                               List<String> packagesToInclude)
      throws NotFoundException, ClassNotFoundException {

    CtClass[] interfaces = ctClass.getInterfaces();
    CtAnnotation intfAnnotation = null;
    for (CtClass intf : interfaces) {
      intfAnnotation = findAnnotation(intf, annotationClass, packagesToInclude);
      if (intfAnnotation != null) {
        break;
      }
    }
    return intfAnnotation;
  }

  /**
   * Overloaded method that searches all packages.
   */
  public static <T extends Annotation> CtAnnotation findAnnotationInInterfaces(CtClass ctClass,
                                                                               Class<T> annotationClass)
      throws NotFoundException, ClassNotFoundException {

    return findAnnotationInInterfaces(ctClass, annotationClass, null);
  }

  private static boolean isInIncludedPackageOrSubpackage(List<String> packagesToInclude, String packageName) {
    if (CollectionUtils.isEmpty(packagesToInclude)) {
      return true;
    }
    for (String packageNameToCheck : packagesToInclude) {
      if (packageName.startsWith(packageNameToCheck)) {
        return true;
      }
    }
    return false;
  }

  public static <T extends Annotation> CtAnnotation findAnnotationOnMethod(CtMethod m,
                                                                           Class<T> ma,
                                                                           List<String> packagesToInclude)
      throws ClassNotFoundException, NotFoundException {
    log.trace("Finding method annotations on method or its superclass");
    Object jaAnnotation = m.getAnnotation(ma);
    if (jaAnnotation == null) {
      // not found here - search in super class
      CtClass superClass = m.getDeclaringClass().getSuperclass();
      // we don't filter by included package here because we want to determine whether the superclass holds the
      // annotation or not.  Only the original class is expected to be in the search path.
      if (superClass != null) {
        log.trace("Annotation {} not found in method {}, searching in superclass method", m.getName(),
                  superClass.getName());
        // find in superclass.
        CtMethod superclassMethod = superClass.getMethod(m.getName(), m.getSignature());
        if (superclassMethod != null) {
          CtAnnotation superAnnotation = findAnnotationOnMethod(superclassMethod, ma, null);
          if (superAnnotation == null) {
            // none of the super classes have this ctAnnotation - check the interfaces
            log.trace("Annotation {} not found on class - searching interfaces of {}", ma, superClass.getName());
            return findAnnotationInInterfaces(superClass, m.getName(), m.getSignature(), ma, null);
          }
          return superAnnotation;
        }
        else {
          // method doesn't exist in the super class - check interfaces of super class
          return findAnnotationInInterfaces(superClass, m.getName(), m.getSignature(), ma, null);
        }
      }
    }
    else {
      // annotations not in this class - check interfaces of this class.
      return findAnnotationInInterfaces(m.getDeclaringClass(), m.getName(), m.getSignature(), ma, packagesToInclude);
    }
    return null;
  }

  @SuppressWarnings("SameParameterValue")
  private static <T extends Annotation> CtAnnotation findAnnotationInInterfaces(CtClass ctClass, String methodName,
                                                                                String methodDescriptor,
                                                                                Class<T> ma,
                                                                                List<String> packagesToInclude)
      throws NotFoundException, ClassNotFoundException {

    CtClass[] interfaces = ctClass.getInterfaces();

    CtAnnotation intfAnnotation = null;
    for (CtClass intf : interfaces) {
      CtMethod method = intf.getMethod(methodName, methodDescriptor);
      intfAnnotation = findAnnotationOnMethod(method, ma, packagesToInclude);
      if (intfAnnotation != null) {
        break;
      }
    }
    return intfAnnotation;
  }
}
