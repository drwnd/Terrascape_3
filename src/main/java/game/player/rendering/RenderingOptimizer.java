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
import org.lwjgl.opengl.GL46;

import java.util.Arrays;

import static game.utils.Constants.*;

public final class RenderingOptimizer {

    public static final int INDIRECT_COMMAND_SIZE = 16;

    public RenderingOptimizer() {
        opaqueIndirectBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_DRAW_INDIRECT_BUFFER, opaqueIndirectBuffer);
        GL46.glBufferData(GL46.GL_DRAW_INDIRECT_BUFFER, (long) LOD_COUNT * CHUNKS_PER_LOD * INDIRECT_COMMAND_SIZE * 6, GL46.GL_DYNAMIC_DRAW);

        waterIndirectBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_DRAW_INDIRECT_BUFFER, waterIndirectBuffer);
        GL46.glBufferData(GL46.GL_DRAW_INDIRECT_BUFFER, (long) LOD_COUNT * CHUNKS_PER_LOD * INDIRECT_COMMAND_SIZE, GL46.GL_DYNAMIC_DRAW);

        glassIndirectBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_DRAW_INDIRECT_BUFFER, glassIndirectBuffer);
        GL46.glBufferData(GL46.GL_DRAW_INDIRECT_BUFFER, (long) LOD_COUNT * CHUNKS_PER_LOD * INDIRECT_COMMAND_SIZE, GL46.GL_DYNAMIC_DRAW);

        occluderBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_SHADER_STORAGE_BUFFER, occluderBuffer);
        GL46.glBufferData(GL46.GL_SHADER_STORAGE_BUFFER, (long) LOD_COUNT * CHUNKS_PER_LOD * AABB_INT_SIZE * 4, GL46.GL_DYNAMIC_DRAW);

        occludeeBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_SHADER_STORAGE_BUFFER, occludeeBuffer);
        GL46.glBufferData(GL46.GL_SHADER_STORAGE_BUFFER, (long) LOD_COUNT * CHUNKS_PER_LOD * AABB_INT_SIZE * 4, GL46.GL_DYNAMIC_DRAW);

        depthTexture = ObjectLoader.createTexture2D(GL46.GL_DEPTH_COMPONENT32F, width, height, GL46.GL_DEPTH_COMPONENT, GL46.GL_FLOAT, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameterfv(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_BORDER_COLOR, new float[]{0, 0, 0, 0});

        framebuffer = GL46.glCreateFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, framebuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_DEPTH_ATTACHMENT, GL46.GL_TEXTURE_2D, depthTexture, 0);
        GL46.glDrawBuffers(GL46.GL_NONE);
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Frame buffer not complete. status " + Integer.toHexString(GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER)));
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
        GL46.glDeleteBuffers(opaqueIndirectBuffer);
        GL46.glDeleteBuffers(waterIndirectBuffer);
        GL46.glDeleteBuffers(glassIndirectBuffer);
        GL46.glDeleteBuffers(occluderBuffer);
        GL46.glDeleteBuffers(occludeeBuffer);
        GL46.glDeleteTextures(depthTexture);
        GL46.glDeleteFramebuffers(framebuffer);
    }


    private void generateIndirectCommandsWithOcclusionCulling(Position cameraPosition, Matrix4f projectionViewMatrix) {
        aabbs.clear();
        for (int lod = 0; lod < LOD_COUNT; lod++) generateIndirectCommandsWithOcclusionCulling(lod);
        int occludeeCount = aabbs.size() / AABB_INT_SIZE;

        GL46.glNamedBufferSubData(opaqueIndirectBuffer, 0, opaqueCommands.toArray());
        GL46.glNamedBufferSubData(waterIndirectBuffer, 0, waterCommands.toArray());
        GL46.glNamedBufferSubData(glassIndirectBuffer, 0, glassCommands.toArray());
        GL46.glNamedBufferSubData(occludeeBuffer, 0, aabbs.toArray());

        aabbs.clear();
        for (int lod = 0; lod < LOD_COUNT; lod++) populateOccluderBuffer(lod);
        GL46.glNamedBufferSubData(occluderBuffer, 0, aabbs.toArray());
        int occluderCount = aabbs.size() / AABB_INT_SIZE;

        renderOccluders(cameraPosition, projectionViewMatrix, occluderCount);
        renderOccludees(cameraPosition, projectionViewMatrix, occludeeCount);
    }

    private void generateIndirectCommandsWithoutOcclusionCulling() {
        for (int lod = 0; lod < LOD_COUNT; lod++) generateIndirectCommandsWithoutOcclusionCulling(lod);

        GL46.glNamedBufferSubData(opaqueIndirectBuffer, 0, opaqueCommands.toArray());
        GL46.glNamedBufferSubData(waterIndirectBuffer, 0, waterCommands.toArray());
        GL46.glNamedBufferSubData(glassIndirectBuffer, 0, glassCommands.toArray());
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

        GL46.glViewport(0, 0, width, height);
        GL46.glPolygonMode(GL46.GL_FRONT_AND_BACK, GL46.GL_FILL);
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glCullFace(GL46.GL_FRONT);
        GL46.glDisable(GL46.GL_STENCIL_TEST);
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glDepthFunc(GL46.GL_LESS);
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, framebuffer);
        GL46.glClear(GL46.GL_DEPTH_BUFFER_BIT | GL46.GL_COLOR_BUFFER_BIT);
        GL46.glDepthMask(true);
        GL46.glColorMask(false, false, false, false);

        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, occluderBuffer);
        GL46.glDrawArraysInstanced(GL46.GL_TRIANGLES, 0, 36, occluderCount);
    }

    private void renderOccludees(Position cameraPosition, Matrix4f projectionViewMatrix, int occludeeCount) {
        Shader shader = AssetManager.get(Shaders.OCCLUSION_CULLING);
        shader.bind();
        shader.setUniform("projectionViewMatrix", projectionViewMatrix);
        shader.setUniform("iCameraPosition",
                cameraPosition.intX & ~CHUNK_SIZE_MASK,
                cameraPosition.intY & ~CHUNK_SIZE_MASK,
                cameraPosition.intZ & ~CHUNK_SIZE_MASK);

        GL46.glDepthFunc(GL46.GL_LEQUAL);
        GL46.glDepthMask(false);
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, occludeeBuffer);
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 1, opaqueIndirectBuffer);
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 2, waterIndirectBuffer);
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 3, glassIndirectBuffer);

        GL46.glDrawArraysInstanced(GL46.GL_TRIANGLES, 0, 36, occludeeCount);

        GL46.glDepthMask(true);
        GL46.glColorMask(true, true, true, true);
        GL46.glViewport(0, 0, Window.getWidth(), Window.getHeight());
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glCullFace(GL46.GL_BACK);
        GL46.glDepthFunc(GL46.GL_LESS);
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
