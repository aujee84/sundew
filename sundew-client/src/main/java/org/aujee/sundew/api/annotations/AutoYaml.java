package org.aujee.sundew.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Default branch(value for field mapping) is field name preceded by fully qualified class name.
 * Per file You can not mix default branch name with custom name.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface AutoYaml {
    String fileName() default "application";
    boolean defaultBranching() default true;
    String customBranch() default "";
}
