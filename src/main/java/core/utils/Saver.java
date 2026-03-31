package core.utils;

import org.joml.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public abstract class Saver<T> {

    public Saver() {
        data = new ByteArrayList(64);
    }

    public Saver(int expectedByteSize) {
        data = new ByteArrayList(expectedByteSize + 4);
    }

    public final void save(T object, String filepath) {
        data.clear();
        saveInt(getVersionNumber());
        save(object);
        File saveFile = FileManager.loadAndCreateFile(filepath);
        try {
            FileOutputStream writer = new FileOutputStream(saveFile);
            writer.write(data.getData(), 0, data.size());
            writer.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public final T load(String filepath) {
        File saveFile = new File(filepath);
        if (!saveFile.exists()) return getDefault();
        try {
            FileInputStream reader = new FileInputStream(saveFile);
            data.setData(reader.readAllBytes());
            reader.close();
        } catch (IOException exception) {
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
        currentIndex = 0;
        int savedVersionNumber = loadInt();
        if (savedVersionNumber != getVersionNumber()) return loadOldVersion(savedVersionNumber);
        return load();
    }


    /**
     * This method is intended for internal use only. Use {@code save(T object, String filepath)} instead.
     * <p>
     * DO NOT CALL THIS METHOD!
     */
    protected abstract void save(T object);

    /**
     * This method is intended for internal use only. Use {@code load(String filepath)} instead.
     * <p>
     * DO NOT CALL THIS METHOD!
     */
    protected abstract T load();

    protected abstract T getDefault();

    protected abstract int getVersionNumber();

    /**
     * Override this method if you want to bother loading old files.
     *
     * @param versionNumber The version number of the savefile.
     */
    protected T loadOldVersion(int versionNumber) {
        return getDefault();
    }


    public final void saveLong(long value) {
        data.add((byte) (value >> 56));
        data.add((byte) (value >> 48));
        data.add((byte) (value >> 40));
        data.add((byte) (value >> 32));
        data.add((byte) (value >> 24));
        data.add((byte) (value >> 16));
        data.add((byte) (value >> 8));
        data.add((byte) value);
    }

    public final long loadLong() {
        return (long) loadByte() << 56
                | (loadByte() & 0xFFL) << 48
                | (loadByte() & 0xFFL) << 40
                | (loadByte() & 0xFFL) << 32
                | (loadByte() & 0xFFL) << 24
                | (loadByte() & 0xFFL) << 16
                | (loadByte() & 0xFFL) << 8
                | loadByte() & 0xFFL;
    }

    public final void saveInt(int value) {
        data.add((byte) (value >> 24));
        data.add((byte) (value >> 16));
        data.add((byte) (value >> 8));
        data.add((byte) value);
    }

    public final int loadInt() {
        return loadByte() << 24
                | (loadByte() & 0xFF) << 16
                | (loadByte() & 0xFF) << 8
                | loadByte() & 0xFF;
    }

    public final void saveShort(short value) {
        data.add((byte) (value >> 8));
        data.add((byte) value);
    }

    public final short loadShort() {
        return (short) ((loadByte() & 0xFF) << 8
                | (loadByte() & 0xFF));
    }

    public final void saveByte(byte value) {
        data.add(value);
    }

    public final byte loadByte() {
        if (currentIndex >= data.size()) {
            currentIndex++;
            return (byte) 0;
        }
        return data.get(currentIndex++);
    }

    public final void saveBoolean(boolean value) {
        data.add((byte) (value ? 1 : 0));
    }

    public final boolean loadBoolean() {
        return loadByte() != 0;
    }

    public final void saveFloat(float value) {
        saveInt(Float.floatToIntBits(value));
    }

    public final float loadFloat() {
        return Float.intBitsToFloat(loadInt());
    }

    public final void saveByteArray(byte[] value) {
        saveInt(value.length);
        data.add(value);
    }

    public final byte[] loadByteArray() {
        int length = loadInt();
        byte[] value = new byte[length];
        if (data.size() >= currentIndex + length)
            System.arraycopy(data.getData(), currentIndex, value, 0, length);
        currentIndex += length;
        return value;
    }

    public final void saveVector2i(Vector2i value) {
        saveInt(value.x);
        saveInt(value.y);
    }

    public final Vector2i loadVector2i() {
        return new Vector2i(loadInt(), loadInt());
    }

    public final void saveVector3i(Vector3i value) {
        saveInt(value.x);
        saveInt(value.y);
        saveInt(value.z);
    }

    public final Vector3i loadVector3i() {
        return new Vector3i(loadInt(), loadInt(), loadInt());
    }

    public final void saveVector4i(Vector4i value) {
        saveInt(value.x);
        saveInt(value.y);
        saveInt(value.z);
        saveInt(value.w);
    }

    public final Vector4i loadVector4i() {
        return new Vector4i(loadInt(), loadInt(), loadInt(), loadInt());
    }

    public final void saveVector2f(Vector2f value) {
        saveFloat(value.x);
        saveFloat(value.y);
    }

    public final Vector2f loadVector2f() {
        return new Vector2f(loadFloat(), loadFloat());
    }

    public final void saveVector3f(Vector3f value) {
        saveFloat(value.x);
        saveFloat(value.y);
        saveFloat(value.z);
    }

    public final Vector3f loadVector3f() {
        return new Vector3f(loadFloat(), loadFloat(), loadFloat());
    }

    public final void saveVector4f(Vector4f value) {
        saveFloat(value.x);
        saveFloat(value.y);
        saveFloat(value.z);
        saveFloat(value.w);
    }

    public final Vector4f loadVector4f() {
        return new Vector4f(loadFloat(), loadFloat(), loadFloat(), loadFloat());
    }

    public final void saveString(String string) {
        saveInt(string.length());
        for (char character : string.toCharArray()) saveShort((short) character);
    }

    public final String loadString() {
        int length = loadInt();
        StringBuilder builder = new StringBuilder(length);
        for (int counter = 0; counter < length; counter++) builder.append((char) loadShort());
        return builder.toString();
    }

    public final <V> void saveGeneric(GenericSaver<V> savable) {
        savable.save(this);
    }

    public final <V> V loadGeneric(GenericLoader<V> loadable) {
        return loadable.load(this);
    }

    private int currentIndex;
    private final ByteArrayList data;

    public interface GenericLoader<T> {
        T load(Saver<?> saver);
    }

    public interface GenericSaver<T> {
        void save(Saver<?> saver);
    }
}
