package shared.processor.support;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.aujee.com.shared.api.annotations.Configurable;
import org.aujee.com.shared.processor.ProcEnvironment;
import org.aujee.com.shared.processor.ProcLogger;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.Collectors;

/**
 * This class was written only for fun and mainly to familiarize with javax annotation processor.
 * It won't be further refactored.
 * With @Configurable on static fields in a class type it will generate a class with name
 * this + "Config" suffix that holds the same fields.
 */
final class ConfigurableSupport extends SupportDispatcher {
    private static final Class<? extends Annotation> SUPPORTS = Configurable.class;
    private static final SourceDestiny DESTINY = SourceDestiny.INTERNAL;
    private static final Set<ElementKind> APPLICABLE_FOR_THIS = Set.of(ElementKind.CLASS);
    private static Elements elementUtils;
    private static int processAbleElements;
    private static Map<PackageElement, Map<TypeElement, Set<VariableElement>>> elementsTree;
    private static Queue<Map.Entry<String, TypeSpec>> createAbleSourceSchemas;

    static ConfigurableSupport provideSupport(Set<? extends Element> elements) {
        processAbleElements = elements.size();
        elementUtils = ProcEnvironment.ON_INIT.elementUtils();
        return new ConfigurableSupport(elements);
    }

    @Override
    public boolean generate() throws IOException {
        if (elementsTree.isEmpty()) {
            ProcLogger.notFoundAnyMes(SUPPORTS.getSimpleName());
            return false;
        }

        ConfigurableSchemasBuilder schemasBuilder = new ConfigurableSchemasBuilder();
        schemasBuilder.createSchemas();
        Map.Entry<String, TypeSpec> schema = schemasBuilder.popSchema();

        while (schema != null) {
            SourceBuilder.create()
                    .buildJavaSource(schema.getKey(), schema.getValue())
                    .writeToFiler();

            schema = schemasBuilder.popSchema();
        }

        return false;
    }

    @Override
    public Class<? extends Annotation> getAnnotation() {
        return SUPPORTS;
    }

    @Override
    public SourceDestiny getDestiny() {
        return DESTINY;
    }


    private ConfigurableSupport(Set<? extends Element> elementsSet)  {
        final String annotation = SUPPORTS.getSimpleName();
        final AtomicInteger discoveryCounter = new AtomicInteger(0);
        final AtomicInteger mistakeCounter = new AtomicInteger(0);
        final AtomicReferenceArray<Element> onNotApplicable = new AtomicReferenceArray<>(processAbleElements);

        elementsTree = elementsSet.stream()
                .peek(element -> {
                    if (!isApplicable(element)) {
                        catchUserMistake(element, onNotApplicable, mistakeCounter);
                    }})
                .filter(this::isApplicable)
                .map(element -> (VariableElement) element)
                .peek(variableElement -> {
                    ProcLogger.elementDiscoveryMes(annotation, variableElement);
                    discoveryCounter.getAndIncrement();
                })
                .collect(Collectors.groupingBy(
                        element -> elementUtils.getPackageOf(element.getEnclosingElement()),
                        () -> HashMap.newHashMap(processAbleElements),
                        Collectors.groupingBy(
                                element -> (TypeElement) element.getEnclosingElement(),
                                () -> HashMap.newHashMap(processAbleElements),
                                Collectors.toCollection(
                                        () -> HashSet.newHashSet(processAbleElements))
                        )));

        int properElements = discoveryCounter.get();
        if (processAbleElements != properElements) {
            int userMistakes = mistakeCounter.get();
            if (userMistakes != 0) {
                ProcLogger.userMistakeMes(annotation, userMistakes, onNotApplicable);
            }
            //Bellow probably should not ever happen.
            int failures = processAbleElements - userMistakes - discoveryCounter.get();
            if (failures != 0) {
                ProcLogger.unexpectedFailureMes(failures, annotation);
            }
        } else {
            ProcLogger.collectingSuccessMes(annotation);
        }

        createAbleSourceSchemas = new ArrayBlockingQueue<>(processAbleElements);
    }

    private <T extends Element> boolean isApplicable(T element) {
        return APPLICABLE_FOR_THIS.contains(element.getEnclosingElement().getKind());
    }

    private <T extends Element> void catchUserMistake(
            T element,
            final AtomicReferenceArray<T> onNotApplicable,
            final AtomicInteger mistakeCounter) {

        onNotApplicable.set(mistakeCounter.get(), element);
        mistakeCounter.getAndIncrement();
    }

    private static class ConfigurableSchemasBuilder {
        String packageName = null;
        String className = null;
        String fieldName = null;
        TypeName typeName = null;
        TypeSpec typeSpec = null;
        int CompleteSchemaFields = 0;
        Set<FieldSpec> fieldSpecs = HashSet.newHashSet(processAbleElements);

        Map.Entry<String, TypeSpec> popSchema() {
            return createAbleSourceSchemas.poll();
        }

        void createSchemas() {
            for (Map.Entry<PackageElement, Map<TypeElement, Set<VariableElement>>> asPackageEntry : elementsTree.entrySet()) {
                packageName = asPackageEntry.getKey().getQualifiedName().toString();
                for (Map.Entry<TypeElement, Set<VariableElement>> asTypeEntry : asPackageEntry.getValue().entrySet()) {
                    className = ClassName.get(asTypeEntry.getKey()).simpleName() + "Config";
                    asTypeEntry.getValue().forEach(
                            variableElement -> {
                                typeName = TypeName.get(variableElement.asType());
                                fieldName = variableElement.getSimpleName().toString();
                                createFieldSpec();
                                ProcLogger.fieldCreationMes(className, typeName ,fieldName);
                                CompleteSchemaFields++;
                            }
                    );
                    createTypeSpec();
                    pushCompleteSchema();
                    clear();
                }
            }
        }

        void pushCompleteSchema() {
            createAbleSourceSchemas.add(Map.entry(packageName, typeSpec));
        }

        void createFieldSpec() {
            FieldSpec fieldSpec = FieldSpec.builder(typeName, fieldName)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .build();
            fieldSpecs.add(fieldSpec);
        }

        void createTypeSpec() {
            typeSpec = TypeSpec.classBuilder(className)
                    .addModifiers(Modifier.PUBLIC)
                    .addFields(fieldSpecs)
                    .build();
        }

        void clear() {
            fieldSpecs = HashSet.newHashSet(processAbleElements - CompleteSchemaFields);
        }
    }
}
