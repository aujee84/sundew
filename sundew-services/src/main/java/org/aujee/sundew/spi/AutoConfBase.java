package org.aujee.sundew.spi;

import com.google.auto.service.AutoService;
import org.aujee.sundew.utils.UtilBucket;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@AutoService(AutoInitializerProvider.class)
public final class AutoConfBase implements AutoInitializerProvider {
    private static Map<String, List<String[]>> forInitDataContainer;
    private static Map<String, int[]> valueIndexContainer;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize() {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        String generatedClassName = "org.aujee.sundew.spi.AutoConfig";
        forInitDataContainer = loadGenerated(lookup, generatedClassName, "forInitDataContainer", Map.class);
        valueIndexContainer = loadGenerated(lookup, generatedClassName, "valueIndexContainer", Map.class);
        init(lookup);
    }

    @SuppressWarnings("unchecked")
    private <T> T loadGenerated(MethodHandles.Lookup lookup, String className, String fieldName, Class<T> fieldType) {
        Class<?> targetClass;
        try {
            targetClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            String mes = String.format("Unable to find %s class.%n", className);
            throw new RuntimeException(mes, UtilBucket.getRootCause(e));
        }
        try {
            VarHandle varHandle = getVarHandle(lookup, targetClass, fieldName, fieldType);
            return (T) varHandle.get();
        } catch (IllegalAccessException | NoSuchFieldException e) {
            String mes = String.format("Unable to initialize %s in %s.%n", fieldName, className);
            throw new RuntimeException(mes, UtilBucket.getRootCause(e));
        }
    }

    private void init(MethodHandles.Lookup lookup) {
        for (Map.Entry<String, List<String[]>> containerEntry : forInitDataContainer.entrySet()) {
            String fileName = containerEntry.getKey();

            try (LineNumberReader lineNumberReader = getToReadByLine(fileName).orElse(null)) {
                if (!Objects.nonNull(lineNumberReader)) {
                    System.err.printf(
                            "%s not present in src/main/resources. Copy it from generated and complete with data.%n", fileName);
                    continue;
                }
                List<String[]> elementData = containerEntry.getValue();
                int[] valueIndex = valueIndexContainer.get(fileName);
                int indexCorrector = 0;
                int elementDataIndex = 0;

                fileEnd:
                for (int currentIndex : valueIndex) {

                    while (true) {
                        int lineNumber = lineNumberReader.getLineNumber();
                        String line = lineNumberReader.readLine();
                        if (line == null) {
                            break fileEnd;
                        }
                        if (isCommentOrEmpty(line)) {
                            indexCorrector++;
                        }
                        int correctedIndex = currentIndex + indexCorrector;
                        if (correctedIndex == lineNumber) {
                            String value = extractValue(line);
                            String[] data = elementData.get(elementDataIndex);
                            varInit(lookup, data, value);
                            elementDataIndex++;
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                System.err.printf("Error raised processing %s. Check encoding. Supports only UTF-8.%n", fileName);
            }
        }
    }

    private static Optional<LineNumberReader> getToReadByLine(String fileName) {
        Optional<InputStream> resourceAsStream = Optional.ofNullable(ClassLoader.getSystemResourceAsStream(fileName));
        return resourceAsStream.map(inputStream -> new LineNumberReader(new InputStreamReader(inputStream)));
    }

    private boolean isCommentOrEmpty(String line) {
        return line.isBlank() || line.isEmpty() || line.startsWith("#") || line.startsWith("!");
    }

    //We are extracting value only from line where the value is.
    private String extractValue(String line) {
        int indexOfColonSign = line.indexOf(":");
        int indexOfEqualSign = line.indexOf("=");
        int beforeValueIndex;
        if (indexOfEqualSign == -1) {
            beforeValueIndex = indexOfColonSign;
        } else if (indexOfColonSign == -1) {
            beforeValueIndex = indexOfEqualSign;
        } else {
            beforeValueIndex = Math.min(indexOfEqualSign, indexOfColonSign);
        }
        int optionalInlineCommentIndex = line.lastIndexOf("#");
        String value = optionalInlineCommentIndex == -1 ?
                line.substring(beforeValueIndex + 1) : line.substring(beforeValueIndex + 1, optionalInlineCommentIndex);

        return value.strip();
    }

    private void varInit(MethodHandles.Lookup lookup, String[] elementData, String value) {
        Class<?> targetClass;
        Class<?> fieldType;
        try {
            targetClass = Class.forName(elementData[0]);
            fieldType = UtilBucket.getType(elementData[2]);
        } catch (ClassNotFoundException e) {
            String mes = String.format("Unable to find %s class.%n", elementData[0]);
            throw new RuntimeException(mes, UtilBucket.getRootCause(e));
        }

        String fieldName = elementData[1];
        Object parsedValue = UtilBucket.getValue(elementData[2], value);

        try {
            VarHandle varHandle = getVarHandle(lookup, targetClass, fieldName, fieldType);
            varHandle.set(parsedValue);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            String mes = String.format("Unable to initialize %s in %s.%n", fieldName, elementData[0]);
            throw new RuntimeException(mes, UtilBucket.getRootCause(e));
        }
    }

    private VarHandle getVarHandle(MethodHandles.Lookup lookup,
                                   Class<?> targetClass,
                                   String fieldName,
                                   Class<?> fieldType) throws IllegalAccessException, NoSuchFieldException {
        return MethodHandles
                .privateLookupIn(
                        targetClass,
                        lookup)
                .findStaticVarHandle(
                        targetClass,
                        fieldName,
                        fieldType);
    }
}


