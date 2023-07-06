package shared.processor.support.autoconfigurablegroup;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.aujee.com.shared.processor.ProcLogger;
import org.aujee.com.shared.processor.support.CreateAble;
import org.aujee.com.shared.processor.support.ElementsProvider;
import org.aujee.com.shared.processor.support.SourceBuilder;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
//to work on - created file should have private initialize method
class ConfigurablePropertyClassWriter implements CreateAble {
    private static final String HANDLES = "AutoConfigure";
    private static final String CLASS_DESTINY = "org.aujee.com.shared.autogencore";
    private static final String CLASS_NAME = "AutoConfig";
    private static Set<? extends Element> properElements;

    static ConfigurablePropertyClassWriter getWriter(
            ElementsProvider<Set<? extends Element>, Function<Element, Boolean>, Set<? extends Element>> provider) {
        properElements = provider.getSelected();
        return new ConfigurablePropertyClassWriter();
    }

    @Override
    public boolean create() throws IOException {
        if (properElements == null) {
            ProcLogger.notFoundAnyMes(HANDLES);
            return false;
        }

        TypeSpec typeSpec = new AutoConfigSchemaBuilder().createSchema();

        SourceBuilder.create()
                .buildJavaSource(CLASS_DESTINY, typeSpec)
                .writeToFiler();

        return true;
    }

    private static class AutoConfigSchemaBuilder {
        static Set<String[]> createAbles = convertToCreateAbles();

        TypeSpec createSchema() {
            class StaticBlockCreator {
                static final String key = "key";
                static final String autoGenVal = "autoGenVal";

                static CodeBlock createStaticBlock() {
                    List<CodeBlock> codeBlocks = new LinkedList<>();

                    CodeBlock staticInitializerSchema = CodeBlock.builder()
                            .addStatement("$T $N", String.class, key)
                            .addStatement("$T $N", String[].class, autoGenVal)
                            .build();

                    codeBlocks.add(staticInitializerSchema);

                    List<CodeBlock> staticChangeables = createAbles.stream()
                            .map(arr ->
                                    CodeBlock.builder()
                                            .addStatement("$N = new String[3]", autoGenVal)
                                            .addStatement("$N[0] = $S", autoGenVal, arr[0])
                                            .addStatement("$N[1] = $S", autoGenVal, arr[1])
                                            .addStatement("$N[2] = $S", autoGenVal, arr[2])
                                            .addStatement("$N = $N[0] + $S + $N[1]", key, autoGenVal, ".", autoGenVal)
                                            .addStatement("$N.put($N, $N)", "autoGenMap", key, autoGenVal)
                                            .build())
                            .toList();

                    codeBlocks.addAll(staticChangeables);

                    return CodeBlock.join(codeBlocks, "\n");
                }
            }

            TypeName typeName = ParameterizedTypeName.get(Map.class, String.class, String[].class);

            FieldSpec autoGenMap = FieldSpec.builder(typeName, "autoGenMap")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T<>()", HashMap.class)
                    .build();

            MethodSpec constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .build();

            MethodSpec initialize = MethodSpec.methodBuilder("initialize")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addStatement("AutoConfBase.load(autoGenMap)")
                    .addStatement("AutoConfBase.initialize()")
                    .returns(void.class)
                    .build();

            return TypeSpec.classBuilder(CLASS_NAME)
                    .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                    .addField(autoGenMap)
                    .addStaticBlock(StaticBlockCreator.createStaticBlock())
                    .addMethod(constructor)
                    .addMethod(initialize)
                    .build();
        }
    }

    private static Set<String[]> convertToCreateAbles() {
        return properElements.stream()
                .map(element -> {
                    String[] array = new String[3];
                    array[0] = element.getEnclosingElement().asType().toString();
                    array[1] = element.getSimpleName().toString();
                    String type = element.asType().toString();
                    int optionalLastDot = type.lastIndexOf(".");
                    String simpleType = optionalLastDot == -1 ? type : type.substring(optionalLastDot + 1);
                    array[2] = simpleType;
                    return array;
                })
                .collect(Collectors.toSet());
    }

    private ConfigurablePropertyClassWriter() {}
}


