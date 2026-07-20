package game.player.rendering;

import static org.lwjgl.opengl.GL46.*;

public final class MemoryAllocator {

/**
 * Creates a new MemoryAllocator instance.
 *
 * @param initialCapacity Y coordinate in local block coordinates
 */
    public MemoryAllocator(int initialCapacity) {
        capacity = initialCapacity;

        buffer = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, capacity, GL_DYNAMIC_DRAW);
        free = new MemoryRegion(0, capacity);
    }

/**
 * Performs mem alloc.
 *
 * @param size parameter
 * @return result
 */
    public int memAlloc(int size) {
        if (size <= 0) return -1;
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

        if (!growAtLeast(size)) return -1;
        return memAlloc(size);
    }

/**
 * Performs mem free.
 *
 * @param start parameter
 */
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

/**
 * Returns the used.
 * @return result
 */
    public int getUsed() {
        int used = 0;
        MemoryRegion region = this.used;
        while (region != null) {
            used += region.size;
            region = region.next;
        }
        return used;
    }

/**
 * Returns the free.
 * @return result
 */
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

/**
 * Returns the highest allocated.
 * @return result
 */
    public int getHighestAllocated() {
        int max = -1;
        MemoryRegion region = used;
        while (region != null) {
            max = Math.max(max, region.start + region.size);
            region = region.next;
        }
        return max;
    }

    public void cleanUp() {
        glDeleteBuffers(buffer);
    }


/**
 * Performs grow at least.
 *
 * @param size parameter
 * @return true if the condition holds
 */
    private boolean growAtLeast(int size) {
        int oldCapacity = capacity;
        int newCapacity = Math.clamp((long) capacity << 1L, capacity + size, glGetInteger(GL_MAX_SHADER_STORAGE_BLOCK_SIZE));
        if (newCapacity < oldCapacity + size) return false;

        capacity = newCapacity;
        int newBuffer = glGenBuffers();

        glBindBuffer(GL_COPY_READ_BUFFER, buffer);
        glBindBuffer(GL_COPY_WRITE_BUFFER, newBuffer);
        glBufferData(GL_COPY_WRITE_BUFFER, capacity, GL_DYNAMIC_DRAW);
        glCopyBufferSubData(GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER, 0, 0, oldCapacity);
        glDeleteBuffers(buffer);
        buffer = newBuffer;

        if (free == null) free = new MemoryRegion(oldCapacity, 0);
        MemoryRegion last = free;
        while (last.next != null) last = last.next;
        if (last.start + last.size == oldCapacity) last.size = last.size + capacity - oldCapacity;
        else last.next = new MemoryRegion(oldCapacity, capacity - oldCapacity);
        return true;
    }

/**
 * Removes free after.
 *
 * @param previous parameter
 * @param region parameter
 */
    private void removeFreeAfter(MemoryRegion previous, MemoryRegion region) {
        if (previous == null) {
            free = region.next;
            return;
        }
        previous.next = region.next;
    }

/**
 * Removes used after.
 *
 * @param previous parameter
 * @param region parameter
 */
    private void removeUsedAfter(MemoryRegion previous, MemoryRegion region) {
        if (previous == null) {
            used = region.next;
            return;
        }
        previous.next = region.next;
    }

/**
 * Removes from used.
 *
 * @param start parameter
 * @return result
 */
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
