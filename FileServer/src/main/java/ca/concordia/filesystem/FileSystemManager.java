package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private static final int BLOCK_SIZE = 128;

    private FEntry[] entries;
    private FNode[] fnodes;
    private boolean[] freeBlockList;

    private final RandomAccessFile disk;

    private int readers = 0;
    private boolean writerActive = false;

    public FileSystemManager(String fileName, int totalSize) {
        try {
            disk = new RandomAccessFile(fileName, "rw");

            entries = new FEntry[MAXFILES];
            fnodes = new FNode[MAXBLOCKS];
            freeBlockList = new boolean[MAXBLOCKS];

            for (int i = 0; i < MAXFILES; i++)
                entries[i] = new FEntry("", (short)0, (short)-1);

            for (int i = 0; i < MAXBLOCKS; i++) {
                fnodes[i] = new FNode(-1);
                freeBlockList[i] = true;
            }

        } catch (Exception e) {
            throw new RuntimeException("Could not initialize filesystem", e);
        }
    }

    private synchronized void beginRead() throws InterruptedException {
        while (writerActive)
            wait();
        readers++;
    }

    private synchronized void endRead() {
        readers--;
        if (readers == 0)
            notifyAll();
    }

    private synchronized void beginWrite() throws InterruptedException {
        while (writerActive || readers > 0)
            wait();
        writerActive = true;
    }

    private synchronized void endWrite() {
        writerActive = false;
        notifyAll();
    }

    private long blockToOffset(int blockIndex) {
        return (1 + blockIndex) * BLOCK_SIZE;
    }

    private int allocateFreeNode() {
        for (int i = 0; i < fnodes.length; i++) {
            if (fnodes[i].getBlockIndex() < 0)
                return i;
        }
        return -1;
    }

    private int allocateFreeBlock() {
        for (int i = 0; i < freeBlockList.length; i++) {
            if (freeBlockList[i]) {
                freeBlockList[i] = false;
                return i;
            }
        }
        return -1;
    }

    // ----------------------------------------------------------
    // CREATE FILE
    // ----------------------------------------------------------
    public void createFile(String fileName) throws Exception {
        beginWrite();
        try {
            if (fileName == null || fileName.length() > 11)
                throw new IllegalArgumentException("ERROR: filename too large");

            for (FEntry e : entries) {
                if (e.isUsed() && e.getFilename().equals(fileName))
                    throw new IllegalArgumentException("ERROR: file already exists");
            }

            int index = -1;
            for (int i = 0; i < entries.length; i++) {
                if (!entries[i].isUsed()) {
                    index = i;
                    break;
                }
            }
            if (index == -1)
                throw new Exception("ERROR: no free file entries");

            FEntry e = entries[index];
            e.setFilename(fileName);
            e.setFilesize((short)0);
            e.setFirstBlock((short)-1);
            e.setUsed(true);

        } finally {
            endWrite();
        }
    }

    // ----------------------------------------------------------
    // DELETE FILE
    // ----------------------------------------------------------
    public void deleteFile(String fileName) throws Exception {
        beginWrite();
        try {
            int index = -1;
            for (int i = 0; i < entries.length; i++) {
                if (entries[i].isUsed() && entries[i].getFilename().equals(fileName)) {
                    index = i;
                    break;
                }
            }
            if (index == -1)
                throw new IllegalArgumentException("ERROR: file does not exist");

            FEntry entry = entries[index];

            int node = entry.getFirstBlock();
            while (node != -1) {
                FNode f = fnodes[node];

                int block = f.getBlockIndex();
                if (block >= 0)
                    freeBlockList[block] = true;

                int next = f.getNext();
                f.setBlockIndex(-1);
                f.setNext(-1);

                node = next;
            }

            entry.setUsed(false);
            entry.setFilename("");
            entry.setFilesize((short)0);
            entry.setFirstBlock((short)-1);

        } finally {
            endWrite();
        }
    }

    // ----------------------------------------------------------
    // WRITE FILE
    // ----------------------------------------------------------
    public void writeFile(String fileName, byte[] contents) throws Exception {
        beginWrite();
        try {
            int index = -1;
            for (int i = 0; i < entries.length; i++) {
                if (entries[i].isUsed() && entries[i].getFilename().equals(fileName)) {
                    index = i;
                    break;
                }
            }
            if (index == -1)
                throw new IllegalArgumentException("ERROR: file does not exist");

            FEntry entry = entries[index];

            int needed = (int)Math.ceil(contents.length / (double)BLOCK_SIZE);

            int freeCount = 0;
            for (boolean b : freeBlockList) if (b) freeCount++;
            if (freeCount < needed)
                throw new Exception("ERROR: file too large");

            int nodePtr = entry.getFirstBlock();
            while (nodePtr != -1) {
                FNode f = fnodes[nodePtr];

                int block = f.getBlockIndex();
                if (block >= 0)
                    freeBlockList[block] = true;

                int next = f.getNext();
                f.setBlockIndex(-1);
                f.setNext(-1);

                nodePtr = next;
            }

            int firstNode = -1;
            int prev = -1;
            int offset = 0;

            for (int i = 0; i < needed; i++) {

                int newNode = allocateFreeNode();
                int block = allocateFreeBlock();

                disk.seek(blockToOffset(block));
                int bytes = Math.min(BLOCK_SIZE, contents.length - offset);
                disk.write(contents, offset, bytes);
                offset += bytes;

                fnodes[newNode].setBlockIndex(block);
                fnodes[newNode].setNext(-1);

                if (prev != -1)
                    fnodes[prev].setNext(newNode);

                if (firstNode == -1)
                    firstNode = newNode;

                prev = newNode;
            }

            entry.setFirstBlock((short) firstNode);
            entry.setFilesize((short) contents.length);

        } finally {
            endWrite();
        }
    }

    // ----------------------------------------------------------
    // READ FILE
    // ----------------------------------------------------------
    public byte[] readFile(String fileName) throws Exception {
        beginRead();
        try {
            FEntry entry = null;
            for (FEntry e : entries) {
                if (e.isUsed() && e.getFilename().equals(fileName)) {
                    entry = e;
                    break;
                }
            }
            if (entry == null)
                throw new IllegalArgumentException("ERROR: file does not exist");

            if (entry.getFirstBlock() == -1)
                return new byte[0];

            int size = entry.getFilesize();
            byte[] result = new byte[size];

            int node = entry.getFirstBlock();
            int offset = 0;

            while (node != -1) {
                FNode f = fnodes[node];
                int block = f.getBlockIndex();

                disk.seek(blockToOffset(block));
                int bytes = Math.min(BLOCK_SIZE, size - offset);
                disk.readFully(result, offset, bytes);

                offset += bytes;
                node = f.getNext();
            }

            return result;

        } finally {
            endRead();
        }
    }

    // ----------------------------------------------------------
    // LIST FILES
    // ----------------------------------------------------------
    public String[] listFiles() {
        try {
            beginRead();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        try {
            List<String> list = new ArrayList<>();
            for (FEntry e : entries)
                if (e.isUsed())
                    list.add(e.getFilename());
            return list.toArray(new String[0]);
        } finally {
            endRead();  
        }
    }
}

