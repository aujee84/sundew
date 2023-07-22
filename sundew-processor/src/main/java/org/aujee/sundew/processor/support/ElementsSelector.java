package org.aujee.sundew.processor.support;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

/**
 * Thread safe. ElementsSelector can be created within a class that has access to inputElements and selector.
 * Two ways of applying rule are provided. If you write a single specialized class that holds the rule,
 * setRule will be used. Else rule can be applied with constructor within the base class.
 */

//TODO - provide external caching of selected elements to make ElementsProvider stateless.

public final class ElementsSelector<T, U, R> {
   private final Lock lock = new ReentrantLock();
   private final T inElements;
   private final BiFunction<T, U, R> selector;
   private R outElements = null;
   private U rule = null;

   public ElementsSelector(final T inElements,
                           final BiFunction<T, U, R> selector) {
      this.inElements = inElements;
      this.selector = selector;
   }

   public ElementsSelector(final T inElements, final BiFunction<T, U, R> selector, final U rule) {
      this(inElements, selector);
      this.rule = rule;
   }

   public void setRule(U rule) throws RuntimeException {
      lock.lock();
      try {
         if (this.rule != null) {
            throw new IllegalStateException(
                    "Rule already set. Rule once set is fixed to the Elements Provider object and can not be changed.");
         }
         this.rule = rule;
      } finally {
         lock.unlock();
      }
   }

   public R getSelected() {
      lock.lock();
      try {
         if (outElements == null) {
            outElements = selector.apply(inElements, rule);
         }
         return outElements;
      } finally {
         lock.unlock();
      }
   }
}
