package org.emaginniss.agni.examples.fibonacci;

/**
 * Created by Eric on 8/4/2015.
 */
public class FibonacciResponse {
    int index;
    long value;
    long duration;

    public FibonacciResponse() {
    }

    public FibonacciResponse(int index, long value, long duration) {
        this.index = index;
        this.value = value;
        this.duration = duration;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
