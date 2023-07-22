package org.aujee.sundew.processor.support.autoconfiggroup;

import org.aujee.sundew.api.annotations.AutoProperties;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

class PropertiesFileWriter implements CreateAble, ClassWriterExecutable {
    private static final Elements ELEMENT_UTILS = ProcEnvironment.ON_INIT.elementUtils();
    private static final Class<? extends Annotation> SUPPORTS = AutoProperties.class;
    private final Map<String, Map.Entry<Boolean, List<String[]>>> elementDataContainer;

    static PropertiesFileWriter getWriter(Set<? extends Element> properElements) {
        return new PropertiesFileWriter(properElements);
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
            String fileName = containerEntry.getKey() + ".properties";

            FileObject file = SourceBuilder
                    .create()
                    .getToWriteFileSource(StandardLocation.CLASS_OUTPUT, "resources", fileName);

            BufferedWriter bufferedWriter = new BufferedWriter(file.openWriter());

            Map.Entry<Boolean, List<String[]>> perFileData = containerEntry.getValue();
            List<String[]> elementData = perFileData.getValue();

            AtomicInteger startIndex = new AtomicInteger(0);
            int[] index = new int[elementData.size()];
            boolean defaultBranching = perFileData.getKey();

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

    private PropertiesFileWriter(Set<? extends Element> properElements) {
        Function<VariableElement, String> fileNameExtractor =
                element -> element.getAnnotation(AutoProperties.class).fileName();
        Function<VariableElement, String> customBranchExtractor =
                element -> element.getAnnotation(AutoProperties.class).customBranch();
        Predicate<VariableElement> branchNamingSplitter =
                element -> element.getAnnotation(AutoProperties.class).defaultBranching();
        elementDataContainer = new DataExtractor().getData(
                properElements,
                fileNameExtractor,
                branchNamingSplitter,
                customBranchExtractor,
                ELEMENT_UTILS);
    }
}
