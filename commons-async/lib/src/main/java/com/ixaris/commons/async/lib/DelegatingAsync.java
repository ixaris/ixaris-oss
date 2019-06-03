package com.ixaris.commons.async.lib;

import com.ixaris.commons.misc.lib.object.Wrapper;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

final class DelegatingAsync<T> implements Async<T>, Wrapper<CompletionStage<T>> {
    
    private final CompletionStage<T> wrapped;
    
    DelegatingAsync(final CompletionStage<T> wrapped) {
        this.wrapped = wrapped;
    }
    
    @Override
    public CompletionStage<T> unwrap() {
        return wrapped;
    }
    
    // completion stage wrapper methods
    
    @Override
    public <U> CompletionStage<U> thenApply(final Function<? super T, ? extends U> fn) {
        return wrapped.thenApply(fn);
    }
    
    @Override
    public <U> CompletionStage<U> thenApplyAsync(final Function<? super T, ? extends U> fn) {
        return wrapped.thenApplyAsync(fn);
    }
    
    @Override
    public <U> CompletionStage<U> thenApplyAsync(final Function<? super T, ? extends U> fn, final Executor executor) {
        return wrapped.thenApplyAsync(fn, executor);
    }
    
    @Override
    public CompletionStage<Void> thenAccept(final Consumer<? super T> action) {
        return wrapped.thenAccept(action);
    }
    
    @Override
    public CompletionStage<Void> thenAcceptAsync(final Consumer<? super T> action) {
        return wrapped.thenAcceptAsync(action);
    }
    
    @Override
    public CompletionStage<Void> thenAcceptAsync(final Consumer<? super T> action, final Executor executor) {
        return wrapped.thenAcceptAsync(action, executor);
    }
    
    @Override
    public CompletionStage<Void> thenRun(final Runnable action) {
        return wrapped.thenRun(action);
    }
    
    @Override
    public CompletionStage<Void> thenRunAsync(final Runnable action) {
        return wrapped.thenRunAsync(action);
    }
    
    @Override
    public CompletionStage<Void> thenRunAsync(final Runnable action, final Executor executor) {
        return wrapped.thenRunAsync(action, executor);
    }
    
    @Override
    public <U, V> CompletionStage<V> thenCombine(
        final CompletionStage<? extends U> other, final BiFunction<? super T, ? super U, ? extends V> fn
    ) {
        return wrapped.thenCombine(other, fn);
    }
    
    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(
        final CompletionStage<? extends U> other, final BiFunction<? super T, ? super U, ? extends V> fn
    ) {
        return wrapped.thenCombineAsync(other, fn);
    }
    
    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(
        final CompletionStage<? extends U> other,
        final BiFunction<? super T, ? super U, ? extends V> fn,
        final Executor executor
    ) {
        return wrapped.thenCombineAsync(other, fn, executor);
    }
    
    @Override
    public <U> CompletionStage<Void> thenAcceptBoth(
        final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action
    ) {
        return wrapped.thenAcceptBoth(other, action);
    }
    
    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(
        final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action
    ) {
        return wrapped.thenAcceptBothAsync(other, action);
    }
    
    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(
        final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action, final Executor executor
    ) {
        return wrapped.thenAcceptBothAsync(other, action, executor);
    }
    
    @Override
    public CompletionStage<Void> runAfterBoth(final CompletionStage<?> other, final Runnable action) {
        return wrapped.runAfterBoth(other, action);
    }
    
    @Override
    public CompletionStage<Void> runAfterBothAsync(final CompletionStage<?> other, final Runnable action) {
        return wrapped.runAfterBothAsync(other, action);
    }
    
    @Override
    public CompletionStage<Void> runAfterBothAsync(
        final CompletionStage<?> other, final Runnable action, final Executor executor
    ) {
        return wrapped.runAfterBothAsync(other, action, executor);
    }
    
    @Override
    public <U> CompletionStage<U> applyToEither(
        final CompletionStage<? extends T> other, final Function<? super T, U> fn
    ) {
        return wrapped.applyToEither(other, fn);
    }
    
    @Override
    public <U> CompletionStage<U> applyToEitherAsync(
        final CompletionStage<? extends T> other, final Function<? super T, U> fn
    ) {
        return wrapped.applyToEitherAsync(other, fn);
    }
    
    @Override
    public <U> CompletionStage<U> applyToEitherAsync(
        final CompletionStage<? extends T> other, final Function<? super T, U> fn, final Executor executor
    ) {
        return wrapped.applyToEitherAsync(other, fn, executor);
    }
    
    @Override
    public CompletionStage<Void> acceptEither(
        final CompletionStage<? extends T> other, final Consumer<? super T> action
    ) {
        return wrapped.acceptEither(other, action);
    }
    
    @Override
    public CompletionStage<Void> acceptEitherAsync(
        final CompletionStage<? extends T> other, final Consumer<? super T> action
    ) {
        return wrapped.acceptEitherAsync(other, action);
    }
    
    @Override
    public CompletionStage<Void> acceptEitherAsync(
        final CompletionStage<? extends T> other, final Consumer<? super T> action, final Executor executor
    ) {
        return wrapped.acceptEitherAsync(other, action, executor);
    }
    
    @Override
    public CompletionStage<Void> runAfterEither(final CompletionStage<?> other, final Runnable action) {
        return wrapped.runAfterEither(other, action);
    }
    
    @Override
    public CompletionStage<Void> runAfterEitherAsync(final CompletionStage<?> other, final Runnable action) {
        return wrapped.runAfterEitherAsync(other, action);
    }
    
    @Override
    public CompletionStage<Void> runAfterEitherAsync(
        final CompletionStage<?> other, final Runnable action, final Executor executor
    ) {
        return wrapped.runAfterEitherAsync(other, action, executor);
    }
    
    @Override
    public <U> CompletionStage<U> thenCompose(final Function<? super T, ? extends CompletionStage<U>> fn) {
        return wrapped.thenCompose(fn);
    }
    
    @Override
    public <U> CompletionStage<U> thenComposeAsync(final Function<? super T, ? extends CompletionStage<U>> fn) {
        return wrapped.thenComposeAsync(fn);
    }
    
    @Override
    public <U> CompletionStage<U> thenComposeAsync(
        final Function<? super T, ? extends CompletionStage<U>> fn, final Executor executor
    ) {
        return wrapped.thenComposeAsync(fn, executor);
    }
    
    @Override
    public CompletionStage<T> exceptionally(final Function<Throwable, ? extends T> fn) {
        return wrapped.exceptionally(fn);
    }
    
    @Override
    public CompletionStage<T> whenComplete(final BiConsumer<? super T, ? super Throwable> action) {
        return wrapped.whenComplete(action);
    }
    
    @Override
    public CompletionStage<T> whenCompleteAsync(final BiConsumer<? super T, ? super Throwable> action) {
        return wrapped.whenCompleteAsync(action);
    }
    
    @Override
    public CompletionStage<T> whenCompleteAsync(
        final BiConsumer<? super T, ? super Throwable> action, final Executor executor
    ) {
        return wrapped.whenCompleteAsync(action, executor);
    }
    
    @Override
    public <U> CompletionStage<U> handle(final BiFunction<? super T, Throwable, ? extends U> fn) {
        return wrapped.handle(fn);
    }
    
    @Override
    public <U> CompletionStage<U> handleAsync(final BiFunction<? super T, Throwable, ? extends U> fn) {
        return wrapped.handleAsync(fn);
    }
    
    @Override
    public <U> CompletionStage<U> handleAsync(
        final BiFunction<? super T, Throwable, ? extends U> fn, final Executor executor
    ) {
        return wrapped.handleAsync(fn, executor);
    }
    
    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return wrapped.toCompletableFuture();
    }
    
}
