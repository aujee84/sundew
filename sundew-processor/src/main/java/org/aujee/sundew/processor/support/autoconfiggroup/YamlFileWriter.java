package org.aujee.sundew.processor.support.autoconfiggroup;

import org.aujee.sundew.api.annotations.AutoYaml;
import org.aujee.sundew.processor.ProcEnvironment;
import org.aujee.sundew.processor.ProcLogger;
import org.aujee.sundew.processor.support.CreateAble;
import org.aujee.sundew.processor.support.SourceBuilder;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

class YamlFileWriter implements CreateAble, ClassWriterExecutable{
    private static final Elements ELEMENT_UTILS = ProcEnvironment.ON_INIT.elementUtils();
    private static final Class<? extends Annotation> SUPPORTS = AutoYaml.class;
    private final Map<String, Map.Entry<Boolean, List<String[]>>> elementDataContainer;

    static YamlFileWriter getWriter(final Set<? extends Element> properElements) {
        return new YamlFileWriter(properElements);
    }

    @Override
    public boolean create() throws IOException {
        //when all annotation was misused because of mixing branch names in one file
        if (elementDataContainer == null) {
            ProcLogger.notFoundAnyMes(SUPPORTS.getSimpleName());
            return false;
        }
        boolean created = false;

        for (Map.Entry<String, Map.Entry<Boolean, List<String[]>>> containerEntry : elementDataContainer.entrySet()) {
            String fileName = containerEntry.getKey() + ".yaml";

            FileObject file = SourceBuilder
                    .create()
                    .getToWriteFileSource(StandardLocation.CLASS_OUTPUT, "resources", fileName);

            BufferedWriter bufferedWriter = new BufferedWriter(file.openWriter());

            Map.Entry<Boolean, List<String[]>> perFileData = containerEntry.getValue();
            List<String[]> elementData = perFileData.getValue();
            int[] index = new int[elementData.size()];

            boolean defaultBranching = perFileData.getKey();

            BranchWriter branchWriter = new BranchWriter(bufferedWriter, elementData, index);

            if (defaultBranching) {
                branchWriter.writeDefaultBranching();
                ProcEnvironment.ON_INIT.messager().printNote(Arrays.toString(index));
            } else {

            }
            bufferedWriter.flush();
            bufferedWriter.close();
            DataClassWriterExecutor.addData(Map.entry(fileName, elementData));
            DataClassWriterExecutor.addIndex(Map.entry(fileName, index));
            DataClassWriterExecutor.incrementFileCount();
            ProcLogger.fileCreationSuccessMes(fileName);
            created = true;
        }

        return created;
    }

    public YamlFileWriter(final Set<? extends Element> properElements) {
        Function<VariableElement, String> fileNameExtractor =
                element -> element.getAnnotation(AutoYaml.class).fileName();
        Function<VariableElement, String> customBranchExtractor =
                element -> element.getAnnotation(AutoYaml.class).customBranch();
        Predicate<VariableElement> branchNamingSplitter =
                element -> element.getAnnotation(AutoYaml.class).defaultBranching();
        elementDataContainer = new DataExtractor().getData(
                properElements,
                fileNameExtractor,
                branchNamingSplitter,
                customBranchExtractor,
                ELEMENT_UTILS);
    }

    private class BranchWriter {
        final BufferedWriter bufferedWriter;
        final List<String[]> elementData;
        final int[] index;
        final AtomicInteger onFieldNameWriteIndex;
        final AtomicInteger elementDataIndex;

        AtomicInteger currentIndent;
        int baseIndent;
        String startingPackageName;

        volatile AtomicReference<String> previousQualifiedName;

        BranchWriter(final BufferedWriter bufferedWriter, final List<String[]> elementData, final int[] index) {
            this.bufferedWriter = bufferedWriter;
            this.elementData = elementData;
            this.index = index;
            this.onFieldNameWriteIndex = new AtomicInteger(0);
            this.elementDataIndex = new AtomicInteger(0);
        }

        void writeDefaultBranching() {
            elementData.forEach(element -> {
                String qualifiedName = element[0];
                String fieldName = element[1];
                int packageClassDotIndex = qualifiedName.lastIndexOf(".");
                String packageName = qualifiedName.substring(0, packageClassDotIndex);
                String className = qualifiedName.substring(packageClassDotIndex + 1);
                String fieldIdentifier ="${" + qualifiedName + "." + fieldName + "}";

                if (elementDataIndex.get() == 0) {
                    prepareDefaultReferences(qualifiedName, packageName);
                    writeLine(packageName + ":\n");
                    writeLine((className + ": ").indent(baseIndent));
                    currentIndent = new AtomicInteger(baseIndent + 7);
                }
                if (qualifiedName.equals(previousQualifiedName.get())) {
                    index[elementDataIndex.get()] = onFieldNameWriteIndex.get();
                    writeLine((fieldName + ": " + fieldIdentifier).indent(currentIndent.get()));
                } else {
                    String remainingPackagePart = packageName.replace(startingPackageName, "");
                    int indent = writeNextPackagePart(remainingPackagePart, baseIndent);
                    previousQualifiedName.set(qualifiedName);
                    indent += 3;
                    writeLine((className + ": ").indent(indent));
                    index[elementDataIndex.get()] = onFieldNameWriteIndex.get();
                    indent += 7;
                    currentIndent.set(indent);
                    writeLine((fieldName + ": " + fieldIdentifier).indent(currentIndent.get()));
                }
                elementDataIndex.getAndIncrement();
            });
        }

        private void prepareDefaultReferences(String qualifiedName, String packageName ) {
            previousQualifiedName = new AtomicReference<>(qualifiedName);
            startingPackageName = packageName;
            baseIndent = packageName.indexOf(".");
        }

        private void writeLine(final String inLine) {
            try {
                bufferedWriter.write(inLine);
                onFieldNameWriteIndex.getAndIncrement();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private int writeNextPackagePart(String innerPackagePart, int baseIndent) {
            int indentIncremented = baseIndent;
            if (innerPackagePart.startsWith(".")) {
                int optionalNextDotIndex = innerPackagePart.indexOf(".", 1);
                String firstPackagePart = optionalNextDotIndex == -1 ?
                        (innerPackagePart.substring(1) + ":") :
                        (innerPackagePart.substring(1, optionalNextDotIndex) + ":");
                String secondPackagePart = optionalNextDotIndex == -1 ?
                        "" :
                        innerPackagePart.substring(optionalNextDotIndex);

                writeLine(firstPackagePart.indent(indentIncremented));
                indentIncremented +=  3;

                writeNextPackagePart(secondPackagePart, indentIncremented);
            }
            return indentIncremented;
        }
    }
}
