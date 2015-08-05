/*
 * Copyright (c) 2015, Eric A Maginniss
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ERIC A MAGINNISS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.emaginniss.agni.messageboxes;

import org.emaginniss.agni.Configuration;
import org.emaginniss.agni.Envelope;
import org.emaginniss.agni.annotations.Component;
import org.emaginniss.agni.util.ExtendedDataInputStream;
import org.emaginniss.agni.util.ExtendedDataOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Component("fileBacked")
public class FileBackedMessageBox implements MessageBox {

    private final LinkedList<Path> storage = new LinkedList<>();
    private String storageLocation;
    private Path storageDir;
    private int maximumSize = 100000;
    private AtomicInteger currentSize = new AtomicInteger(0);
    private AtomicInteger currentMemorySize = new AtomicInteger(0);
    private LinkedList<Envelope> readQueue = new LinkedList<>();
    private LinkedList<Envelope> writeQueue = new LinkedList<>();

    public FileBackedMessageBox(Configuration configuration) {
        storageLocation = configuration.getString("storageLocation", null);
        maximumSize = configuration.getInt("maximumSize", 50000);
    }

    @Override
    public void enqueue(Envelope envelope) {
        synchronized (this) {
            writeQueue.addLast(envelope);
            currentSize.incrementAndGet();
            currentMemorySize.incrementAndGet();

            if (writeQueue.size() > maximumSize) {
                storeWriteQueue();
            }

            this.notify();
        }
    }

    @Override
    public List<Envelope> dequeue(int max, boolean wait) throws InterruptedException {
        List<Envelope> out = new ArrayList<>();

        while (out.size() == 0) {
            synchronized (this) {
                if (readQueue.size() == 0 && storage.size() == 0 && writeQueue.size() == 0) {
                    if (wait) {
                        this.wait();
                    } else {
                        return null;
                    }
                }

                if (readQueue.size() == 0) {
                    if (storage.size() > 0) {
                        loadReadQueue();
                    } else {
                        readQueue = writeQueue;
                        writeQueue = new LinkedList<>();
                    }
                }

                while (readQueue.size() > 0 && out.size() < max) {
                    out.add(readQueue.removeFirst());
                }

                currentMemorySize.addAndGet(0 - out.size());
                currentSize.addAndGet(0 - out.size());
            }
        }

        return out;
    }

    private void storeWriteQueue() {
        try {
            if (storageDir == null) {
                if (storageLocation == null) {
                    storageDir = Files.createTempDirectory("messagebox");
                } else {
                    storageDir = Files.createTempDirectory(new File(storageLocation).toPath(), "messagebox");
                }
            }

            Path outputPath = Files.createTempFile(storageDir, "mb", ".dat");
            FileOutputStream fos = new FileOutputStream(outputPath.toFile());
            GZIPOutputStream gzos = new GZIPOutputStream(fos);
            ExtendedDataOutputStream dos = new ExtendedDataOutputStream(gzos);

            for (Envelope envelope : writeQueue) {
                envelope.write(dos);
            }

            dos.close();
            gzos.close();
            fos.close();

            currentMemorySize.addAndGet(0 - writeQueue.size());
            writeQueue.clear();

            storage.addLast(outputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadReadQueue() {
        try {
            Path outputPath = storage.removeFirst();
            FileInputStream fis = new FileInputStream(outputPath.toFile());
            GZIPInputStream gzis = new GZIPInputStream(fis);
            ExtendedDataInputStream dis = new ExtendedDataInputStream(gzis);

            int count = 0;
            while (true) {
                try {
                    readQueue.add(Envelope.read(dis));
                    count++;
                } catch (EOFException e) {
                    break;
                }
            }

            dis.close();
            gzis.close();
            fis.close();

            currentMemorySize.addAndGet(count);

            if (!outputPath.toFile().delete()) {
                outputPath.toFile().deleteOnExit();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getMaximumSize() {
        return maximumSize;
    }

    public int getCurrentSize() {
        return currentSize.get();
    }

    public int getCurrentMemorySize() {
        return currentMemorySize.get();
    }

    @Override
    public void shutdown() {

    }
}
