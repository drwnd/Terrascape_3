package game.player.interaction;

import core.assets.AssetManager;
import core.utils.MathUtils;
import core.utils.Saver;
import core.utils.Vector3l;

import game.assets.StructureIdentifier;
import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.server.material.Properties;
import game.server.saving.ChunkSaver;
import game.settings.IntSettings;
import game.utils.Transformation;
import game.utils.Utils;

import org.joml.Matrix4f;
import org.joml.Vector3i;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class StructurePlaceable implements Placeable {

    /**
     * Initializes the structure placeable with a structure identifier.
     *
     * @param identifier the identifier of the structure
     */
    public StructurePlaceable(StructureIdentifier identifier) {
        this.identifier = identifier;
        this.structure = AssetManager.get(identifier);
    }

    /**
     * Saves the structure placeable's state to a saver.
     *
     * @param saver the saver to use
     */
    public void save(Saver<?> saver) {
        saver.saveByte((byte) 2);
        saver.saveString(identifier.structureName());
    }

    @Override
    public void rotateForwards() {
        rotation = (Rotation8Way) rotation.next();
    }

    @Override
    public void rotateBackwards() {
        rotation = (Rotation8Way) rotation.previous();
    }

    public static StructurePlaceable load(Saver<?> saver) {
        return new StructurePlaceable(new StructureIdentifier(saver.loadString()));
    }

    @Override
    public void place(Vector3l position, int lod) {
        byte transform = getTransform();
        long chunkStartX = position.x >>> CHUNK_SIZE_BITS + lod;
        long chunkStartY = position.y >>> CHUNK_SIZE_BITS + lod;
        long chunkStartZ = position.z >>> CHUNK_SIZE_BITS + lod;
        long chunkEndX = Utils.getWrappedChunkCoordinate(position.x + structure.sizeX(transform) >>> CHUNK_SIZE_BITS + lod, chunkStartX, lod);
        long chunkEndY = Utils.getWrappedChunkCoordinate(position.y + structure.sizeY(transform) >>> CHUNK_SIZE_BITS + lod, chunkStartY, lod);
        long chunkEndZ = Utils.getWrappedChunkCoordinate(position.z + structure.sizeZ(transform) >>> CHUNK_SIZE_BITS + lod, chunkStartZ, lod);
        ChunkSaver saver = new ChunkSaver();

        for (long chunkX = chunkStartX; chunkX <= chunkEndX; chunkX++)
            for (long chunkY = chunkStartY; chunkY <= chunkEndY; chunkY++)
                for (long chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ++)
                    placeInChunk(saver.loadAndGenerate(chunkX, chunkY, chunkZ, lod), position);
    }

    @Override
    public ArrayList<Chunk> getAffectedChunks() {
        return affectedChunks;
    }

    @Override
    public Structure getStructure() {
        return structure;
    }

    @Override
    public void offsetPosition(Vector3l position, int targetedSide) {
        byte transform = getTransform();
        position.x -= structure.sizeX(transform) >> 1;
        position.z -= structure.sizeZ(transform) >> 1;

        int breakPlaceAlign = IntSettings.BREAK_PLACE_ALIGN.value();
        int mask = -(1 << breakPlaceAlign);
        position.x &= mask;
        position.y &= mask;
        position.z &= mask;
    }

    @Override
    public boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max) {
        min.sub(position);
        max.sub(position);
        byte transform = getTransform();

        for (int structureX = (int) min.x; structureX < max.x; structureX++)
            for (int structureY = (int) min.y; structureY < max.y; structureY++)
                for (int structureZ = (int) min.z; structureZ < max.z; structureZ++) {
                    byte material = structure.getMaterial(structureX, structureY, structureZ, transform);
                    if (Properties.doesntHaveProperties(material, NO_COLLISION)) return true;
                }
        return false;
    }

    @Override
    public void spawnParticles(Vector3l position) {
        byte transform = getTransform();
        Vector3i lengths = new Vector3i(structure.sizeX(), structure.sizeY(), structure.sizeZ());
        Game.getPlayer().getParticleCollector().addPlaceParticleEffect(position.x, position.y, position.z, structure, lengths, transform);
    }

    @Override
    public int getPreferredSize() {
        return Math.max(structure.sizeX(), Math.max(structure.sizeY(), structure.sizeZ()));
    }

    @Override
    public boolean allowBreak() {
        return false;
    }

    public StructureIdentifier getIdentifier() {
        return identifier;
    }

    public Matrix4f getModelMatrix() {
        return Transformation.getModelMatrix(getTransform(), structure);
    }

    /**
     * Returns the side transformations for the structure based on its current rotation.
     *
     * @return an array of side indices
     */
    public int[] getSideTransform() {
        boolean rotate90 = (rotation.ordinal() & Structure.ROTATE_90) != 0;

        return new int[]{
                rotate90 ? WEST : NORTH,
                TOP,
                rotate90 ? SOUTH : WEST,
                rotate90 ? EAST : SOUTH,
                BOTTOM,
                rotate90 ? NORTH : EAST
        };
    }


    /**
     * Internal method to place the structure within a specific chunk.
     *
     * @param chunk    the chunk to place in
     * @param position the world block coordinates (LOD 0) of the structure's origin
     */
    private void placeInChunk(Chunk chunk, Vector3l position) {
        byte transform = getTransform();
        long chunkStartX = chunk.X << CHUNK_SIZE_BITS + chunk.LOD;
        long chunkStartY = chunk.Y << CHUNK_SIZE_BITS + chunk.LOD;
        long chunkStartZ = chunk.Z << CHUNK_SIZE_BITS + chunk.LOD;

        int inChunkX = (int) Utils.wrappedMax(chunkStartX, position.x) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkY = (int) Utils.wrappedMax(chunkStartY, position.y) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkZ = (int) Utils.wrappedMax(chunkStartZ, position.z) >> chunk.LOD & CHUNK_SIZE_MASK;

        int startX = (int) (chunkStartX + (inChunkX << chunk.LOD) - position.x);
        int startY = (int) (chunkStartY + (inChunkY << chunk.LOD) - position.y);
        int startZ = (int) (chunkStartZ + (inChunkZ << chunk.LOD) - position.z);

        int lengthX = MathUtils.min(structure.sizeX(transform) - startX, CHUNK_SIZE - inChunkX << chunk.LOD, structure.sizeX(transform));
        int lengthY = MathUtils.min(structure.sizeY(transform) - startY, CHUNK_SIZE - inChunkY << chunk.LOD, structure.sizeY(transform));
        int lengthZ = MathUtils.min(structure.sizeZ(transform) - startZ, CHUNK_SIZE - inChunkZ << chunk.LOD, structure.sizeZ(transform));
        if (lengthX <= 0 || lengthY <= 0 || lengthZ <= 0) return;

        chunk.storeStructureMaterials(
                inChunkX, inChunkY, inChunkZ,
                startX, startY, startZ,
                lengthX, lengthY, lengthZ,
                chunk.LOD, structure, transform);

        affectedChunks.add(chunk);
        World world = Game.getWorld();
        if (inChunkX == 0) affectedChunks.add(world.getChunk(chunk.X - 1, chunk.Y, chunk.Z, chunk.LOD));
        if (inChunkY == 0) affectedChunks.add(world.getChunk(chunk.X, chunk.Y - 1, chunk.Z, chunk.LOD));
        if (inChunkZ == 0) affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z - 1, chunk.LOD));
        if (inChunkX + lengthX == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X + 1, chunk.Y, chunk.Z, chunk.LOD));
        if (inChunkY + lengthY == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X, chunk.Y + 1, chunk.Z, chunk.LOD));
        if (inChunkZ + lengthZ == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z + 1, chunk.LOD));
    }

    private byte getTransform() {
        return (byte) rotation.ordinal();
    }

    private Rotation8Way rotation = Rotation8Way.ROTATION_1;
    private final StructureIdentifier identifier;
    private final Structure structure;
    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
}
