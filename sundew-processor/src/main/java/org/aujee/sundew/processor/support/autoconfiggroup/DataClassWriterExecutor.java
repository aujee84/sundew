package org.aujee.sundew.processor.support.autoconfiggroup;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.aujee.sundew.processor.support.SourceBuilder;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class DataClassWriterExecutor {
    private static final Lock COMPARE_LOCK = new ReentrantLock();
    private static final AtomicInteger TO_CREATE_FILE_COUNTER = new AtomicInteger(0);
    private static final AtomicInteger CREATED_CODEBLOCK_COUNTER = new AtomicInteger(0);
    private static final AtomicBoolean NOT_PREPARED = new AtomicBoolean(true);
    private static final AtomicBoolean NOT_EXECUTED = new AtomicBoolean(true);
    private static final Queue<Map.Entry<String, List<String[]>>> forInitData = new ConcurrentLinkedQueue<>();
    private static final Queue<Map.Entry<String, int[]>> valueIndex = new ConcurrentLinkedQueue<>();

    //There are two static code blocks created per one file.
    static void incrementFileCount() {
        TO_CREATE_FILE_COUNTER.getAndAdd(2);
    }

    static void incrementCodeBlockTaskCount() {
        CREATED_CODEBLOCK_COUNTER.getAndIncrement();
    }

    static void addData(Map.Entry<String, List<String[]>> dataEntry) {
        forInitData.add(dataEntry);
    }

    static void addIndex(Map.Entry<String, int[]> indexEntry) {
        valueIndex.add(indexEntry);
    }

    static Map.Entry<String, List<String[]>> popData() {
        return forInitData.poll();
    }

    static Map.Entry<String, int[]> popIndex() {
        return valueIndex.poll();
    }

    static void prepare() {
        if (NOT_PREPARED.compareAndSet(true, false)) {
            DataClassWriter.prepare();
        }
    }

    static void execute() throws IOException {
        if (NOT_EXECUTED.compareAndSet(true, false)) {
            AtomicBoolean notCompleted = new AtomicBoolean(true);
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            while (notCompleted.get()) {
                executorService.submit(DataClassWriter::dataCodeBlockTask);
                executorService.submit(DataClassWriter::indexCodeBlockTask);
                COMPARE_LOCK.lock();
                try {
                    notCompleted.compareAndSet(true, TO_CREATE_FILE_COUNTER.get() != CREATED_CODEBLOCK_COUNTER.get());
                } finally {
                    COMPARE_LOCK.unlock();
                }
            }
            executorService.close();
            DataClassWriter.finish();
        }
    }
    private static class DataClassWriter {
        private static final String CLASS_DESTINY = "org.aujee.sundew.spi";
        private static final String CLASS_NAME = "AutoConfig";
        private static final Queue<CodeBlock> STATIC_CODE_BLOCKS = new ConcurrentLinkedQueue<>();
        private static final String FILE_NAME_DATA = "fileNameData";
        private static final String FILE_NAME_INDEX = "fileNameIndex";
        private static final String FOR_INIT_DATA_CONTAINER = "forInitDataContainer";
        private static final String VALUE_INDEX_CONTAINER = "valueIndexContainer";
        private static final String DATA_LIST = "dataList";
        private static final String DATA = "data";
        private static final String INDEX = "index";

        private static TypeSpec.Builder typeSpecBuilder;

        static void prepare() {
            ClassName mapClass = ClassName.get(Map.class);
            TypeName stringType = TypeName.get(String.class);
            TypeName innerForData = ParameterizedTypeName.get(List.class, String[].class);
            TypeName forDataTypeName = ParameterizedTypeName.get(mapClass, stringType, innerForData);

            FieldSpec forInitDataContainer = FieldSpec.builder(forDataTypeName, FOR_INIT_DATA_CONTAINER)
                    .addModifiers(Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T<>()", HashMap.class)
                    .build();

            TypeName forIndexTypeName = ParameterizedTypeName.get(Map.class, String.class, int[].class);

            FieldSpec valueIndexContainer = FieldSpec.builder(forIndexTypeName, VALUE_INDEX_CONTAINER)
                    .addModifiers(Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T<>()", HashMap.class)
                    .build();

            MethodSpec constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .build();

            typeSpecBuilder = TypeSpec.classBuilder(CLASS_NAME)
                    .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                    .addField(forInitDataContainer)
                    .addField(valueIndexContainer)
                    .addMethod(constructor);

            CodeBlock staticInitializerSchema = CodeBlock.builder()
                    .addStatement("$T $N", String.class, FILE_NAME_DATA)
                    .addStatement("$T $N", innerForData, DATA_LIST)
                    .addStatement("$T $N", String[].class, DATA)
                    .addStatement("$T $N", String.class, FILE_NAME_INDEX)
                    .addStatement("$T $N", int[].class, INDEX)
                    .build();

            STATIC_CODE_BLOCKS.add(staticInitializerSchema);
        }

        static void finish() throws IOException {
            TypeSpec typeSpec = typeSpecBuilder
                    .addStaticBlock(CodeBlock.join(STATIC_CODE_BLOCKS, "\n"))
                    .build();

            SourceBuilder.create()
                    .buildJavaSource(CLASS_DESTINY, typeSpec)
                    .writeToFiler();
        }

        static void dataCodeBlockTask() {
            Map.Entry<String, List<String[]>> head = popData();
            if (head == null){return;}
            String fileName = head.getKey();
            List<String[]> dataList = head.getValue();
            List<CodeBlock> codeBlocks = new LinkedList<>();
            TypeName linkedListType = ParameterizedTypeName.get(LinkedList.class);

            CodeBlock initBlock = CodeBlock.builder()
                    .addStatement("$N = $S", FILE_NAME_DATA, fileName)
                    .addStatement("$N = new $T()", DATA_LIST, linkedListType)
                    .build();
            codeBlocks.add(initBlock);

            List<CodeBlock> changeableBlock = dataList.stream()
                    .map(arr ->
                            CodeBlock.builder()
                                    .addStatement("$N = new String[3]", DATA)
                                    .addStatement("$N[0] = $S", DATA, arr[0])
                                    .addStatement("$N[1] = $S", DATA, arr[1])
                                    .addStatement("$N[2] = $S", DATA, arr[2])
                                    .addStatement("$N.add($N)", DATA_LIST, DATA)
                                    .build())
                    .toList();
            codeBlocks.addAll(changeableBlock);

            CodeBlock finishBlock = CodeBlock.builder()
                    .addStatement("$N.put($N, $N)", FOR_INIT_DATA_CONTAINER, FILE_NAME_DATA, DATA_LIST)
                    .build();
            codeBlocks.add(finishBlock);

            STATIC_CODE_BLOCKS.add(CodeBlock.join(codeBlocks, "\n"));
            incrementCodeBlockTaskCount();
        }

        static void indexCodeBlockTask() {
            Map.Entry<String, int[]> head = popIndex();
            if (head == null){return;}
            String fileName = head.getKey();
            int[] index = head.getValue();
            int indexLength = index.length;
            List<CodeBlock> codeBlocks = new LinkedList<>();

            CodeBlock initBlock = CodeBlock.builder()
                    .addStatement("$N = $S", FILE_NAME_INDEX, fileName)
                    .addStatement("$N = new int[$L]", INDEX, indexLength)
                    .build();
            codeBlocks.add(initBlock);

            List<CodeBlock> arrayInitBlocks = new LinkedList<>();

            for (int pointer = 0; pointer < indexLength; pointer++) {
                int onPointValue = index[pointer];
                arrayInitBlocks.add(CodeBlock.builder()
                        .addStatement("$N[$L] = $L", INDEX, pointer, onPointValue)
                        .build());
            }
            codeBlocks.addAll(arrayInitBlocks);

            CodeBlock finishBlock = CodeBlock.builder()
                    .addStatement("$N.put($N, $N)", VALUE_INDEX_CONTAINER, FILE_NAME_INDEX, INDEX)
                    .build();
            codeBlocks.add(finishBlock);

            STATIC_CODE_BLOCKS.add(CodeBlock.join(codeBlocks, "\n"));
            incrementCodeBlockTaskCount();
        }
    }
}
