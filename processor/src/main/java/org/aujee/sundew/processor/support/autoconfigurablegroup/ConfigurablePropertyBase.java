package shared.processor.support.autoconfigurablegroup;

import org.aujee.com.shared.api.annotations.AutoConfigure;
import org.aujee.com.shared.processor.ProcLogger;
import org.aujee.com.shared.processor.support.CreateAble;
import org.aujee.com.shared.processor.support.ElementsProvider;
import org.aujee.com.shared.processor.support.SourceDestiny;
import org.aujee.com.shared.processor.support.SupportDispatcher;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConfigurablePropertyBase extends SupportDispatcher  {
    private static final Class<? extends Annotation> SUPPORTS = AutoConfigure.class;
    private static final SourceDestiny DESTINY = SourceDestiny.INTERNAL;
    private final Set<CreateAble> toHandleSet;

    public static SupportDispatcher provideSupport(final Set<? extends Element> elements) {
        return new ConfigurablePropertyBase(elements);
    }

    @Override
    public boolean generate() {

        return toHandleSet.stream().parallel()
                .map(createAble -> {
                    try {
                        return createAble.create();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(bool -> bool.equals(Boolean.FALSE))
                .findAny()
                .orElse(true);
    }

    @Override
    public Class<? extends Annotation> getAnnotation() {
        return SUPPORTS;
    }

    @Override
    public SourceDestiny getDestiny() {
        return DESTINY;
    }

    private ConfigurablePropertyBase(final Set<? extends Element> elements) {
        final BiFunction<Set<? extends Element>, Function<Element, Boolean>, Set<? extends Element>> selector =
                this::extractProperElements;
        final Function<Element, Boolean> rule = this::isApplicable;
        final ElementsProvider<Set<? extends Element>, Function<Element, Boolean>, Set<? extends Element>> elementsProvider =
                new ElementsProvider<>(elements, selector, rule);

        toHandleSet = new HashSet<>();
        toHandleSet.add(ConfigurablePropertyFileWriter.getWriter(elementsProvider));
        toHandleSet.add(ConfigurablePropertyClassWriter.getWriter(elementsProvider));
    }

    private  <T extends Element> boolean isApplicable(T element) {
        boolean inClass = element.getEnclosingElement().getKind().isClass();
        boolean asStatic = element.getModifiers().contains(Modifier.STATIC);
        boolean isAllowedType = allowedTypes.contains(element.asType().toString());
        boolean isPrimitive = element.asType().getKind().isPrimitive();

        return inClass && asStatic && (isAllowedType || isPrimitive);
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

    private Set<? extends Element> extractProperElements(Set<? extends Element> elementsSet, Function<Element, Boolean> rule) {
        final int processAbleElements = elementsSet.size();
        final String annotation = SUPPORTS.getSimpleName();
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
                    ProcLogger.elementDiscoveryMes(SUPPORTS.getSimpleName(), variableElement);
                    discoveryCounter.getAndIncrement();
                })
                .collect(Collectors.toSet());

        int properElements = discoveryCounter.get();

        if (processAbleElements != properElements) {
            int userMistakes = mistakeCounter.get();
            if (userMistakes != 0) {
                ProcLogger.userMistakeMes(annotation, userMistakes, holdsNotApplicable);
            }
            int failures = processAbleElements - userMistakes - discoveryCounter.get();
            if (failures != 0) {
                ProcLogger.unexpectedFailureMes(failures, annotation);
            }
        } else {
            ProcLogger.collectingSuccessMes(annotation);
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
