package org.aujee.sundew.processor;

import com.squareup.javapoet.TypeName;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class ProcLogger {
    private static final Messager messager = ProcEnvironment.ON_INIT.messager();

    private ProcLogger() {}

    public static void notFoundAnyMes(String annotation) {
        messager.printWarning(String.format(
                "There was no proper annotated elements with @%s.", annotation));
    }

    public static void unexpectedFailureMes(int count, String annotation) {
        messager.printError(String.format(
                "Unexpected failure for %d proper annotated with @%s elements", count, annotation ));
    }

    public static void collectingSuccessMes(String annotation) {
        messager.printNote(String.format(
                "Elements annotated with @%s collected without failure", annotation));
    }

    public static void userMistakeMes(String annotation, int userMistakes, AtomicReferenceArray<Element> onNotApplicable) {
        for (int i = 0; i < userMistakes; i++) {
            Element element = onNotApplicable.get(i);
            String onFieldType = element.getKind().toString();
            String onFieldName = element.getSimpleName().toString();
            String inType = element.getEnclosingElement().asType().toString();
            String msg = String.format("@%s is not allowed on %s %s in %s.", annotation, onFieldType, onFieldName, inType);
            messager.printWarning(msg);
        }
    }

    public static void elementDiscoveryMes(String annotation, Element variableElement) {
        String canonicalName = variableElement.getEnclosingElement().asType().toString();
        String elementName = variableElement.getSimpleName().toString();
        String msg = String.format("@%s discovered on %s in %s", annotation, elementName, canonicalName);
        messager.printNote(msg);
    }

    public static void fieldCreationMes(String className, TypeName typeName, String fieldName) {
        String msg = String.format("Created %s %s field in %s", typeName.toString(), fieldName, className);
        messager.printNote(msg);
    }

    public static void branchNamesMixMes(String fileName) {
        String msg = String.format(
                "Mixing default branch names with custom in one file not supported. Skipped creating %s.", fileName);
        messager.printWarning(msg);
    }

    public static void fileCreationSuccessMes(String fileName) {
        messager.printNote(String.format("%s created without failure.", fileName));
    }
}
