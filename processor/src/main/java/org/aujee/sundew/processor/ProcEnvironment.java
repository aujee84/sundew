package shared.processor;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public enum ProcEnvironment {
    ON_INIT;

    private final Messager messager;
    private final Filer filer;
    private final Elements elementUtils;
    private final Types typeUtils;

    ProcEnvironment() {
        ProcessingEnvironment procEnvironment = StartUpProcessor.getEnvironment();
        messager = procEnvironment.getMessager();
        filer = procEnvironment.getFiler();
        elementUtils = procEnvironment.getElementUtils();
        typeUtils = procEnvironment.getTypeUtils();
    }

    public Filer filer() {
        return filer;
    }

    public Elements elementUtils() {
        return elementUtils;
    }

    public Types typeUtils() {
        return typeUtils;
    }

    public Messager messager() {
        return messager;
    }
}
