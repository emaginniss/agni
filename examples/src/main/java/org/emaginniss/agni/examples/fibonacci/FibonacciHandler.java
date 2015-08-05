package org.emaginniss.agni.examples.fibonacci;

import org.emaginniss.agni.annotations.Subscribe;

/**
 * Created by Eric on 8/4/2015.
 */
public class FibonacciHandler {

    @Subscribe
    public FibonacciResponse handle(FibonacciRequest in) {
        long start = System.currentTimeMillis();
        long previous = 0;
        long current = 1;
        for (int i = 0; i < in.getIndex(); i++) {
            long hold = previous + current;
            previous = current;
            current = hold;
        }
        long end = System.currentTimeMillis();

        return new FibonacciResponse(in.getIndex(), current, (int) (end - start));
    }

}
