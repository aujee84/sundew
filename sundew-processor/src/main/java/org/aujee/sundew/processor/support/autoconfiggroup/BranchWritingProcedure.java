package org.aujee.sundew.processor.support.autoconfiggroup;

import java.util.function.Consumer;

class BranchWritingProcedure<V> {
    private final Consumer<V> defaultBranchWritingProcedure;
    private final Consumer<V> customBranchWritingProcedure;

    BranchWritingProcedure(final Consumer<V> defaultBranchWritingProcedure,
                           final Consumer<V> customBranchWritingProcedure) {
        this.defaultBranchWritingProcedure = defaultBranchWritingProcedure;
        this.customBranchWritingProcedure = customBranchWritingProcedure;
    }

    void proceedWithDefaultBranching(V elementData) {
        defaultBranchWritingProcedure.accept(elementData);
    }

    void proceedWithCustomBranching(V elementData) {
        customBranchWritingProcedure.accept(elementData);
    }
}
