package com.ixaris.commons.misc.lib.object;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author daniel.grech
 */
@SuppressWarnings({ "resource", "IOResourceOpenedButNotSafelyClosed" })
public class SafeObjectInputStreamTest {
    
    /**
     * Serialize a {@link Tuple2} and try to read it back, using a {@link SafeObjectInputStream} that allows reading
     * {@link Tuple2}s. Should read the object back correctly.
     */
    @Test
    public void deserializeAllowedType_ShouldBeReadCorrectly() throws Exception {
        final Tuple2 original = tuple("a", "b");
        final Tuple2 deserialized = serializeAndDeserialize(original, in -> createSafeInputStream(in, Tuple2.class));
        assertEquals("Object should be read back correctly", original, deserialized);
    }
    
    /**
     * Serialize a {@link Tuple2} and try to read it back, using a {@link SafeObjectInputStream} that does not allow
     * reading any class. Should fail with a {@link InvalidClassException}.
     */
    @Test
    public void deserializeIllegalType_ShouldThrowException() {
        final Tuple2 original = tuple("a", "b");
        Assertions.assertThatThrownBy(() -> serializeAndDeserialize(original, in -> createSafeInputStream(in))).isInstanceOf(InvalidClassException.class);
    }
    
    /**
     * Wrap the creation of a new {@link SafeObjectInputStream} so it does not throw unchecked exceptions, so it can be
     * used in a lambda.
     */
    private static SafeObjectInputStream createSafeInputStream(final InputStream in, final Class<?>... allowedClasses) {
        try {
            return new SafeObjectInputStream(in, allowedClasses);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T serializeAndDeserialize(
                                                 final T t, final Function<InputStream, SafeObjectInputStream> inputStreamSupplier) throws IOException, ClassNotFoundException {
        // Write the object to a byte array to simulate serialization
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final ObjectOutput objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(t);
        objectOutputStream.flush();
        byteArrayOutputStream.flush();
        
        final byte[] written = byteArrayOutputStream.toByteArray();
        
        objectOutputStream.close();
        byteArrayOutputStream.close();
        
        // Read it back in
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(written);
        final SafeObjectInputStream safeObjectInputStream = inputStreamSupplier.apply(byteArrayInputStream);
        
        final Object read = safeObjectInputStream.readObject();
        
        safeObjectInputStream.close();
        
        return (T) read;
    }
}
