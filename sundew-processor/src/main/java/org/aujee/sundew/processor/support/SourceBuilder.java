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
 * Thread safe policy assumes that SourceBuilder object will be used only by one thread.
 * - JavaFile (thread confined) won't be a shared resource. Only filler is a shared resource.
 */
public final class SourceBuilder {
    private static final Lock FILLER_LOCK = new ReentrantLock();
    private static final Filer FILER = ProcEnvironment.ON_INIT.filer();
    private JavaFile javaFile;

    private SourceBuilder() {}

    public static SourceBuilder create() {
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
            return FILER.createResource(location, moduleAndPkg, relativeName);
        } finally {
            FILLER_LOCK.unlock();
        }
    }

    public void writeToFiler() throws IOException {
        FILLER_LOCK.lock();
        try {
            javaFile.writeTo(FILER);
        } finally {
            FILLER_LOCK.unlock();
        }
    }
}
