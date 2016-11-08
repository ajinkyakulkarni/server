/*
 * MongoWP - ToroDB-poc: common
 * Copyright © 2014 8Kdata Technology (www.8kdata.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.torodb.common.util;

import com.google.common.base.Optional;
import java.util.concurrent.Callable;
import java.util.function.IntBinaryOperator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 *
 */
public class RetryHelper {
	
    private RetryHelper() {}

    public static <R> R retry(@Nonnull ExceptionHandler<R, RuntimeException> handler, Callable<R> job) {
        return retryOrThrow(handler, job);
    }

    public static <R, T extends Exception> R retryOrThrow(@Nonnull ExceptionHandler<R, T> handler, Callable<R> job) throws T {
        RetryCallback<R> retryCallback = null;
        int attempts = 0;
        do {
            try {
                return job.call();
            } catch (Exception ex) {
                attempts++;
                if (retryCallback == null) {
                    retryCallback = new RetryCallback<>();
                }
                handler.handleException(retryCallback, ex, attempts);
                switch (retryCallback.action) {
                    case RETURN: {
                        return retryCallback.result;
                    }
                    default:
                }
            }
        } while(true);
    }

    public static class RetryCallback<Result> {
        @Nonnull
        RetryAction action = RetryAction.RETRY;
        @Nullable
        Result result;

        public void doRetry() {
            action = RetryAction.RETRY;
        }

        public void doReturn(Result result) {
            this.result = result;
            action = RetryAction.RETURN;
        }

        public RetryAction getAction() {
            return action;
        }

        public Result getResult() {
            return result;
        }

    }

    public static enum RetryAction {
        RETRY,
        RETURN;
    }

    public static interface ExceptionHandler<Result, T extends Exception> {
        void handleException(RetryCallback<Result> callback, Exception t, int attempts) throws T;
    }

    public static <R, T extends Exception> ExceptionHandler<R, T> throwHandler() {
        return ThrowExceptionHandler.getInstance();
    }

    public static <R, T extends Exception> ExceptionHandler<R, T> alwaysRetryHandler() {
        return AlwaysRetryExceptionHandler.getInstance();
    }

    public static <R, T extends Exception> ExceptionHandler<R, T> defaultValueHandler(R defaultResult) {
        return new DefaultValueExceptionHandler<>(defaultResult);
    }

    public static <R, T extends Exception> ExceptionHandler<R, T> retryUntilHandler(int maxAttempts, R defaultValue) {
        ExceptionHandler<R, T> beforeHandler = alwaysRetryHandler();
        ExceptionHandler<R, T> afterHandler = defaultValueHandler(defaultValue);
        return new UntilAttemptsExceptionHandler<>(maxAttempts, beforeHandler, afterHandler);
    }

    public static <R, T extends Exception> ExceptionHandler<R, T> retryUntilHandler(int maxAttempts, ExceptionHandler<R, T> afterHandler) {
        ExceptionHandler<R, T> beforeHandler = alwaysRetryHandler();
        return new UntilAttemptsExceptionHandler<>(maxAttempts, beforeHandler, afterHandler);
    }

    public static <R, T extends Exception> ExceptionHandler<R, T> waitExceptionHandler(int millis) {
        return new FixedMillisWaitExceptionHandler<>(millis);
    }

    public static <R, CT extends Exception, T2 extends Exception> StorerExceptionHandler<R, CT, T2> storerExceptionHandler(Class<CT> excetionClass, ExceptionHandler<R, T2> delegate) {
        return new StorerExceptionHandler<>(delegate, excetionClass);
    }

    public static class ThrowExceptionHandler<R, T extends Exception> implements ExceptionHandler<R, T> {
        @SuppressWarnings("rawtypes")
        private static final ThrowExceptionHandler INSTANCE = new ThrowExceptionHandler();

        @SuppressWarnings("unchecked")
        private static <R2, T2 extends Exception> ThrowExceptionHandler<R2, T2> getInstance() {
            return INSTANCE;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleException(RetryCallback<R> callback, Exception t, int attempts) throws T {
            try {
                throw (T) t;
            } catch(Exception throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    public static class AlwaysRetryExceptionHandler<R, T extends Exception> implements ExceptionHandler<R, T> {
        @SuppressWarnings("rawtypes")
        private static final AlwaysRetryExceptionHandler INSTANCE = new AlwaysRetryExceptionHandler();

        @SuppressWarnings("unchecked")
        private static <R2, T2 extends Exception> AlwaysRetryExceptionHandler<R2, T2> getInstance() {
            return INSTANCE;
        }

        @Override
        public void handleException(RetryCallback<R> callback, Exception t, int attempts) throws T {
            callback.doRetry();
        }
    }

    public static class DefaultValueExceptionHandler<R, T extends Exception> implements ExceptionHandler<R, T> {

        private final R defaultValue;

        public DefaultValueExceptionHandler(R defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public void handleException(RetryCallback<R> callback, Exception t, int attempts) throws T {
            callback.doReturn(defaultValue);
        }

    }

    public static class DelegateExceptionHandler<Result, T extends Exception> implements ExceptionHandler<Result, T> {
        private final ExceptionHandler<Result, T> delegate;

        public DelegateExceptionHandler(ExceptionHandler<Result, T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void handleException(RetryCallback<Result> callback, Exception t, int attempts) throws T {
            delegate.handleException(callback, t, attempts);
        }
    }

    public static class UntilAttemptsExceptionHandler<Result, T extends Exception> implements ExceptionHandler<Result, T> {
        private final int maxAttempts;
        private final ExceptionHandler<Result, T> beforeLimitDelegate;
        private final ExceptionHandler<Result, T> afterLimitDelegate;

        public UntilAttemptsExceptionHandler(int maxAttempts,
                ExceptionHandler<Result, T> beforeLimitDelegate,
                ExceptionHandler<Result, T> afterLimitDelegate) {
            this.maxAttempts = maxAttempts;
            this.beforeLimitDelegate = beforeLimitDelegate;
            this.afterLimitDelegate = afterLimitDelegate;
        }

        @Override
        public void handleException(RetryCallback<Result> callback, Exception t, int attempts) throws T {
            if (attempts < maxAttempts) {
                beforeLimitDelegate.handleException(callback, t, attempts);
            }
            else {
                afterLimitDelegate.handleException(callback, t, attempts);
            }
        }
    }

    public static class FixedMillisWaitExceptionHandler<Result, T extends Exception> implements ExceptionHandler<Result, T> {
        private final int millis;

        public FixedMillisWaitExceptionHandler(int millis) {
            this.millis = millis;
        }

        @Override
        public void handleException(RetryCallback<Result> callback, Exception t, int attempts) throws T {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
                Thread.interrupted();
            }
            callback.doRetry();
        }
    }

    @NotThreadSafe
    public static class IncrementalWaitExceptionHandler<Result, T extends Exception> extends DelegateExceptionHandler<Result, T> {
        private final IntBinaryOperator newMillisFunction;
        private int currentMillis;

        /**
         *
         * @param newMillisFunction the first argument is the millis that it has wait on the last
         *                          iteration (or 0 for the first) and the second the attempts. The
         *                          result is the number of millis that will wait or a negative
         *                          number if it should give up.
         * @param delegate          the exception handler on which delegate when giving up.
         */
        public IncrementalWaitExceptionHandler(IntBinaryOperator newMillisFunction, ExceptionHandler<Result, T> delegate) {
            super(delegate);
            this.newMillisFunction = newMillisFunction;
            this.currentMillis = 1;
        }

        @Override
        public void handleException(RetryCallback<Result> callback, Exception t, int attempts)
                throws T {
            try {
                currentMillis = newMillisFunction.applyAsInt(currentMillis, attempts);
                if (currentMillis < 0) {
                    super.handleException(callback, t, attempts);
                } else {
                    if (currentMillis > 0) {
                        Thread.sleep(currentMillis);
                    }
                    callback.doRetry();
                }
            } catch (InterruptedException ex) {
                Thread.interrupted();
            }
        }
    }

    public static class StorerExceptionHandler<R, CT extends Exception, T2 extends Exception> extends DelegateExceptionHandler<R, T2> {

        private final Class<CT> exClass;
        private Optional<CT> thrown;

        public StorerExceptionHandler(ExceptionHandler<R, T2> delegate, Class<CT> exClass) {
            super(delegate);
            this.exClass = exClass;
            thrown = Optional.absent();
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handleException(RetryCallback<R> callback, Exception t, int attempts) throws T2 {
            if (exClass.isInstance(t)) {
                thrown = Optional.of((CT) t);
            }
            else {
                super.handleException(callback, t, attempts);
            }
        }

        public Optional<CT> getThrown() {
            return thrown;
        }

    }
}
