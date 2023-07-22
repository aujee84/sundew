package org.aujee.sundew.processor.support;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.aujee.sundew.processor.ProcEnvironment;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread safe. Only filler is a shared resource.
 */
public final class SourceBuilder {
    private static final Lock FILLER_LOCK = new ReentrantLock();
    private static Filer filer = null;
    private JavaFile javaFile;

    private SourceBuilder() {}

    public static SourceBuilder create() {
        filer = ProcEnvironment.ON_INIT.filer();
        return new SourceBuilder();
    }

    public SourceBuilder buildJavaSource(String packageName, TypeSpec typeSpec) {
        javaFile = JavaFile.builder(packageName, typeSpec).build();
        return this;
    }

    public FileObject getToWriteFileSource (JavaFileManager.Location location,
                                  CharSequence moduleAndPkg,
                                  CharSequence relativeName) throws IOException {
        FILLER_LOCK.lock();
        try {
            return filer.createResource(location, moduleAndPkg, relativeName);
        } finally {
            FILLER_LOCK.unlock();
        }
    }

    public void writeToFiler() throws IOException {
        FILLER_LOCK.lock();
        try {
            javaFile.writeTo(filer);
        } finally {
            FILLER_LOCK.unlock();
        }
    }
}
