package org.aujee.sundew.processor.support.autoconfiggroup;

import org.aujee.sundew.processor.ProcEnvironment;
import org.aujee.sundew.processor.ProcLogger;
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
import java.util.function.Function;
import java.util.function.Predicate;

abstract class BaseFileWriter {
    private static final Elements ELEMENT_UTILS = ProcEnvironment.ON_INIT.elementUtils();
    protected Map<String, Map.Entry<Boolean, List<String[]>>> elementDataContainer;

    BaseFileWriter(Set<? extends Element> properElements,
                   Function<VariableElement, String> FILE_NAME_EXTRACTOR,
                   Predicate<VariableElement> BRANCH_NAMING_SPLITTER,
                   Function<VariableElement, String> CUSTOM_BRANCH_EXTRACTOR) {

        elementDataContainer = new DataExtractor().getData(
                properElements,
                FILE_NAME_EXTRACTOR,
                BRANCH_NAMING_SPLITTER,
                CUSTOM_BRANCH_EXTRACTOR,
                ELEMENT_UTILS);

        DataClassWriterExecutor.prepare();
        DataClassWriterExecutor.incrementExecuteCalls();
    }

    boolean createWithBranchingProcedure(Class<? extends Annotation> SUPPORTS, String fileSuffix) throws IOException {
        if (elementDataContainer == null) {
            ProcLogger.notFoundAnyMes(SUPPORTS.getSimpleName());
            return false;
        }
        boolean created = false;

        for (Map.Entry<String, Map.Entry<Boolean, List<String[]>>> containerEntry : elementDataContainer.entrySet()) {
            String fileName = containerEntry.getKey() + fileSuffix;

            FileObject file = SourceBuilder
                    .create()
                    .getToWriteFileSource(StandardLocation.CLASS_OUTPUT, "resources", fileName);

            BufferedWriter bufferedWriter = new BufferedWriter(file.openWriter());

            Map.Entry<Boolean, List<String[]>> perFileData = containerEntry.getValue();
            List<String[]> elementData = perFileData.getValue();
            int[] index = new int[elementData.size()];

            boolean defaultBranching = perFileData.getKey();

            BranchWritingProcedure<List<String[]>> procedure = getProcedure(bufferedWriter, elementData, index);

            if (defaultBranching) {
                procedure.proceedWithDefaultBranching(elementData);
            } else {
                procedure.proceedWithCustomBranching(elementData);
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

    abstract BranchWritingProcedure<List<String[]>> getProcedure (BufferedWriter bufferedWriter,
                                                                  List<String[]> elementData,
                                                                  int[] index);
}
