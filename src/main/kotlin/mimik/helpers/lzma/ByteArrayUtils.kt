package mimik.helpers.lzma

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class OpenByteArrayInputStream(buf: ByteArray?) : ByteArrayInputStream(buf) {
    /**
     * An array of bytes that was provided
     * by the creator of the stream. Elements <code>buf[0]</code>
     * through <code>buf[count-1]</code> are the
     * only bytes that can ever be read from the
     * stream;  element <code>buf[pos]</code> is
     * the next byte to be read.
     */
    var Buffer
        get() = buf.take(count).toByteArray()
        set(value) {
            buf = value
        }

    /**
     * The index of the next character to read from the input stream buffer.
     * This value should always be nonnegative
     * and not larger than the value of <code>count</code>.
     * The next byte to be read from the input stream buffer
     * will be <code>buf[pos]</code>.
     */
    var Position
        get() = pos
        set(value) {
            if (value in 0 until count)
                pos = value
        }

    /**
     * The currently marked position in the stream.
     * ByteArrayInputStream objects are marked at position zero by
     * default when constructed.  They may be marked at another
     * position within the buffer by the <code>mark()</code> method.
     * The current buffer position is set to this point by the
     * <code>reset()</code> method.
     * <p>
     * If no mark has been set, then the value of mark is the offset
     * passed to the constructor (or 0 if the offset was not supplied).
     *
     * @since JDK1.1
     */
    var Mark
        get() = mark
        set(value) {
            if (value in 0 until count)
                mark = value
        }

    /**
     * The index one greater than the last valid character in the input
     * stream buffer.
     * This value should always be nonnegative
     * and not larger than the length of <code>buf</code>.
     * It  is one greater than the position of
     * the last byte within <code>buf</code> that
     * can ever be read  from the input stream buffer.
     */
    var Size
        get() = count
        set(value) {
            if (value in buf.indices)
                count = value
        }

    /**
     * Returns the currently read data
     */
    val ReadData get() = buf.take(pos).toByteArray()

    /**
     * Returns the unread data
     */
    val UnreadData get() = buf.drop(pos).toByteArray()
}

class OpenByteArrayOutputStream() : ByteArrayOutputStream() {
    /**
     * The buffer where data is stored.
     */
    var Buffer
        get() = buf.take(count).toByteArray()
        set(value) {
            buf = value
        }

    /**
     * The number of valid bytes in the buffer.
     */
    var Size
        get() = count
        set(value) {
            if (value in buf.indices)
                count = value
        }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    val Max_Array_Size get() = Int.MAX_VALUE - 8
}
