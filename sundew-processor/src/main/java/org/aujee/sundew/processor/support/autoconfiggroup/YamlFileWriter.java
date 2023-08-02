package org.aujee.sundew.processor.support.autoconfiggroup;

import org.aujee.sundew.api.annotations.AutoYaml;
import org.aujee.sundew.processor.support.CreateAble;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

final class YamlFileWriter extends BaseFileWriter implements CreateAble {
    private static final Function<VariableElement, String> FILE_NAME_EXTRACTOR = element
            -> element.getAnnotation(AutoYaml.class).fileName();
    private static final Function<VariableElement, String> CUSTOM_BRANCH_EXTRACTOR = element
            -> element.getAnnotation(AutoYaml.class).customBranch();
    private static final Predicate<VariableElement> BRANCH_NAMING_SPLITTER = element
            -> element.getAnnotation(AutoYaml.class).defaultBranching();
    private static final Class<? extends Annotation> SUPPORTS = AutoYaml.class;
    private static final String FILE_SUFFIX = ".yaml";


    YamlFileWriter(final Set<? extends Element> properElements) {
        super(properElements, FILE_NAME_EXTRACTOR, BRANCH_NAMING_SPLITTER, CUSTOM_BRANCH_EXTRACTOR);
    }

    @Override
    public boolean create() throws IOException {
        return createWithBranchingProcedure(SUPPORTS, FILE_SUFFIX);
    }

    @Override
    BranchWritingProcedure<List<String[]>> getProcedure (final BufferedWriter bufferedWriter,
                                                         final List<String[]> elementData,
                                                         final int[] index) {
        YamlWriter writer = new YamlWriter(bufferedWriter, elementData, index);
        return new BranchWritingProcedure<>(
                arrL -> writer.writeDefaultBranching(),
                arrL -> writer.writeCustomBranching());
    }

    private static class YamlWriter {
        final BufferedWriter bufferedWriter;
        final List<String[]> elementData;
        final int[] index;
        final AtomicInteger onFieldNameWriteIndex;
        final AtomicInteger elementDataIndex;

        AtomicInteger currentIndent;
        int baseIndent;
        String startingPackageName;

        volatile AtomicReference<String> previousQualifiedName;

        YamlWriter(final BufferedWriter bufferedWriter, final List<String[]> elementData, final int[] index) {
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

        void writeCustomBranching() {

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
