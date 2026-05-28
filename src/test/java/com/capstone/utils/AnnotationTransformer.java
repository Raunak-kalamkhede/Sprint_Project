package com.capstone.utils;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Automatically binds the RetryAnalyzer to all incoming TestNG execution contexts.
 */
public class AnnotationTransformer implements IAnnotationTransformer {
    
    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        // Tie the analyzer directly to your test suites globally
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}