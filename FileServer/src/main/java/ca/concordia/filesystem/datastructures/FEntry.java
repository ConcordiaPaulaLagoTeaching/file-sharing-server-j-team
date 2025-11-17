package ca.concordia.filesystem.datastructures;

public class FEntry {

    private String filename;
    private short filesize;
    private short firstBlock; 
    private boolean used;


    public FEntry() {
    this.filename = "";
    this.filesize = 0;
    this.firstBlock = -1;
    }

    public FEntry(String filename, short filesize, short firstBlock) {
        setFilename(filename);
        setFilesize(filesize);
        this.firstBlock = firstBlock;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public boolean isUsed() {
        return filename != null && !filename.isEmpty();
    }

    // Getters and Setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) throws IllegalArgumentException {
        if (filename == null) {
           this.filename = "";
            return;
        }
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
    }

    public short getFilesize() {
        return filesize;
    }

    public void setFilesize(short filesize) {
        if (filesize < 0) {
            throw new IllegalArgumentException("Filesize cannot be negative.");
        }
        this.filesize = filesize;
    }

    public short getFirstBlock() {
        return firstBlock;
    }

    public void setFirstBlock(short firstBlock) {
        this.firstBlock = firstBlock;
    }
}

