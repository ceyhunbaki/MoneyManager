package com.jgmoneymanager.tools;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Ceyhun on 06.10.2017.
 */

public class ProgressInputStream extends InputStream {

    private InputStream stream;
    private ProgressInputListener progressListener;
    private long offset = 0;

    public ProgressInputStream(InputStream stream, ProgressInputListener  progressListener) {
        this.stream = stream;
        this.progressListener = progressListener;
    }

    @Override
    public int read() throws IOException {
        int res = this.stream.read();
        this.progressListener.onProgressChanged(++this.offset);

        return res;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int res = this.stream.read(b, off, len);
        this.offset += res;
        this.progressListener.onProgressChanged(this.offset);

        return res;
    }

    // You might need to override additional methods but you can just
    // call the corresponding method on the stream property

    public interface ProgressInputListener {
        void onProgressChanged(long bytes);
    }
}
