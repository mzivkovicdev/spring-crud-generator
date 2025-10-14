package com.markozivkovic.codegen.models.flyway;
import java.util.Objects;

public class FileState {
    
    private String file;
    private String hash;

    public FileState() {

    }

    public FileState(final String file, final String hash) {
        this.file = file;
        this.hash = hash;
    }

    public String getFile() {
        return this.file;
    }

    public FileState setFile(final String file) {
        this.file = file;
        return this;
    }

    public String getHash() {
        return this.hash;
    }

    public FileState setHash(final String hash) {
        this.hash = hash;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FileState)) {
            return false;
        }
        final FileState fileState = (FileState) o;
        return Objects.equals(file, fileState.file) && Objects.equals(hash, fileState.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, hash);
    }

    @Override
    public String toString() {
        return "{" +
            " file='" + getFile() + "'" +
            ", hash='" + getHash() + "'" +
            "}";
    }    

}
