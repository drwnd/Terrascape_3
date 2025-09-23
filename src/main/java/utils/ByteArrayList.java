package utils;

/**
 * <code>ArrayList</code> is nice and all but the performance sucks compared to this.
 */
public final class ByteArrayList {

    public ByteArrayList(int initialCapacity) {
        data = new byte[Math.max(1, initialCapacity)];
    }

    public void add(byte value) {
        if (size == data.length) grow();
        data[size] = value;
        size++;
    }

    public void add(byte[] values) {
        growToMatch(size + values.length);
        System.arraycopy(values, 0, data, size, values.length);
        size += values.length;
    }

    public void copyInto(byte[] target, int startIndex) {
        System.arraycopy(data, 0, target, startIndex, size);
    }

    public int size() {
        return size;
    }

    public void set(byte value, int index) {
        data[index] = value;
    }

    public void pad(int length) {
        size += length;
        if (size >= data.length) grow();
    }

    public byte[] toByteArray() {
        byte[] data = new byte[size];
        System.arraycopy(this.data, 0, data, 0, size);
        return data;
    }

    public void clear() {
        size = 0;
    }


    private void grow() {
        byte[] newData = new byte[Math.max(data.length << 1, size)];
        System.arraycopy(data, 0, newData, 0, data.length);
        data = newData;
    }

    private void growToMatch(int length) {
        if (length <= data.length) return;
        byte[] newData = new byte[length];
        System.arraycopy(data, 0, newData, 0, data.length);
        data = newData;
    }

    private byte[] data;
    private int size = 0;
}
