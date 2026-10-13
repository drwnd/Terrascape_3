package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.MainThread;
import core.utils.MathUtils;
import core.utils.Saver;
import core.utils.Vector3l;

import game.language.UiMessages;
import game.player.interaction.PlaceMode;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;
import game.player.sound.PlaceBreakSound;
import game.server.Chunk;
import game.server.Game;
import game.server.generation.Structure;
import game.server.materials_data.MaterialsData;
import game.server.saving.ChunkSaver;
import game.settings.IntSettings;
import game.settings.OptionSettings;
import game.utils.Utils;

import org.joml.Vector2f;
import org.joml.primitives.AABBi;
import org.joml.primitives.Intersectionf;

import java.util.ArrayList;
import java.util.Objects;

import static game.utils.Constants.*;

public final class CapsulePlaceable extends ShapePlaceable {

    public CapsulePlaceable(byte material) {
        super(null, material);
        this.material = material;
        loadSettings();
    }

    public static void offsetPositions(Vector3l startPosition, Vector3l endPosition) {
        int length = 1 << IntSettings.BREAK_PLACE_ALIGN.value();
        int startMask = -length;

        startPosition.x &= startMask;
        startPosition.y &= startMask;
        startPosition.z &= startMask;

        endPosition.x -= MathUtils.mod(endPosition.x - startPosition.x, length);
        endPosition.y -= MathUtils.mod(endPosition.y - startPosition.y, length);
        endPosition.z -= MathUtils.mod(endPosition.z - startPosition.z, length);
    }

    public static CapsulePlaceable load(Saver<?> saver) {
        CapsulePlaceable placeable = new CapsulePlaceable(saver.loadByte());
        placeable.radius.setValue(saver.loadInt());
        return placeable;
    }

    public void setStartEndPositions(Vector3l startPosition, Vector3l endPosition) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }


    @Override
    public void save(Saver<?> saver) {
        saver.saveByte((byte) 20);
        saver.saveByte(material);
        saver.saveInt(radius.value());
    }

    @Override
    protected void fillBitMap(long[] bitMap, int size, boolean forceSize) {
        if (positionsInvalid()) return;

        int radius = this.radius.value();
        Vector3l minPosition = Utils.min(startPosition, endPosition).sub(radius, radius, radius);
        fillBitMap(bitMap, minPosition, getPreferredSizePowOf2(), minPosition.x, minPosition.y, minPosition.z);
    }

    @Override
    public Structure getSmallStructure() {
        return new Structure(4, material, CAPSULE_ICON_BITMAP);
    }

    @Override
    public void place(Vector3l position, int lod) {
        if (positionsInvalid()) return;

        int radius = this.radius.value();
        Vector3l minPosition = Utils.min(startPosition, endPosition).sub(radius, radius, radius);
        Vector3l maxPosition = Utils.max(startPosition, endPosition).add(radius, radius, radius);

        long chunkStartX = minPosition.x >>> CHUNK_SIZE_BITS + lod;
        long chunkStartY = minPosition.y >>> CHUNK_SIZE_BITS + lod;
        long chunkStartZ = minPosition.z >>> CHUNK_SIZE_BITS + lod;
        long chunkEndX = Utils.getWrappedChunkCoordinate(maxPosition.x >>> CHUNK_SIZE_BITS + lod, chunkStartX, lod);
        long chunkEndY = Utils.getWrappedChunkCoordinate(maxPosition.y >>> CHUNK_SIZE_BITS + lod, chunkStartY, lod);
        long chunkEndZ = Utils.getWrappedChunkCoordinate(maxPosition.z >>> CHUNK_SIZE_BITS + lod, chunkStartZ, lod);
        ChunkSaver saver = new ChunkSaver();

        for (long chunkX = chunkStartX; chunkX <= chunkEndX; chunkX++)
            for (long chunkY = chunkStartY; chunkY <= chunkEndY; chunkY++)
                for (long chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ++)
                    placeInChunk(saver.loadAndGenerate(chunkX, chunkY, chunkZ, lod));
    }

    @Override
    public ArrayList<Chunk> getAffectedChunks() {
        return affectedChunks;
    }

    @Override
    public Structure getStructure() {
        if (startPosition == null || endPosition == null) return getSmallStructure();
        if (positionsInvalid()) return new Structure(0, AIR);

        int size = getPreferredSizePowOf2();
        long[] bitMap = new long[Math.max(size * size * size >> 6, 1)];
        fillBitMap(bitMap, 0, false);
        return new Structure(Integer.numberOfTrailingZeros(size), material, bitMap);
    }

    @Override
    public boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max) {
        if (positionsInvalid()) return true;

        for (long x = min.x; x != max.x + 1; x++)
            for (long y = min.y; y != max.y + 1; y++)
                for (long z = min.z; z != max.z + 1; z++)
                    if (!isOutside(x, y, z)) return true;
        return false;
    }

    @Override
    public void offsetPosition(Vector3l position, int targetedSide) {
        if (positionsInvalid()) return;
        offsetPositions(startPosition, endPosition);
    }

    @Override
    @MainThread
    public void spawnParticles(Vector3l position) {
        if (positionsInvalid()) return;

        updateBitMap(false);
        Vector3l minPosition = Utils.min(startPosition, endPosition).sub(radius.value(), radius.value(), radius.value());
        Game.getPlayer().getParticleCollector().addBreakPlaceParticleEffect(minPosition.x, minPosition.y, minPosition.z, 1, 1, 1, this);
    }

    @Override
    @MainThread
    public void playSounds(Vector3l position) {
        if (positionsInvalid()) return;

        updateBitMap(false);
        Vector3l minPosition = Utils.min(startPosition, endPosition).sub(radius.value(), radius.value(), radius.value());
        PlaceBreakSound.playBreakSounds(minPosition.x, minPosition.y, minPosition.z, 1, 1, 1, this);
        PlaceBreakSound.playPlaceSounds(minPosition.x, minPosition.y, minPosition.z, 1, 1, 1, this);
    }

    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        CapsulePlaceable copy = new CapsulePlaceable(material);
        copy.radius.setValue(radius.value());
        return copy;
    }

    @Override
    protected ShapeSetting[] getSettings() {
        return new ShapeSetting[]{
                new ShapeSetting(radius, UiMessages.RADIUS, null)
        };
    }

    @Override
    protected void loadSettings() {
        ShapeSetting[] baseSettings = getSettings();
        settings = new ShapeSetting[baseSettings.length];
        System.arraycopy(baseSettings, 0, settings, 0, baseSettings.length);
    }

    @Override
    public boolean allowBreak() {
        return Game.getPlayer().getInteractionHandler().getStartTarget() != null;
    }

    @Override
    public boolean allowPlace() {
        return Game.getPlayer().getInteractionHandler().getStartTarget() != null;
    }

    @Override
    public boolean offsetOnBreak() {
        return true;
    }

    @Override
    public int unrotatedLengthX() {
        if (positionsInvalid()) return 16;
        return (int) Math.abs(startPosition.x - endPosition.x) + 2 * radius.value();
    }

    @Override
    public int unrotatedLengthY() {
        if (positionsInvalid()) return 16;
        return (int) Math.abs(startPosition.y - endPosition.y) + 2 * radius.value();
    }

    @Override
    public int unrotatedLengthZ() {
        if (positionsInvalid()) return 16;
        return (int) Math.abs(startPosition.z - endPosition.z) + 2 * radius.value();
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, radius.value(), startPosition, endPosition);
    }

    public int getRadius() {
        return radius.value();
    }


    private boolean fastMissesAABB(long minX, long minY, long minZ, long maxX, long maxY, long maxZ) {
        int radius = this.radius.value();
        AABBi aabb = new AABBi(-radius, -radius, -radius, (int) (maxX - minX) + radius, (int) (maxY - minY) + radius, (int) (maxZ - minZ) + radius);

        return aabb.intersectLineSegment(
                startPosition.x - minX, startPosition.y - minY, startPosition.z - minZ,
                endPosition.x - minX, endPosition.y - minY, endPosition.z - minZ, new Vector2f()) == Intersectionf.OUTSIDE;
    }

    private void placeInChunk(Chunk chunk) {
        int bits = CHUNK_SIZE_BITS + chunk.LOD;
        Vector3l min = new Vector3l(chunk.X << bits, chunk.Y << bits, chunk.Z << bits);
        Vector3l max = new Vector3l(chunk.X + 1 << bits, chunk.Y + 1 << bits, chunk.Z + 1 << bits);
        if (fastMissesAABB(min.x, min.y, min.z, max.x, max.y, max.z)) return;

        byte[] uncompressedMaterials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];
        chunk.getMaterials().fillUncompressedMaterialsInto(uncompressedMaterials);
        boolean hasUpdated = false;

        int stepSize = 8 << chunk.LOD;
        for (long totalX = min.x; totalX != max.x; totalX += stepSize)
            for (long totalY = min.y; totalY != max.y; totalY += stepSize)
                for (long totalZ = min.z; totalZ != max.z; totalZ += stepSize)
                    hasUpdated = hasUpdated | placeInSection(uncompressedMaterials, chunk.LOD, totalX, totalY, totalZ);

        if (hasUpdated) {
            chunk.getMaterials().compressIntoData(uncompressedMaterials);
            chunk.setModified();
            affectedChunks.add(chunk);
        }
    }

    private boolean placeInSection(byte[] uncompressedMaterials, int lod, long totalX, long totalY, long totalZ) {
        int sectionSize = 8 << lod;
        if (fastMissesAABB(totalX, totalY, totalZ, totalX + sectionSize, totalY + sectionSize, totalZ + sectionSize)) return false;

        boolean paint = OptionSettings.PLACE_MODE.value() == PlaceMode.PAINT;
        boolean replaceAir = OptionSettings.PLACE_MODE.value() == PlaceMode.REPLACE_AIR;
        boolean breakHeldOnly = OptionSettings.PLACE_MODE.value() == PlaceMode.BREAK_HELD_ONLY;
        byte heldMaterial = breakHeldOnly ? ((ShapePlaceable) Game.getPlayer().getHeldPlaceable()).getMaterial() : AIR;

        int startX = (int) totalX >> lod & CHUNK_SIZE_MASK;
        int startY = (int) totalY >> lod & CHUNK_SIZE_MASK;
        int startZ = (int) totalZ >> lod & CHUNK_SIZE_MASK;

        for (int inChunkX = startX; inChunkX < startX + 8; inChunkX++)
            for (int inChunkY = startY; inChunkY < startY + 8; inChunkY++)
                for (int inChunkZ = startZ; inChunkZ < startZ + 8; inChunkZ++) {

                    long relativeX = totalX + ((long) inChunkX - startX << lod);
                    long relativeY = totalY + ((long) inChunkY - startY << lod);
                    long relativeZ = totalZ + ((long) inChunkZ - startZ << lod);

                    if (isOutside(relativeX, relativeY, relativeZ)) continue;
                    int materialIndex = MaterialsData.getUncompressedIndex(inChunkX, inChunkY, inChunkZ);

                    if (paint && uncompressedMaterials[materialIndex] == AIR
                            || replaceAir && uncompressedMaterials[materialIndex] != AIR
                            || breakHeldOnly && uncompressedMaterials[materialIndex] != heldMaterial) continue;
                    uncompressedMaterials[materialIndex] = material;
                }
        return true;
    }

    private boolean positionsInvalid() {
        int maxDistance = 256 - radius.value() * 2;
        return startPosition == null || endPosition == null
                || Math.abs(startPosition.x - endPosition.x) > maxDistance
                || Math.abs(startPosition.y - endPosition.y) > maxDistance
                || Math.abs(startPosition.z - endPosition.z) > maxDistance;
    }

    private void fillBitMap(long[] bitMap, Vector3l minPosition, int length, long totalX, long totalY, long totalZ) {
        if (fastMissesAABB(totalX, totalY, totalZ, totalX + length, totalY + length, totalZ + length)) return;

        if (length <= 8) {
            int size = length == 8 ? 8 : 4;
            for (long x = totalX; x != totalX + size; x++)
                for (long y = totalY; y != totalY + size; y++)
                    for (long z = totalZ; z != totalZ + size; z++) {
                        if (isOutside(x, y, z)) continue;
                        int index = MaterialsData.getUncompressedIndex((int) (x - minPosition.x), (int) (y - minPosition.y), (int) (z - minPosition.z));
                        bitMap[index >> 6] |= 1L << index;
                    }
            return;
        }

        length >>= 1;
        fillBitMap(bitMap, minPosition, length, totalX, totalY, totalZ);
        fillBitMap(bitMap, minPosition, length, totalX, totalY, totalZ + length);
        fillBitMap(bitMap, minPosition, length, totalX, totalY + length, totalZ);
        fillBitMap(bitMap, minPosition, length, totalX, totalY + length, totalZ + length);
        fillBitMap(bitMap, minPosition, length, totalX + length, totalY, totalZ);
        fillBitMap(bitMap, minPosition, length, totalX + length, totalY, totalZ + length);
        fillBitMap(bitMap, minPosition, length, totalX + length, totalY + length, totalZ);
        fillBitMap(bitMap, minPosition, length, totalX + length, totalY + length, totalZ + length);
    }

    private boolean isOutside(long x, long y, long z) {
        long paX = x - startPosition.x, paY = y - startPosition.y, paZ = z - startPosition.z;
        long baX = endPosition.x - startPosition.x, baY = endPosition.y - startPosition.y, baZ = endPosition.z - startPosition.z;
        if (baX == 0 && baY == 0 && baZ == 0) return lengthSquared(paX, paY, paZ) > radius.value() * radius.value();

        double h = Math.clamp(dot(paX, paY, paZ, baX, baY, baZ) / dot(baX, baY, baZ, baX, baY, baZ), 0.0, 1.0);
        return lengthSquared(paX - baX * h, paY - baY * h, paZ - baZ * h) >= radius.value() * radius.value();
    }


    private static double dot(long aX, long aY, long aZ, long bX, long bY, long bZ) {
        return aX * bX + aY * bY + aZ * bZ;
    }

    private static double lengthSquared(double x, double y, double z) {
        return x * x + y * y + z * z;
    }


    private final byte material;
    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
    private final StandAloneIntSetting radius = new StandAloneIntSetting(1, 64, 8);

    private Vector3l startPosition, endPosition;

    private static final long[] CAPSULE_ICON_BITMAP = new long[]{
            0x0L, 0x0L, 0x0L, 0x0L, 0x0L, 0x0L, 0x0L, 0xFEA0C00000000000L, 0x0L, 0x0L, 0x0L, 0x0L, 0x0L, 0x0L, 0xDCFF004040C00000L, 0x0L, 0x0L, 0x0L, 0x0L, 0x0L,
            0x0L, 0xBA00FF202000A000L, 0x0L, 0x0L, 0xFEA0C00000000000L, 0xDCFF004040C00000L, 0xBA00FF202000A000L, 0xFFFFFFFF70F0F0F8L, 0xFFFFFFFFFFFFFFFEL,
            0x4FF005D5DFF00DCL, 0x200FF3B3B00FFBAL, 0x3057F7FFFFFFFL, 0xFEA0C00000000000L, 0xDCFF004040C00000L, 0xBA00FF202000A000L, 0xFFFFFFFFFFFFFFFEL,
            0xFFAACC08FFEAEC88L, 0x4FF004C5DFF00CCL, 0x200FF2A3B00FFAAL, 0x3057F7FFFFFFFL, 0x0L, 0x0L, 0x4FF005D5DFF00DCL, 0x0L, 0x0L, 0x0L, 0x50004L, 0x0L,
            0x0L, 0x200FF3B3B00FFBAL, 0x0L, 0x0L, 0x0L, 0x302L, 0x0L, 0x0L, 0x3057F7FFFFFFFL, 0x50004L, 0x302L, 0x0L, 0x0L, 0x0L, 0x0L, 0x0L};
}
