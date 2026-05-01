package game.server.generation;

final class NativeFunctions {

    private NativeFunctions() {
    }

    public static native int[] generateMesh(byte[] materialsData, byte[] surfaceEquivalent,
                                            byte[] north, byte[] top, byte[] west, byte[] south, byte[] bottom, byte[] east,
                                            int xStart, int yStart, int zStart);
}
