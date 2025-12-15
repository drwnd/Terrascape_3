package game.player.rendering;

import org.lwjgl.opengl.GL46;

public final class MemoryAllocator {

    public MemoryAllocator(int initialCapacity) {
        capacity = initialCapacity;

        buffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_SHADER_STORAGE_BUFFER, buffer);
        GL46.glBufferData(GL46.GL_SHADER_STORAGE_BUFFER, capacity, GL46.GL_DYNAMIC_DRAW);
        free = new MemoryRegion(0, capacity);
    }

    public int memAlloc(int size) {
        if (size == 0) return -1;
        MemoryRegion region = free;
        MemoryRegion previous = null;

        while (region != null) {
            if (region.size < size) {
                previous = region;
                region = region.next;
                continue;
            }

            int start = region.start;
            region.size = region.size - size;
            region.start = region.start + size;
            if (region.size == 0) removeFreeAfter(previous, region);

            MemoryRegion allocated = new MemoryRegion(start, size);
            allocated.next = used;
            used = allocated;
            return start;
        }

        growAtLeast(size);
        return memAlloc(size);
    }

    public void memFree(int start) {
        if (start == -1) return;
        MemoryRegion freed = removeFromUsed(start);
        if (freed == null) return;

        MemoryRegion before = null;
        MemoryRegion after = free;
        while (after != null && after.start < freed.start) {
            before = after;
            after = after.next;
        }

        boolean merged = false;
        if (before != null && before.start + before.size == freed.start) {
            before.size += freed.size;
            freed = before;
            merged = true;
        }
        if (after != null && freed.start + freed.size == after.start) {
            freed.size += after.size;
            if (before != null) before.next = freed;
            freed.next = after.next;
            merged = true;
        }
        if (!merged) {
            if (before == null) free = freed;
            else before.next = freed;
            freed.next = after;
        }
    }

    public int getBuffer() {
        return buffer;
    }

    public int getUsed() {
        int used = 0;
        MemoryRegion region = this.used;
        while (region != null) {
            used += region.size;
            region = region.next;
        }
        return used;
    }

    public int getFree() {
        int free = 0;
        MemoryRegion region = this.free;
        while (region != null) {
            free += region.size;
            region = region.next;
        }
        return free;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getHighestAllocated() {
        int max = -1;
        MemoryRegion region = used;
        while (region != null) {
            max = Math.max(max, region.start + region.size);
            region = region.next;
        }
        return max;
    }


    private void growAtLeast(int size) {
        int oldCapacity = capacity;
        capacity = Math.max(capacity << 1, capacity + size);

        int newBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_COPY_READ_BUFFER, buffer);
        GL46.glBindBuffer(GL46.GL_COPY_WRITE_BUFFER, newBuffer);
        GL46.glBufferData(GL46.GL_COPY_WRITE_BUFFER, capacity, GL46.GL_DYNAMIC_DRAW);
        GL46.glCopyBufferSubData(GL46.GL_COPY_READ_BUFFER, GL46.GL_COPY_WRITE_BUFFER, 0, 0, oldCapacity);
        GL46.glDeleteBuffers(buffer);
        buffer = newBuffer;

        if (free == null) free = new MemoryRegion(oldCapacity, 0);
        MemoryRegion last = free;
        while (last.next != null) last = last.next;
        if (last.start + last.size == oldCapacity) last.size = last.size + capacity - oldCapacity;
        else last.next = new MemoryRegion(oldCapacity, capacity - oldCapacity);
    }

    private void removeFreeAfter(MemoryRegion previous, MemoryRegion region) {
        if (previous == null) {
            free = region.next;
            return;
        }
        previous.next = region.next;
    }

    private void removeUsedAfter(MemoryRegion previous, MemoryRegion region) {
        if (previous == null) {
            used = region.next;
            return;
        }
        previous.next = region.next;
    }

    private MemoryRegion removeFromUsed(int start) {
        MemoryRegion used = this.used;
        MemoryRegion previous = null;

        while (used != null) {
            if (used.start == start) {
                removeUsedAfter(previous, used);
                return used;
            }
            previous = used;
            used = used.next;
        }
        return null;
    }

    private MemoryRegion free;
    private MemoryRegion used;
    private int buffer;
    private int capacity;

    private static final class MemoryRegion {
        int start;
        int size;
        MemoryRegion next = null;

        MemoryRegion(int start, int size) {
            this.start = start;
            this.size = size;
        }
    }
}
