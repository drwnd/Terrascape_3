package core.utils;

/**
 * {@code ArrayList<Byte>} is nice and all but the performance sucks compared to this.
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

    public byte[] getData() {
        return data;
    }

    public byte[] toArray() {
        byte[] data = new byte[size];
        copyInto(data, 0);
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        size = data.length;
    }

    public void set(byte value, int index) {
        data[index] = value;
    }

    public byte get(int index) {
        return data[index];
    }

    public void pad(int length) {
        size += length;
        if (size >= data.length) grow();
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
