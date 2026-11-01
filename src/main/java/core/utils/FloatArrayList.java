package core.utils;

/**
 * {@code ArrayList<Float>} is nice and all, but the performance sucks compared to this.
 */
public final class FloatArrayList {

    public FloatArrayList(int initialCapacity) {
        data = new float[Math.max(1, initialCapacity)];
    }

    public void set(int index, float value) {
        data[index] = value;
    }

    public void add(float value) {
        if (size == data.length) grow();
        data[size] = value;
        size++;
    }

    public void addAll(float[] values) {
        if (size + values.length >= data.length) growAtLeast(size + values.length);
        System.arraycopy(values, 0, data, size, values.length);
        size += values.length;
    }

    public void clear() {
        size = 0;
    }

    public void copyInto(float[] target, int startIndex) {
        System.arraycopy(data, 0, target, startIndex, size);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public float[] getData() {
        return data;
    }

    public float[] trimmed() {
        float[] trimmedData = new float[size];
        System.arraycopy(data, 0, trimmedData, 0, size);
        return trimmedData;
    }


    private void grow() {
        float[] newData = new float[Math.max(data.length << 1, size)];
        System.arraycopy(data, 0, newData, 0, size);
        data = newData;
    }

    private void growAtLeast(int requiredSize) {
        float[] newData = new float[MathUtils.max(data.length << 1, size, requiredSize)];
        System.arraycopy(data, 0, newData,0, size);
        data = newData;
    }

    private float[] data;
    private int size = 0;
}

