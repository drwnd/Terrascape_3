package game.player.rendering;

final class MemoryRegion {
    int start;
    int size;
    MemoryRegion next = null;

    MemoryRegion(int start, int size) {
        this.start = start;
        this.size = size;
    }
}
