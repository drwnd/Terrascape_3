package core.utils;

/**
 * {@code ArrayList<Integer>} is nice and all but the performance sucks compared to this.
 */
public final class IntArrayList {

    public IntArrayList(int initialCapacity) {
        data = new int[Math.max(1, initialCapacity)];
    }

    public void set(int index, int value) {
        data[index] = value;
    }

    public void add(int value) {
        if (size == data.length) grow();
        data[size] = value;
        size++;
    }

    public void clear() {
        size = 0;
    }

    public void copyInto(int[] target, int startIndex) {
        System.arraycopy(data, 0, target, startIndex, size);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int[] getData() {
        return data;
    }

    public int[] toArray() {
        int[] array = new int[size];
        copyInto(array, 0);
        return data;
    }


    private void grow() {
        int[] newData = new int[Math.max(data.length << 1, size)];
        System.arraycopy(data, 0, newData, 0, data.length);
        data = newData;
    }

    private int[] data;
    private int size = 0;
}
