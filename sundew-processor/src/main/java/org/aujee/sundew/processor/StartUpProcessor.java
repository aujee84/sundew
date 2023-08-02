package org.aujee.sundew.processor;

import com.google.auto.service.AutoService;
import org.aujee.sundew.api.annotations.AutoConf;
import org.aujee.sundew.api.annotations.AutoJson;
import org.aujee.sundew.api.annotations.AutoProperties;
import org.aujee.sundew.api.annotations.AutoToml;
import org.aujee.sundew.api.annotations.AutoYaml;
import org.aujee.sundew.processor.support.SourceDestiny;
import org.aujee.sundew.processor.support.SupportDispatcher;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedSourceVersion(SourceVersion.RELEASE_20)
@AutoService(Processor.class)
public class StartUpProcessor extends AbstractProcessor {

    private static final Set<Class<? extends Annotation>> SUPPORTED_ANNOTATIONS = Set.of(
            AutoProperties.class,
            AutoYaml.class,
            AutoToml.class,
            AutoConf.class,
            AutoJson.class);

    private static ProcessingEnvironment processingEnvironment;
    private static Messager messager;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        processingEnvironment = processingEnv;
        messager = ProcEnvironment.ON_INIT.messager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return SUPPORTED_ANNOTATIONS.stream()
                .map(Class::getCanonicalName)
                .collect(Collectors.toSet());
    }

    private Set<Map.Entry<Enum<SourceDestiny>, SupportDispatcher>> rolledErrorSet =
            HashSet.newHashSet(0);

    @Override
    public synchronized boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

        try {
            if (roundEnv.errorRaised()) {
                for (final Map.Entry<Enum<SourceDestiny>, SupportDispatcher> next : rolledErrorSet) {
                    Enum<SourceDestiny> destiny = next.getKey();

                    if (SourceDestiny.PUBLIC_API.equals(destiny)) {
                        messager.printWarning(String.format(
                                "Some errors raised processing @%s. Processing will continue.",
                                next.getValue().getAnnotation().getSimpleName()));

                    } else if (SourceDestiny.INTERNAL.equals(destiny)) {
                        messager.printError(String.format(
                                "Error raised processing internal sources with @%s.",
                                next.getValue().getAnnotation().getSimpleName()
                        ));
                        throw new RuntimeException();
                    }
                }
            }

            rolledErrorSet = HashSet.newHashSet(toProcessSet.size());

            if (!roundEnv.processingOver()) {
                Set<SupportDispatcher> supports = getSupport(roundEnv);
    
                supports.forEach(support -> {
                    boolean processed = false;
                    try {
                        rolledErrorSet.add(Map.entry(support.getDestiny(), support));
                        processed = support.generate();
                    } catch (IOException e) {
                        messager.printError(String.format(
                                "Error processing @%s. Root cause: %s",
                                support.getAnnotation().getSimpleName(), getRootCause(e).getMessage()));
                    }
                    if (processed) {
                        Class<? extends Annotation> annotation = support.getAnnotation();
                        processedSet.add(annotation);
                        toProcessSet.remove(annotation);
                    }
                });
            } else {
                processedSet.forEach(annotation ->
                        messager.printNote(String.format(
                                "@%s annotations were processed.",
                                annotation.getSimpleName())));

                toProcessSet.forEach(annotation ->
                        messager.printNote(String.format(
                                "Non @%s annotations were processed.",
                                annotation.getSimpleName())));
            }
        } catch (RuntimeException e) {
            messager.printError(getRootCause(e).getMessage());
            throw new RuntimeException("Terminating...", e);
        }

        return true;
    }


    private final Set<Class<? extends Annotation>> toProcessSet =
            new HashSet<>(SUPPORTED_ANNOTATIONS);
    private final Set<Class<? extends Annotation>> processedSet =
            HashSet.newHashSet(SUPPORTED_ANNOTATIONS.size());

    private Set<SupportDispatcher> getSupport(RoundEnvironment roundEnvironment) {
        Set<SupportDispatcher> supports = HashSet.newHashSet(SUPPORTED_ANNOTATIONS.size());

        toProcessSet.forEach(anno -> {
            Set<? extends Element> elementsSet = roundEnvironment.getElementsAnnotatedWith(anno);

            if (!elementsSet.isEmpty()) {
                int size = elementsSet.size();
                String suffix = size == 1 ? "" : "s";
                messager.printNote(String.format(
                    "Found %d element%s annotated with @%s.", size, suffix, anno.getSimpleName()));

                SupportDispatcher support = SupportDispatcher.getSupport(anno, elementsSet);

                supports.add(support);
            }
        });
        
        return supports;
    }

    static ProcessingEnvironment getEnvironment() {
        return processingEnvironment;
    }

    private static Throwable getRootCause(Throwable throwable) {
        Objects.requireNonNull(throwable);
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }
}

