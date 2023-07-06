package shared.processor.support;

import org.aujee.com.shared.processor.support.autoconfigurablegroup.ConfigurablePropertyBase;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Set;

public abstract class SupportDispatcher implements ProcessAble {

    public static SupportDispatcher getSupport(Class<? extends Annotation> clazz,
                                               Set<? extends Element> elements){

        String annotationName = clazz.getSimpleName();

        return switch (annotationName) {
            case "Configurable" -> ConfigurableSupport.provideSupport(elements);
            case "AutoConfigure" -> ConfigurablePropertyBase.provideSupport(elements);
            default -> throw new IllegalStateException(String.format(
                    "Unsupported annotation type: @%s.", annotationName));
        };
    }
}

