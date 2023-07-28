package org.aujee.sundew.processor.support.autoconfiggroup;

import org.aujee.sundew.processor.ProcLogger;
import org.aujee.sundew.processor.support.CreateAble;
import org.aujee.sundew.processor.support.ElementsSelector;
import org.aujee.sundew.processor.support.SourceDestiny;
import org.aujee.sundew.processor.support.SupportDispatcher;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class FileWriterDispatcher extends SupportDispatcher implements ClassWriterExecutable {
    private static final SourceDestiny DESTINY = SourceDestiny.INTERNAL;
    private final Class<? extends Annotation> annotationClazz;
    private final String annotationName;
    private final CreateAble toHandle;
    private volatile boolean generated;

    public static SupportDispatcher provideSupport(final Set<? extends Element> elements,
                                                   final Class<? extends Annotation> supportedAnnotation) {
        DataClassWriterExecutor.prepare();
        DataClassWriterExecutor.incrementExecuteCalls();
        return new FileWriterDispatcher(elements, supportedAnnotation);
    }

    @Override
    public boolean generate() throws RuntimeException {
        try {
            generated = toHandle.create();
            if (generated) {
                DataClassWriterExecutor.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return generated;
    }

    @Override
    public Class<? extends Annotation> getAnnotation() {
        return annotationClazz;
    }

    @Override
    public SourceDestiny getDestiny() {
        return DESTINY;
    }

    private FileWriterDispatcher(final Set<? extends Element> elements,
                                 final Class<? extends Annotation> supportedAnnotation) {
        annotationClazz = supportedAnnotation;
        annotationName = supportedAnnotation.getSimpleName();

        BiFunction<Set<? extends Element>, Function<Element, Boolean>, Set<? extends Element>> selector =
                this::extractProperElements;
        Function<Element, Boolean> rule = this::isApplicable;
        ElementsSelector<Set<? extends Element>, Function<Element, Boolean>, Set<? extends Element>> elementsSelector =
                new ElementsSelector<>(elements, selector, rule);

        toHandle = dispatch(elementsSelector.getSelected());
    }

    private CreateAble dispatch (Set<? extends Element> properElements) {
        return switch (annotationName) {
            case "AutoProperties" -> PropertiesFileWriter.getWriter(properElements);
            case "AutoYaml" -> YamlFileWriter.getWriter(properElements);
//            case "AutoToml" -> TomlFileWriter.getWriter(properElements);
//            case "AutoConf" -> ConfFileWriter.getWriter(properElements);
//            case "AutoJson" -> JsonFileWriter.getWriter(properElements);
            default -> throw new IllegalStateException("Unsupported annotation: @" + annotationName);
        };
    }

    private  <T extends Element> boolean isApplicable(T element) {
        boolean inClass = element.getEnclosingElement().getKind().isClass();
        boolean isStatic = element.getModifiers().contains(Modifier.STATIC);
        boolean isAllowedType = allowedTypes.contains(element.asType().toString());
        boolean isPrimitive = element.asType().getKind().isPrimitive();

        return inClass && isStatic && (isAllowedType || isPrimitive);
    }

    private final Set<String> allowedTypes = Set.of(
            "java.lang.String",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Boolean",
            "java.lang.Character",
            "java.lang.Character[]",
            "char[]"
    );

    /*
    Provides comprehensive processor runtime information for the user and collects proper elements.
     */
    private Set<? extends Element> extractProperElements(Set<? extends Element> elementsSet, Function<Element, Boolean> rule) {
        final int processAbleElements = elementsSet.size();
        final AtomicInteger discoveryCounter = new AtomicInteger(0);
        final AtomicInteger mistakeCounter = new AtomicInteger(0);
        final AtomicReferenceArray<Element> holdsNotApplicable = new AtomicReferenceArray<>(processAbleElements);

        Set<VariableElement> extractedElements = elementsSet.stream()
                .peek(element -> {
                    if (!rule.apply(element)) {
                        catchUserMistake(element, holdsNotApplicable, mistakeCounter);
                    }
                })
                .filter(rule::apply)
                .map(element -> (VariableElement) element)
                .peek(variableElement -> {
                    ProcLogger.elementDiscoveryMes(annotationName, variableElement);
                    discoveryCounter.getAndIncrement();
                })
                .collect(Collectors.toSet());

        int properElements = discoveryCounter.get();

        if (processAbleElements != properElements) {
            int userMistakes = mistakeCounter.get();
            if (userMistakes != 0) {
                ProcLogger.userMistakeMes(annotationName, userMistakes, holdsNotApplicable);
            }
            int failures = processAbleElements - userMistakes - discoveryCounter.get();
            if (failures != 0) {
                ProcLogger.unexpectedFailureMes(failures, annotationName);
            }
        } else {
            ProcLogger.collectingSuccessMes(annotationName);
        }
        return extractedElements;
    }

    private <T extends Element> void catchUserMistake(
            T element,
            final AtomicReferenceArray<T> onNotApplicable,
            final AtomicInteger mistakeCounter) {

        onNotApplicable.set(mistakeCounter.get(), element);
        mistakeCounter.getAndIncrement();
    }
}
