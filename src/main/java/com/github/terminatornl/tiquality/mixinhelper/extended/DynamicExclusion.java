package com.github.terminatornl.tiquality.mixinhelper.extended;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Allows to mark custom methods for exclusion
 * See: DynamicMethodRedirector
 */
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.METHOD)
public @interface DynamicExclusion {
}
