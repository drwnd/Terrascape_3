package core.utils;

public final class ArrayQueue<E> {

    private int headPointer = 0;
    private int tailPointer = 0;
    private Object[] elements;

    public ArrayQueue(int capacity) {
        elements = new Object[Math.max(2, capacity)];
    }

    public void enqueue(E element) {
        if (incIndex(tailPointer) == headPointer) grow();
        elements[tailPointer] = element;
        tailPointer = incIndex(tailPointer);
    }

    public E dequeue() {
        E element = (E) elements[headPointer];
        elements[headPointer] = null;
        headPointer = incIndex(headPointer);
        return element;
    }

    public E front() {
        return (E) elements[headPointer];
    }

    public boolean isEmpty() {
        return headPointer == tailPointer;
    }

    public boolean contains(E element) {
        for (int index = headPointer; index != tailPointer; index = incIndex(index))
            if (elements[index] == element) return true;
        return false;
    }

    public int size() {
        return headPointer <= tailPointer ? tailPointer - headPointer : elements.length + tailPointer - headPointer;
    }

    private void grow() {
        Object[] newElements = new Object[elements.length << 1];

        int index = headPointer;
        int newIndex = 0;
        while (index != tailPointer) {
            newElements[newIndex] = elements[index];
            newIndex++;
            index = incIndex(index);
        }
        elements = newElements;
        headPointer = 0;
        tailPointer = newIndex;
    }

    private int incIndex(int index) {
        return index + 1 >= elements.length ? 0 : index + 1;
    }
}
