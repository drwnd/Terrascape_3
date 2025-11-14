package core.utils;

import game.utils.Position;
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


    protected final void saveLong(long value) {
        data.add((byte) (value >> 56));
        data.add((byte) (value >> 48));
        data.add((byte) (value >> 40));
        data.add((byte) (value >> 32));
        data.add((byte) (value >> 24));
        data.add((byte) (value >> 16));
        data.add((byte) (value >> 8));
        data.add((byte) value);
    }

    protected final long loadLong() {
        return (long) loadByte() << 56
                | (loadByte() & 0xFFL) << 48
                | (loadByte() & 0xFFL) << 40
                | (loadByte() & 0xFFL) << 32
                | (loadByte() & 0xFFL) << 24
                | (loadByte() & 0xFFL) << 16
                | (loadByte() & 0xFFL) << 8
                | loadByte() & 0xFFL;
    }

    protected final void saveInt(int value) {
        data.add((byte) (value >> 24));
        data.add((byte) (value >> 16));
        data.add((byte) (value >> 8));
        data.add((byte) value);
    }

    protected final int loadInt() {
        return loadByte() << 24
                | (loadByte() & 0xFF) << 16
                | (loadByte() & 0xFF) << 8
                | loadByte() & 0xFF;
    }

    protected final void saveShort(short value) {
        data.add((byte) (value >> 8));
        data.add((byte) value);
    }

    protected final short loadShort() {
        return (short) ((loadByte() & 0xFF) << 8
                | (loadByte() & 0xFF));
    }

    protected final void saveByte(byte value) {
        data.add(value);
    }

    protected final byte loadByte() {
        if (currentIndex >= data.size()) {
            currentIndex++;
            return (byte) 0;
        }
        return data.get(currentIndex++);
    }

    protected final void saveBoolean(boolean value) {
        data.add((byte) (value ? 1 : 0));
    }

    protected final boolean loadBoolean() {
        return loadByte() != 0;
    }

    protected final void saveFloat(float value) {
        saveInt(Float.floatToIntBits(value));
    }

    protected final float loadFloat() {
        return Float.intBitsToFloat(loadInt());
    }

    protected final void saveByteArray(byte[] value) {
        saveInt(value.length);
        data.add(value);
    }

    protected final byte[] loadByteArray() {
        int length = loadInt();
        byte[] value = new byte[length];
        if (data.size() >= currentIndex + length)
            System.arraycopy(data.getData(), currentIndex, value, 0, length);
        currentIndex += length;
        return value;
    }

    protected final void saveVector2i(Vector2i value) {
        saveInt(value.x);
        saveInt(value.y);
    }

    protected final Vector2i loadVector2i() {
        return new Vector2i(loadInt(), loadInt());
    }

    protected final void saveVector3i(Vector3i value) {
        saveInt(value.x);
        saveInt(value.y);
        saveInt(value.z);
    }

    protected final Vector3i loadVector3i() {
        return new Vector3i(loadInt(), loadInt(), loadInt());
    }

    protected final void saveVector4i(Vector4i value) {
        saveInt(value.x);
        saveInt(value.y);
        saveInt(value.z);
        saveInt(value.w);
    }

    protected final Vector4i loadVector4i() {
        return new Vector4i(loadInt(), loadInt(), loadInt(), loadInt());
    }

    protected final void saveVector2f(Vector2f value) {
        saveFloat(value.x);
        saveFloat(value.y);
    }

    protected final Vector2f loadVector2f() {
        return new Vector2f(loadFloat(), loadFloat());
    }

    protected final void saveVector3f(Vector3f value) {
        saveFloat(value.x);
        saveFloat(value.y);
        saveFloat(value.z);
    }

    protected final Vector3f loadVector3f() {
        return new Vector3f(loadFloat(), loadFloat(), loadFloat());
    }

    protected final void saveVector4f(Vector4f value) {
        saveFloat(value.x);
        saveFloat(value.y);
        saveFloat(value.z);
        saveFloat(value.w);
    }

    protected final Vector4f loadVector4f() {
        return new Vector4f(loadFloat(), loadFloat(), loadFloat(), loadFloat());
    }

    protected final void savePosition(Position position) {
        saveVector3i(position.intPosition());
        saveVector3f(position.fractionPosition());
    }

    protected final Position loadPosition() {
        return new Position(loadVector3i(), loadVector3f());
    }

    protected final void saveString(String string) {
        saveInt(string.length());
        for (char character : string.toCharArray()) saveShort((short) character);
    }

    protected final String loadString() {
        int length = loadInt();
        StringBuilder builder = new StringBuilder(length);
        for (int counter = 0; counter < length; counter++) builder.append((char) loadShort());
        return builder.toString();
    }


    private int currentIndex;
    private final ByteArrayList data;
}
