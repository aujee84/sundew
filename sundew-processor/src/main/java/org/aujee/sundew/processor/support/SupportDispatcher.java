package org.aujee.sundew.processor.support;

import org.aujee.sundew.processor.support.autoconfiggroup.FileWriterDispatcher;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Set;

public abstract class SupportDispatcher implements ProcessAble {

    public static SupportDispatcher getSupport(Class<? extends Annotation> clazz,
                                               Set<? extends Element> elements){

        String annotationName = clazz.getSimpleName();

        return switch (annotationName) {
            case "AutoConfigure", "AutoProperties", "AutoYaml" -> FileWriterDispatcher.provideSupport(elements, clazz);
            default -> throw new IllegalStateException(String.format(
                    "Unsupported annotation type: @%s.", annotationName));
        };
    }
}

