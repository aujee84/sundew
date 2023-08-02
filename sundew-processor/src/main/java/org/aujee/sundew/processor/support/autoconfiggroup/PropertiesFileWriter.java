package org.aujee.sundew.processor.support.autoconfiggroup;

import org.aujee.sundew.api.annotations.AutoProperties;
import org.aujee.sundew.processor.ProcLogger;
import org.aujee.sundew.processor.support.CreateAble;
import org.aujee.sundew.processor.support.SourceBuilder;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

final class PropertiesFileWriter extends BaseFileWriter implements CreateAble{
    private static final Function<VariableElement, String> FILE_NAME_EXTRACTOR = element
            -> element.getAnnotation(AutoProperties.class).fileName();
    private static final Function<VariableElement, String> CUSTOM_BRANCH_EXTRACTOR = element
            -> element.getAnnotation(AutoProperties.class).customBranch();
    private static final Predicate<VariableElement> BRANCH_NAMING_SPLITTER = element
            -> element.getAnnotation(AutoProperties.class).defaultBranching();
    private static final Class<? extends Annotation> SUPPORTS = AutoProperties.class;
    private static final String FILE_SUFFIX = ".properties";


    PropertiesFileWriter(Set<? extends Element> properElements) {
        super(properElements, FILE_NAME_EXTRACTOR, BRANCH_NAMING_SPLITTER, CUSTOM_BRANCH_EXTRACTOR);
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
            String fileName = containerEntry.getKey() + FILE_SUFFIX;

            FileObject file = SourceBuilder
                    .create()
                    .getToWriteFileSource(StandardLocation.CLASS_OUTPUT, "resources", fileName);

            BufferedWriter bufferedWriter = new BufferedWriter(file.openWriter());

            Map.Entry<Boolean, List<String[]>> perFileData = containerEntry.getValue();
            List<String[]> elementData = perFileData.getValue();
            int[] index = new int[elementData.size()];

            boolean defaultBranching = perFileData.getKey();

            AtomicInteger startIndex = new AtomicInteger(0);

            if (defaultBranching) {
                elementData.forEach(element -> {
                    String qualifiedName = element[0];
                    String fieldName = element[1];
                    String propertyKey = qualifiedName + "." + fieldName;
                    String fullLine = propertyKey + "=${" + propertyKey + "}";
                    try {
                        bufferedWriter.write(fullLine);
                        bufferedWriter.newLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    index[startIndex.get()] = startIndex.get();
                    startIndex.getAndIncrement();
                });
            } else {
                elementData.forEach(element -> {
                    String fullLine = element[3] + "=${" + element[3] + "}";
                    try {
                        bufferedWriter.write(fullLine);
                        bufferedWriter.newLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    index[startIndex.get()] = startIndex.get();
                    startIndex.getAndIncrement();
                });
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

    @Override
    BranchWritingProcedure<List<String[]>> getProcedure(final BufferedWriter bufferedWriter,
                                                        final List<String[]> elementData,
                                                        final int[] index) {
        return null;
    }
}
