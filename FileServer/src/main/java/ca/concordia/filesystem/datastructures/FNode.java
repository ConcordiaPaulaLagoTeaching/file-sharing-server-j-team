package ca.concordia.filesystem.datastructures;

public class FNode {

    private int blockIndex;  // physical block index in data section
    private int next;        // index of next FNode, -1 if none

    public FNode() {
        this.blockIndex = -1;
        this.next = -1;
    }

    public FNode(int blockIndex) {
        this.blockIndex = blockIndex;
        this.next = -1;
    }

    public boolean isFree() {
        return blockIndex < 0;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    public int getNext() {
        return next;
    }

    public void setNext(int next) {
        this.next = next;
    }
}

