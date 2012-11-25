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
package bitstreams;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A FilterInputStream that reads sequences of up to 64 bits.
 * 
 * @author Georgios Migdos <cyberpython@gmail.com>
 */
public final class BitInputStream extends FilterInputStream {
    

    private static final byte BITS_PER_BYTE = 8;
    private byte bitsInBuffer;
    private short buffer;
    private byte bitsInBufferWhenMarked;
    private short bufferWhenMarked;

    public BitInputStream(InputStream in) {
        super(in);
        bitsInBuffer = 0;
        buffer = 0;
        bitsInBufferWhenMarked = 0;
        bufferWhenMarked = 0;
    }

    /**
     * Reads howManyBits bits from the underlying stream
     * @param howManyBits The number of bits to read from the stream.<br>
     *                   If howManyBits is less than 0 or greater than 32 an
     *                   IllegalArgumentException is thrown.
     * 
     * @return A long containing the bits read.
     * 
     * @throws EOFException If EOF was reached before reading the requested 
     *                      number of bits.
     * @throws IOException If an I/O error occurs.
     */
    public long read(int howManyBits) throws IOException, EOFException {

        if (howManyBits < 1 || howManyBits > 32) {
            throw new IllegalArgumentException("Bits to read must be between 1 and 32");
        }

        int result = 0;
        int remainingBitsToRead = howManyBits;
        
        int available;
        int lastBitsRead;

        while (remainingBitsToRead > 0) {
            if (bitsInBuffer <= 0) {
                if ((buffer = (short) in.read()) == -1) {
                    throw new EOFException();
                }
                bitsInBuffer = BITS_PER_BYTE;
            }
            available = (remainingBitsToRead - bitsInBuffer < 0)?remainingBitsToRead:bitsInBuffer ;
            
            // Read available bits from the buffer:
            lastBitsRead = buffer >> (BITS_PER_BYTE - available);
            remainingBitsToRead -= available;
            
            //Update the result:
            result |= lastBitsRead << remainingBitsToRead;
            
            // Delete the read bits from the buffer:
            buffer = (short) ((buffer << available) & 255);
            bitsInBuffer -= available;
        }
        return result;
    }

    @Override
    public int read() throws IOException {
        try{
            return (int) read(BITS_PER_BYTE);
        }catch(EOFException e){
            return -1;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len <= 0) {return 0;}
        int c = read();
        if (c == -1) { return -1; }
        
        b[off] = (byte) c;
        int i = 1;
        
        while (i < len) {
            c = read();
            if (c == -1) { break;}
            if (b != null) {
                b[off + i] = (byte) c;
            }
            i++;
        }
        
        return i;
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
        bitsInBuffer = bitsInBufferWhenMarked;
        buffer = bufferWhenMarked;
    }

    @Override
    public synchronized void mark(int readlimit) {
        super.mark(readlimit);
        bitsInBufferWhenMarked = bitsInBuffer;
        bufferWhenMarked = buffer;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public long skip(long n) throws IOException {
        long count;
        for (count = 0; count < n; count++) {
            if (read() == -1) {
                break;
            }
        }
        return count;
    }
}

