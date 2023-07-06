package shared.processor.support.autoconfigurablegroup;

import org.aujee.com.shared.processor.ProcLogger;
import org.aujee.com.shared.processor.support.CreateAble;
import org.aujee.com.shared.processor.support.ElementsProvider;
import org.aujee.com.shared.processor.support.SourceBuilder;

import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;
import java.util.function.Function;

class ConfigurablePropertyFileWriter implements CreateAble {
    private static final String HANDLES = "AutoConfigure";
    private static final String FILE_NAME = "application.properties";
    private static Set<? extends Element> properElements;

    static ConfigurablePropertyFileWriter getWriter(
            ElementsProvider<Set<? extends Element>, Function<Element, Boolean>, Set<? extends Element>> provider) {
        properElements = provider.getSelected();
        return new ConfigurablePropertyFileWriter();
    }

    @Override
    public boolean create() throws IOException {
        if (properElements == null) {
            ProcLogger.notFoundAnyMes(HANDLES);
            return false;
        }

        FileObject file = SourceBuilder
                .create()
                .getToWriteFileSource(StandardLocation.SOURCE_OUTPUT, "resources", FILE_NAME);

        BufferedWriter bufferedWriter = new BufferedWriter(file.openWriter());

        properElements.forEach(element -> {
            StringBuilder builder = new StringBuilder();
            String qualifiedName = element.getEnclosingElement().asType().toString();
            String fieldName = element.getSimpleName().toString();
            String propertyKey = qualifiedName + "." + fieldName;
            String fullLine = builder
                    .append(propertyKey)
                    .append("=${")
                    .append(propertyKey)
                    .append("}")
                    .toString();
            try {
                bufferedWriter.write(fullLine);
                bufferedWriter.newLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        bufferedWriter.flush();
        bufferedWriter.close();

        ProcLogger.propertiesCreationSuccessMes();
        return true;
    }

    private ConfigurablePropertyFileWriter() {}
}
