package com.ixaris.commons.misc.lib.optional;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Utility extension methods for Java's {@link Optional}.
 *
 * @author <a href="mailto:francesco.bonaci@ixaris.com">Francesco Borg Bonaci</a>
 */
public final class OptionalUtils {
    
    private OptionalUtils() {}
    
    /**
     * If a value in the provided optional is present, performs the given action with the value; otherwise performs the
     * given empty-based action.
     *
     * <p>This can be deprecated with the release of JDK 9 as it will be available directly in {@link Optional}.
     *
     * @param optional the optional to check for a present value
     * @param action the action to be performed, if a value is present
     * @param emptyAction the empty-based action to be performed, if no value is present
     * @throws NullPointerException if a value is present and the given action is {@code null}, or no value is present
     *     and the given empty-based action is {@code null}.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> void ifPresentOrElse(
        final Optional<T> optional, final Consumer<? super T> action, final Runnable emptyAction
    ) {
        if (optional.isPresent()) {
            action.accept(optional.get());
        } else {
            emptyAction.run();
        }
    }
}
