package game.server.saving;

import core.utils.ByteArrayList;
import core.utils.FileManager;
import game.utils.Position;
import org.joml.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public abstract class Saver<T> {

    public Saver() {
        toSaveData = new ByteArrayList(64);
    }

    public Saver(int expectedByteSize) {
        toSaveData = new ByteArrayList(expectedByteSize);
    }

    public final synchronized void save(T object, String filepath) {
        toSaveData.clear();
        saveInt(getVersionNumber());
        save(object);
        File saveFile = FileManager.loadAndCreateFile(filepath);
        try {
            FileOutputStream writer = new FileOutputStream(saveFile);
            writer.write(toSaveData.getData(), 0, toSaveData.size());
            writer.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public final synchronized T load(String filepath) {
        File saveFile = new File(filepath);
        if (!saveFile.exists()) return getDefault();
        try {
            FileInputStream reader = new FileInputStream(saveFile);
            readData = reader.readAllBytes();
            reader.close();
        } catch (IOException exception) {
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
        currentIndex = 0;
        int saveVersionNumber = loadInt();
        if (saveVersionNumber != getVersionNumber()) return loadOldVersion(saveVersionNumber);
        return load();
    }


    /**
     * This method is intended for internal use only. Use {@code save(T object, String filepath)} instead.
     * <p>
     * DO NOT CALL THIS METHOD!
     */
    abstract void save(T object);

    /**
     * This method is intended for internal use only. Use {@code load(String filepath)} instead.
     * <p>
     * DO NOT CALL THIS METHOD!
     */
    abstract T load();

    abstract T getDefault();

    abstract int getVersionNumber();

    /**
     * Override this method if you want to bother loading old files.
     * @param versionNumber The version number of the savefile.
     */
    T loadOldVersion(int versionNumber) {
        return getDefault();
    }


    final void saveLong(long value) {
        toSaveData.add((byte) (value >> 56));
        toSaveData.add((byte) (value >> 48));
        toSaveData.add((byte) (value >> 40));
        toSaveData.add((byte) (value >> 32));
        toSaveData.add((byte) (value >> 24));
        toSaveData.add((byte) (value >> 16));
        toSaveData.add((byte) (value >> 8));
        toSaveData.add((byte) value);
    }

    final long loadLong() {
        return (long) loadByte() << 56
                | (loadByte() & 0xFFL) << 48
                | (loadByte() & 0xFFL) << 40
                | (loadByte() & 0xFFL) << 32
                | (loadByte() & 0xFFL) << 24
                | (loadByte() & 0xFFL) << 16
                | (loadByte() & 0xFFL) << 8
                | loadByte() & 0xFFL;
    }

    final void saveInt(int value) {
        toSaveData.add((byte) (value >> 24));
        toSaveData.add((byte) (value >> 16));
        toSaveData.add((byte) (value >> 8));
        toSaveData.add((byte) value);
    }

    final int loadInt() {
        return loadByte() << 24
                | (loadByte() & 0xFF) << 16
                | (loadByte() & 0xFF) << 8
                | loadByte() & 0xFF;
    }

    final void saveShort(short value) {
        toSaveData.add((byte) (value >> 8));
        toSaveData.add((byte) value);
    }

    final short loadShort() {
        return (short) ((loadByte() & 0xFF) << 8
                | (loadByte() & 0xFF));
    }

    final void saveByte(byte value) {
        toSaveData.add(value);
    }

    final byte loadByte() {
        if (currentIndex >= readData.length) {
            currentIndex++;
            return (byte) 0;
        }
        return readData[currentIndex++];
    }

    final void saveBoolean(boolean value) {
        toSaveData.add((byte) (value ? 1 : 0));
    }

    final boolean loadBoolean() {
        return loadByte() != 0;
    }

    final void saveFloat(float value) {
        saveInt(Float.floatToIntBits(value));
    }

    final float loadFloat() {
        return Float.intBitsToFloat(loadInt());
    }

    final void saveByteArray(byte[] value) {
        saveInt(value.length);
        toSaveData.add(value);
    }

    final byte[] loadByteArray() {
        int length = loadInt();
        byte[] value = new byte[length];
        if (readData.length >= currentIndex + length)
            System.arraycopy(readData, currentIndex, value, 0, length);
        currentIndex += length;
        return value;
    }

    final void saveVector2i(Vector2i value) {
        saveInt(value.x);
        saveInt(value.y);
    }

    final Vector2i loadVector2i() {
        return new Vector2i(loadInt(), loadInt());
    }

    final void saveVector3i(Vector3i value) {
        saveInt(value.x);
        saveInt(value.y);
        saveInt(value.z);
    }

    final Vector3i loadVector3i() {
        return new Vector3i(loadInt(), loadInt(), loadInt());
    }

    final void saveVector4i(Vector4i value) {
        saveInt(value.x);
        saveInt(value.y);
        saveInt(value.z);
        saveInt(value.w);
    }

    final Vector4i loadVector4i() {
        return new Vector4i(loadInt(), loadInt(), loadInt(), loadInt());
    }

    final void saveVector2f(Vector2f value) {
        saveFloat(value.x);
        saveFloat(value.y);
    }

    final Vector2f loadVector2f() {
        return new Vector2f(loadFloat(), loadFloat());
    }

    final void saveVector3f(Vector3f value) {
        saveFloat(value.x);
        saveFloat(value.y);
        saveFloat(value.z);
    }

    final Vector3f loadVector3f() {
        return new Vector3f(loadFloat(), loadFloat(), loadFloat());
    }

    final void saveVector4f(Vector4f value) {
        saveFloat(value.x);
        saveFloat(value.y);
        saveFloat(value.z);
        saveFloat(value.w);
    }

    final Vector4f loadVector4f() {
        return new Vector4f(loadFloat(), loadFloat(), loadFloat(), loadFloat());
    }

    final void savePosition(Position position) {
        saveVector3i(position.intPosition());
        saveVector3f(position.fractionPosition());
    }

    final Position loadPosition() {
        return new Position(loadVector3i(), loadVector3f());
    }


    private int currentIndex;
    private byte[] readData;
    private final ByteArrayList toSaveData;
}
