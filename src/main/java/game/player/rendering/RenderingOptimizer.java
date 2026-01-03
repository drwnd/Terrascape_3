package game.player.rendering;

import core.assets.AssetManager;
import core.rendering_api.Window;
import core.rendering_api.shaders.Shader;
import core.settings.ToggleSetting;
import core.utils.IntArrayList;

import game.assets.Shaders;
import game.player.Player;
import game.server.Game;
import game.utils.Position;
import game.utils.Transformation;
import game.utils.Utils;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3i;

import java.util.Arrays;

import static game.utils.Constants.*;
import static org.lwjgl.opengl.GL46.*;

public final class RenderingOptimizer {

    public static final int INDIRECT_COMMAND_SIZE = 16;

    public RenderingOptimizer() {
        opaqueIndirectBuffer = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, opaqueIndirectBuffer);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, (long) LOD_COUNT * CHUNKS_PER_LOD * INDIRECT_COMMAND_SIZE * 6, GL_DYNAMIC_DRAW);

        waterIndirectBuffer = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, waterIndirectBuffer);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, (long) LOD_COUNT * CHUNKS_PER_LOD * INDIRECT_COMMAND_SIZE, GL_DYNAMIC_DRAW);

        glassIndirectBuffer = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, glassIndirectBuffer);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, (long) LOD_COUNT * CHUNKS_PER_LOD * INDIRECT_COMMAND_SIZE, GL_DYNAMIC_DRAW);

        occluderBuffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, occluderBuffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, (long) LOD_COUNT * CHUNKS_PER_LOD * AABB_INT_SIZE * 4, GL_DYNAMIC_DRAW);

        occludeeBuffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, occludeeBuffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, (long) LOD_COUNT * CHUNKS_PER_LOD * AABB_INT_SIZE * 4, GL_DYNAMIC_DRAW);

        depthTexture = ObjectLoader.createTexture2D(GL_DEPTH_COMPONENT32F, width, height, GL_DEPTH_COMPONENT, GL_FLOAT, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[]{0, 0, 0, 0});

        framebuffer = glCreateFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0);
        glDrawBuffers(GL_NONE);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Frame buffer not complete. status " + Integer.toHexString(glCheckFramebufferStatus(GL_FRAMEBUFFER)));
    }

    public void computeVisibility(Player player, Position cameraPosition, Matrix4f projectionViewMatrix) {
        FrustumIntersection frustumIntersection = new FrustumIntersection(Transformation.getFrustumCullingMatrix(player.getCamera()));

        meshCollector = player.getMeshCollector();
        Vector3i position = cameraPosition.intPosition();

        cameraChunkX = position.x >> CHUNK_SIZE_BITS;
        cameraChunkY = position.y >> CHUNK_SIZE_BITS;
        cameraChunkZ = position.z >> CHUNK_SIZE_BITS;
        cameraX = position.x;
        cameraY = position.y;
        cameraZ = position.z;

        for (int lod = 0; lod < LOD_COUNT; lod++) computeLodVisibility(lod, frustumIntersection, visibilityBits);
        for (int lod = LOD_COUNT - 1; lod >= 0; lod--) removeLodVisibilityOverlap(lod);

        opaqueCommands.clear();
        waterCommands.clear();
        glassCommands.clear();
        if (ToggleSetting.USE_OCCLUSION_CULLING.value())
            generateIndirectCommandsWithOcclusionCulling(cameraPosition, projectionViewMatrix);
        else generateIndirectCommandsWithoutOcclusionCulling();
    }

    public long getOpaqueLodStart(int lod) {
        return lodStarts[lod * 3];
    }

    public int getOpaqueLodDrawCount(int lod) {
        return lodDrawCounts[lod * 3];
    }

    public long getWaterLodStart(int lod) {
        return lodStarts[lod * 3 + 1];
    }

    public int getWaterLodDrawCount(int lod) {
        return lodDrawCounts[lod * 3 + 1];
    }

    public long getGlassLodStart(int lod) {
        return lodStarts[lod * 3 + 2];
    }

    public int getGlassLodDrawCount(int lod) {
        return lodDrawCounts[lod * 3 + 2];
    }

    public int getOpaqueIndirectBuffer() {
        return opaqueIndirectBuffer;
    }

    public int getWaterIndirectBuffer() {
        return waterIndirectBuffer;
    }

    public int getGlassIndirectBuffer() {
        return glassIndirectBuffer;
    }

    public int getDepthTexture() {
        return depthTexture;
    }

    public void cleanUp() {
        glDeleteBuffers(opaqueIndirectBuffer);
        glDeleteBuffers(waterIndirectBuffer);
        glDeleteBuffers(glassIndirectBuffer);
        glDeleteBuffers(occluderBuffer);
        glDeleteBuffers(occludeeBuffer);
        glDeleteTextures(depthTexture);
        glDeleteFramebuffers(framebuffer);
    }


    private void generateIndirectCommandsWithOcclusionCulling(Position cameraPosition, Matrix4f projectionViewMatrix) {
        aabbs.clear();
        for (int lod = 0; lod < LOD_COUNT; lod++) generateIndirectCommandsWithOcclusionCulling(lod);
        int occludeeCount = aabbs.size() / AABB_INT_SIZE;

        glNamedBufferSubData(opaqueIndirectBuffer, 0, opaqueCommands.toArray());
        glNamedBufferSubData(waterIndirectBuffer, 0, waterCommands.toArray());
        glNamedBufferSubData(glassIndirectBuffer, 0, glassCommands.toArray());
        glNamedBufferSubData(occludeeBuffer, 0, aabbs.toArray());

        aabbs.clear();
        for (int lod = 0; lod < LOD_COUNT; lod++) populateOccluderBuffer(lod);
        glNamedBufferSubData(occluderBuffer, 0, aabbs.toArray());
        int occluderCount = aabbs.size() / AABB_INT_SIZE;

        renderOccluders(cameraPosition, projectionViewMatrix, occluderCount);
        renderOccludees(cameraPosition, projectionViewMatrix, occludeeCount);
    }

    private void generateIndirectCommandsWithoutOcclusionCulling() {
        for (int lod = 0; lod < LOD_COUNT; lod++) generateIndirectCommandsWithoutOcclusionCulling(lod);

        glNamedBufferSubData(opaqueIndirectBuffer, 0, opaqueCommands.toArray());
        glNamedBufferSubData(waterIndirectBuffer, 0, waterCommands.toArray());
        glNamedBufferSubData(glassIndirectBuffer, 0, glassCommands.toArray());
    }

    private void computeLodVisibility(int lod, FrustumIntersection frustumIntersection, long[][] visibilityBits) {
        int chunkX = cameraChunkX >> lod;
        int chunkY = cameraChunkY >> lod;
        int chunkZ = cameraChunkZ >> lod;

        lodVisibilityBits = visibilityBits[lod];
        Arrays.fill(lodVisibilityBits, 0L);

        int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);
        lodVisibilityBits[chunkIndex >> 6] |= 1L << chunkIndex;

        fillVisibleChunks(chunkX, chunkY, chunkZ + 1, (byte) (1 << NORTH), lod, frustumIntersection);
        fillVisibleChunks(chunkX, chunkY, chunkZ - 1, (byte) (1 << SOUTH), lod, frustumIntersection);

        fillVisibleChunks(chunkX, chunkY + 1, chunkZ, (byte) (1 << TOP), lod, frustumIntersection);
        fillVisibleChunks(chunkX, chunkY - 1, chunkZ, (byte) (1 << BOTTOM), lod, frustumIntersection);

        fillVisibleChunks(chunkX + 1, chunkY, chunkZ, (byte) (1 << WEST), lod, frustumIntersection);
        fillVisibleChunks(chunkX - 1, chunkY, chunkZ, (byte) (1 << EAST), lod, frustumIntersection);
    }

    private void fillVisibleChunks(int chunkX, int chunkY, int chunkZ, byte traveledDirections, int lod, FrustumIntersection intersection) {
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;

        int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);
        if ((lodVisibilityBits[chunkIndex >> 6] & 1L << chunkIndex) != 0) return;

        if (Game.getWorld().getChunk(chunkIndex, lod) == null) return;
        if (!intersection.testAab(
                (chunkX << chunkSizeBits) - cameraX,
                (chunkY << chunkSizeBits) - cameraY,
                (chunkZ << chunkSizeBits) - cameraZ,
                (chunkX + 1 << chunkSizeBits) - cameraX,
                (chunkY + 1 << chunkSizeBits) - cameraY,
                (chunkZ + 1 << chunkSizeBits) - cameraZ)) return;

        lodVisibilityBits[chunkIndex >> 6] |= 1L << chunkIndex;

        if ((traveledDirections & 1 << SOUTH) == 0)
            fillVisibleChunks(chunkX, chunkY, chunkZ + 1, (byte) (traveledDirections | 1 << NORTH), lod, intersection);
        if ((traveledDirections & 1 << NORTH) == 0)
            fillVisibleChunks(chunkX, chunkY, chunkZ - 1, (byte) (traveledDirections | 1 << SOUTH), lod, intersection);

        if ((traveledDirections & 1 << BOTTOM) == 0)
            fillVisibleChunks(chunkX, chunkY + 1, chunkZ, (byte) (traveledDirections | 1 << TOP), lod, intersection);
        if ((traveledDirections & 1 << TOP) == 0)
            fillVisibleChunks(chunkX, chunkY - 1, chunkZ, (byte) (traveledDirections | 1 << BOTTOM), lod, intersection);

        if ((traveledDirections & 1 << EAST) == 0)
            fillVisibleChunks(chunkX + 1, chunkY, chunkZ, (byte) (traveledDirections | 1 << WEST), lod, intersection);
        if ((traveledDirections & 1 << WEST) == 0)
            fillVisibleChunks(chunkX - 1, chunkY, chunkZ, (byte) (traveledDirections | 1 << EAST), lod, intersection);
    }

    private void removeLodVisibilityOverlap(int lod) {
        lodVisibilityBits = visibilityBits[lod];
        computeStartEnd(lod);

        for (int lodModelX = startX; lodModelX <= endX; lodModelX++)
            for (int lodModelZ = startZ; lodModelZ <= endZ; lodModelZ++)
                for (int lodModelY = startY; lodModelY <= endY; lodModelY++)
                    removeModelVisibilityOverlap(lod, lodModelX, lodModelY, lodModelZ);
    }

    private void removeModelVisibilityOverlap(int lod, int lodModelX, int lodModelY, int lodModelZ) {
        int index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
        if ((lodVisibilityBits[index >> 6] & 1L << index) == 0) return;

        OpaqueModel opaqueModel = meshCollector.getOpaqueModel(index, lod);
        TransparentModel transparentModel = meshCollector.getTransparentModel(index, lod);
        if (opaqueModel == null || transparentModel == null) {
            lodVisibilityBits[index >> 6] &= ~(1L << index);
            return;
        }

        if (lod == 0 || modelFarEnoughAway(lodModelX, lodModelY, lodModelZ, lod)) return;

        int nextLodX = lodModelX << 1;
        int nextLodY = lodModelY << 1;
        int nextLodZ = lodModelZ << 1;
        if (modelCubePresent(nextLodX, nextLodY, nextLodZ, lod - 1)) lodVisibilityBits[index >> 6] &= ~(1L << index);
        else clearModelCubeVisibility(nextLodX, nextLodY, nextLodZ, lod - 1);
    }

    private boolean modelFarEnoughAway(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        int distanceX = Math.abs((cameraChunkX >> lod) - lodModelX);
        int distanceY = Math.abs((cameraChunkY >> lod) - lodModelY);
        int distanceZ = Math.abs((cameraChunkZ >> lod) - lodModelZ);

        return distanceX > (RENDER_DISTANCE_XZ >> 1) + 1 || distanceZ > (RENDER_DISTANCE_XZ >> 1) + 1 || distanceY > (RENDER_DISTANCE_Y >> 1) + 1;
    }

    private boolean modelCubePresent(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        return meshCollector.isModelPresent(lodModelX, lodModelY, lodModelZ, lod)
                && meshCollector.isModelPresent(lodModelX, lodModelY, lodModelZ + 1, lod)
                && meshCollector.isModelPresent(lodModelX, lodModelY + 1, lodModelZ, lod)
                && meshCollector.isModelPresent(lodModelX, lodModelY + 1, lodModelZ + 1, lod)
                && meshCollector.isModelPresent(lodModelX + 1, lodModelY, lodModelZ, lod)
                && meshCollector.isModelPresent(lodModelX + 1, lodModelY, lodModelZ + 1, lod)
                && meshCollector.isModelPresent(lodModelX + 1, lodModelY + 1, lodModelZ, lod)
                && meshCollector.isModelPresent(lodModelX + 1, lodModelY + 1, lodModelZ + 1, lod);
    }

    private void clearModelCubeVisibility(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        long[] visibilityBits = this.visibilityBits[lod];
        int index;
        index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
        visibilityBits[index >> 6] &= ~(3L << index);
        index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ + 1, lod);
        visibilityBits[index >> 6] &= ~(3L << index);
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY, lodModelZ, lod);
        visibilityBits[index >> 6] &= ~(3L << index);
        index = Utils.getChunkIndex(lodModelX + 1, lodModelY, lodModelZ + 1, lod);
        visibilityBits[index >> 6] &= ~(3L << index);
    }

    private void generateIndirectCommandsWithOcclusionCulling(int lod) {
        int drawCount = 0;
        lodStarts[lod * 3 + 0] = (long) opaqueCommands.size() / 4 * INDIRECT_COMMAND_SIZE;
        lodStarts[lod * 3 + 1] = (long) waterCommands.size() / 4 * INDIRECT_COMMAND_SIZE;
        lodStarts[lod * 3 + 2] = (long) glassCommands.size() / 4 * INDIRECT_COMMAND_SIZE;

        lodVisibilityBits = visibilityBits[lod];
        computeStartEnd(lod);
        int lodCameraChunkX = cameraChunkX >> lod;
        int lodCameraChunkY = cameraChunkY >> lod;
        int lodCameraChunkZ = cameraChunkZ >> lod;

        for (int lodModelX = startX; lodModelX <= endX; lodModelX++)
            for (int lodModelZ = startZ; lodModelZ <= endZ; lodModelZ++)
                for (int lodModelY = startY; lodModelY <= endY; lodModelY++) {

                    int index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
                    if ((lodVisibilityBits[index >> 6] & 1L << index) == 0) continue;

                    OpaqueModel opaqueModel = meshCollector.getOpaqueModel(index, lod);
                    TransparentModel transparentModel = meshCollector.getTransparentModel(index, lod);
                    AABB occludee = meshCollector.getOccludee(index, lod);
                    if (opaqueModel == null || transparentModel == null) continue;
                    if (opaqueModel.isEmpty() && transparentModel.isEmpty()) continue;

                    drawCount++;
                    opaqueModel.addDataWithOcclusionCulling(opaqueCommands, lodCameraChunkX, lodCameraChunkY, lodCameraChunkZ);
                    transparentModel.addDataWithOcclusionCulling(waterCommands, glassCommands);
                    occludee.addData(aabbs, lodModelX, lodModelY, lodModelZ, lod);
                }
        lodDrawCounts[lod * 3 + 0] = drawCount * 6;
        lodDrawCounts[lod * 3 + 1] = drawCount;
        lodDrawCounts[lod * 3 + 2] = drawCount;
    }

    private void generateIndirectCommandsWithoutOcclusionCulling(int lod) {
        int oldOpaqueDrawCount = opaqueCommands.size() / 4;
        int oldWaterDrawCount = waterCommands.size() / 4;
        int oldGlassDrawCount = glassCommands.size() / 4;
        lodStarts[lod * 3 + 0] = (long) opaqueCommands.size() / 4 * INDIRECT_COMMAND_SIZE;
        lodStarts[lod * 3 + 1] = (long) waterCommands.size() / 4 * INDIRECT_COMMAND_SIZE;
        lodStarts[lod * 3 + 2] = (long) glassCommands.size() / 4 * INDIRECT_COMMAND_SIZE;

        lodVisibilityBits = visibilityBits[lod];
        computeStartEnd(lod);
        int lodCameraChunkX = cameraChunkX >> lod;
        int lodCameraChunkY = cameraChunkY >> lod;
        int lodCameraChunkZ = cameraChunkZ >> lod;

        for (int lodModelX = startX; lodModelX <= endX; lodModelX++)
            for (int lodModelZ = startZ; lodModelZ <= endZ; lodModelZ++)
                for (int lodModelY = startY; lodModelY <= endY; lodModelY++) {
                    int index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
                    if ((lodVisibilityBits[index >> 6] & 1L << index) == 0) continue;

                    OpaqueModel opaqueModel = meshCollector.getOpaqueModel(index, lod);
                    TransparentModel transparentModel = meshCollector.getTransparentModel(index, lod);
                    if (opaqueModel == null || transparentModel == null) continue;

                    opaqueModel.addDataWithoutOcclusionCulling(opaqueCommands, lodCameraChunkX, lodCameraChunkY, lodCameraChunkZ);
                    transparentModel.addDataWithoutOcclusionCulling(waterCommands, glassCommands);
                }

        lodDrawCounts[lod * 3 + 0] = opaqueCommands.size() / 4 - oldOpaqueDrawCount;
        lodDrawCounts[lod * 3 + 1] = waterCommands.size() / 4 - oldWaterDrawCount;
        lodDrawCounts[lod * 3 + 2] = glassCommands.size() / 4 - oldGlassDrawCount;
    }

    private void populateOccluderBuffer(int lod) {
        lodVisibilityBits = visibilityBits[lod];
        computeStartEnd(lod);

        for (int lodModelX = startX; lodModelX <= endX; lodModelX++)
            for (int lodModelZ = startZ; lodModelZ <= endZ; lodModelZ++)
                for (int lodModelY = startY; lodModelY <= endY; lodModelY++) {
                    int index = Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod);
                    if ((lodVisibilityBits[index >> 6] & 1L << index) == 0) continue;
                    if (!meshCollector.neighborHasModel(lodModelX, lodModelY, lodModelZ, lod)) continue;

                    AABB occluder = meshCollector.getOccluder(index, lod);
                    if (occluder != null) occluder.addData(aabbs, lodModelX, lodModelY, lodModelZ, lod);
                }
    }

    private void renderOccluders(Position cameraPosition, Matrix4f projectionViewMatrix, int occluderCount) {
        Shader shader = AssetManager.get(Shaders.AABB);
        shader.bind();
        shader.setUniform("projectionViewMatrix", projectionViewMatrix);
        shader.setUniform("iCameraPosition",
                cameraPosition.intX & ~CHUNK_SIZE_MASK,
                cameraPosition.intY & ~CHUNK_SIZE_MASK,
                cameraPosition.intZ & ~CHUNK_SIZE_MASK);

        glViewport(0, 0, width, height);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);
        glDisable(GL_STENCIL_TEST);
        glDisable(GL_BLEND);
        glDepthFunc(GL_LESS);
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        glDepthMask(true);
        glColorMask(false, false, false, false);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, occluderBuffer);
        glDrawArraysInstanced(GL_TRIANGLES, 0, 36, occluderCount);
    }

    private void renderOccludees(Position cameraPosition, Matrix4f projectionViewMatrix, int occludeeCount) {
        Shader shader = AssetManager.get(Shaders.OCCLUSION_CULLING);
        shader.bind();
        shader.setUniform("projectionViewMatrix", projectionViewMatrix);
        shader.setUniform("iCameraPosition",
                cameraPosition.intX & ~CHUNK_SIZE_MASK,
                cameraPosition.intY & ~CHUNK_SIZE_MASK,
                cameraPosition.intZ & ~CHUNK_SIZE_MASK);

        glDepthFunc(GL_LEQUAL);
        glDepthMask(false);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, occludeeBuffer);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, opaqueIndirectBuffer);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, waterIndirectBuffer);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, glassIndirectBuffer);

        glDrawArraysInstanced(GL_TRIANGLES, 0, 36, occludeeCount);

        glDepthMask(true);
        glColorMask(true, true, true, true);
        glViewport(0, 0, Window.getWidth(), Window.getHeight());
        glDisable(GL_BLEND);
        glCullFace(GL_BACK);
        glDepthFunc(GL_LESS);
    }

    private void computeStartEnd(int lod) {
        int lodCameraChunkX = cameraChunkX >> lod;
        int lodCameraChunkY = cameraChunkY >> lod;
        int lodCameraChunkZ = cameraChunkZ >> lod;

        startX = Utils.makeEven(lodCameraChunkX - RENDER_DISTANCE_XZ - 2);
        startY = Utils.makeEven(lodCameraChunkY - RENDER_DISTANCE_Y - 2);
        startZ = Utils.makeEven(lodCameraChunkZ - RENDER_DISTANCE_XZ - 2);

        endX = Utils.makeOdd(lodCameraChunkX + RENDER_DISTANCE_XZ + 2);
        endY = Utils.makeOdd(lodCameraChunkY + RENDER_DISTANCE_Y + 2);
        endZ = Utils.makeOdd(lodCameraChunkZ + RENDER_DISTANCE_XZ + 2);
    }


    private long[] lodVisibilityBits;
    private MeshCollector meshCollector;
    private int cameraChunkX, cameraChunkY, cameraChunkZ;
    private int cameraX, cameraY, cameraZ;
    private int startX, endX, startY, endY, startZ, endZ;

    private final int opaqueIndirectBuffer, waterIndirectBuffer, glassIndirectBuffer;
    private final int occluderBuffer, occludeeBuffer;
    private final int framebuffer, depthTexture;

    private final long[][] visibilityBits = new long[LOD_COUNT][CHUNKS_PER_LOD / 64];
    private final long[] lodStarts = new long[LOD_COUNT * 3];
    private final int[] lodDrawCounts = new int[LOD_COUNT * 3];

    private final IntArrayList opaqueCommands = new IntArrayList(INDIRECT_COMMAND_SIZE * 256);
    private final IntArrayList waterCommands = new IntArrayList(INDIRECT_COMMAND_SIZE * 128);
    private final IntArrayList glassCommands = new IntArrayList(INDIRECT_COMMAND_SIZE * 128);
    private final IntArrayList aabbs = new IntArrayList(AABB_INT_SIZE * 256);

    private static final int AABB_INT_SIZE = 4;
    private static final int width = 400, height = 225;
}
