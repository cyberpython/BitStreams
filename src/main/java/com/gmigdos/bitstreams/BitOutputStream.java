/*
 * The MIT License
 *
 * Copyright 2012 Georgios Migdos <cyberpython@gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.gmigdos.bitstreams;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Georgios Migdos <cyberpython@gmail.com>
 */
public class BitOutputStream extends FilterOutputStream {

    private static final byte BITS_PER_BYTE = 8;
    private static final long MAX_INT = 0xFFFFFFFF;
    private byte bitsInBuffer;
    private short buffer;

    public BitOutputStream(OutputStream out) {
        super(out);
        bitsInBuffer = 0;
        buffer = 0;
    }

    /**
     * Writes a the value of b to the stream using howManyBits bits.
     * If not all howManyBits bits are needed to represent b, the bit pattern 
     * written to the stream is left-padded with 0s.
     * 
     * <p><b>Close must be called in the end in order to write any remaining
     * unwritten bits and flush the underlying stream.</b></p>
     * 
     * @param b The value to write
     * @param howManyBits How many bits to write to the output. If b cannot be 
     *                    written using howManyBits an IllegalArgumentException
     *                    is thrown.
     */
    public void write(int b, int howManyBits) throws IOException {

        if ((b & MAX_INT) > ((1L << howManyBits) - 1)) {
            throw new IllegalArgumentException(b + "cannot be written in " + howManyBits + " bits");
        }

        int emptyBitsInBuffer;
        int bits;
        int len;

        int remainingBits = howManyBits;
        int offset = howManyBits;

        while (remainingBits > 0) {
            if (bitsInBuffer != BITS_PER_BYTE) {
                emptyBitsInBuffer = BITS_PER_BYTE - bitsInBuffer;
                len = (remainingBits - emptyBitsInBuffer < 0) ? remainingBits : emptyBitsInBuffer;
                offset -= len;
                bits = (b & ((~(-1 << offset + len)) & (-1 << offset))) >> offset;
                buffer |= bits << (emptyBitsInBuffer - len);
                remainingBits -= len;
                bitsInBuffer += len;
            }
            if (bitsInBuffer == BITS_PER_BYTE) {
                out.write((int) buffer);
                bitsInBuffer = 0;
                buffer = 0;
            }
        }

    }

    @Override
    /**
     * Does absolutely nothing
     */
    public void flush() throws IOException {
    }

    @Override
    public void write(int b) throws IOException {
        write(b & 0xFF, BITS_PER_BYTE);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = off; i < off + len; i++) {
            write(b[i]);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void close() throws IOException {
        if (bitsInBuffer > 0) {
            out.write((int) buffer);
        }
        out.flush();
        out.close();
    }
}
