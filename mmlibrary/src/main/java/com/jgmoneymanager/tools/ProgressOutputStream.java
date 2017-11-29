package com.jgmoneymanager.tools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Ceyhun on 06.10.2017.
 */

public class ProgressOutputStream extends OutputStream {

    OutputStream underlying;
    ProgressOutputListener listener;
    int completed;
    long totalSize;

    public ProgressOutputStream(long totalSize, FileOutputStream underlying, ProgressOutputListener listener) {
        this.underlying = underlying;
        this.listener = listener;
        this.completed = 0;
        this.totalSize = totalSize;
    }

    @Override
    public void write(byte[] data, int off, int len) throws IOException {
        this.underlying.write(data, off, len);
        track(len);
    }

    @Override
    public void write(byte[] data) throws IOException {
        this.underlying.write(data);
        track(data.length);
    }

    @Override
    public void write(int c) {
        try {
            this.underlying.write(c);
        } catch (IOException e) {
            e.printStackTrace();
        }
        track(1);
    }

    private void track(int len) {
        this.completed += len;
        this.listener.progress(this.completed, this.totalSize);
    }

    public interface ProgressOutputListener {
        public void progress(long completed, long totalSize);
    }
}