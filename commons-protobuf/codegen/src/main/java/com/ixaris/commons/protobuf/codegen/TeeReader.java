package com.ixaris.commons.protobuf.codegen;

/**
 * Tee-Filters a Reader into an Writer.
 *
 * @author "Martin Krischik" <martin.krischik@noser.com>
 * @version 1.0 $Revision: 1046 $
 * @since 1.0
 */
class TeeReader extends java.io.Reader {
    /**
     * Reader to read from
     */
    private final java.io.Reader in;
    /**
     * Tee output to which read date is written before beeing passed on.
     */
    private final java.io.Writer tee;
    
    /**
     * create new filter.
     *
     * @param in Reader to read from
     * @param tee Tee output to which read date is written before beeing passed on.
     */
    public TeeReader(final java.io.Reader in, final java.io.Writer tee) {
        this.in = in;
        this.tee = tee;
    } // TeeReader
    
    /**
     * Close the stream. Once a stream has been closed, further read(), ready(), mark(), or reset() invocations will
     * throw an IOException. Closing a previously-closed stream, however, has no effect.
     *
     * @throws java.io.IOException
     * @see java.io.Reader#close()
     */
    @Override
    public void close() throws java.io.IOException {
        this.in.close();
    } // close
    
    /**
     * Reads characters into a portion of an array. This method will block until some input is available, an I/O error
     * occurs, or the end of the stream is reached.
     *
     * @param cbuf Destination buffer
     * @param off Offset at which to start storing characters
     * @param len Maximum number of characters to read
     * @return The number of characters read, or -1 if the end of the stream has been reached Throws:
     * @throws java.io.IOException
     * @see java.io.Reader#read(char[], int, int)
     */
    @Override
    public int read(final char[] cbuf, final int off, final int len) throws java.io.IOException {
        final int retval = this.in.read(cbuf, off, len);
        
        if (retval >= 0) {
            this.tee.write(cbuf, off, retval);
        } else {
            this.tee.close();
        } // if
        
        return retval;
    } // read
} // TeeReader
