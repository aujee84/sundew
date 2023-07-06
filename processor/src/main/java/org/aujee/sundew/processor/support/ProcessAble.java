package shared.processor.support;

import java.io.IOException;
import java.lang.annotation.Annotation;

interface ProcessAble {

    boolean generate() throws IOException;

    Class<? extends Annotation> getAnnotation();

    SourceDestiny getDestiny();
}
