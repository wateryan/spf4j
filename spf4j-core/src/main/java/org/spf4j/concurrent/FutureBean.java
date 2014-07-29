 /*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.spf4j.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.base.Pair;

/**
 * bean like implementation of a future
 * @author zoly
 */
@ThreadSafe
public class FutureBean<T> implements Future<T> {
    private volatile Pair<T, ? extends ExecutionException> resultStore;

    @Override
    public final boolean cancel(final boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final boolean isCancelled() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final boolean isDone() {
        return resultStore != null;
    }
    
    public final Pair<T, ? extends ExecutionException> getResultStore() {
        return resultStore;
    }

    @Override
    // Findbugs complain here is rubbish, InterruptedException is thrown by wait
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("BED_BOGUS_EXCEPTION_DECLARATION")
    public final synchronized T get() throws InterruptedException, ExecutionException {
        while (resultStore == null) {
            this.wait();
        }
        return processResult(resultStore);
    }

    @Override
    public final T get(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        long timeoutMillis = unit.toMillis(timeout);
        long toWait = timeoutMillis;
        long startTime = System.currentTimeMillis();
        synchronized (this) {
            while (toWait > 0 && resultStore == null) {
                this.wait(toWait);
                toWait = timeoutMillis - (System.currentTimeMillis() - startTime);
            }
            if (resultStore == null) {
                throw new TimeoutException();
            }
            return processResult(resultStore);
        }
    }

    public static <T> T processResult(final Pair<T, ? extends ExecutionException> result) throws ExecutionException {
        ExecutionException e = result.getSecond();
        if (e != null) {
            throw e;
        } else {
            return (T) result.getFirst();
        }
    }

    public final synchronized void setResult(final T result) {
        if (resultStore != null) {
            throw new IllegalStateException("cannot set result multiple times " +  result);
        }
        resultStore = Pair.of(result, (ExecutionException) null);
        done();
        this.notifyAll();
    }
    
    public final synchronized void setExceptionResult(final ExecutionException result) {
        if (resultStore != null) {
            throw new IllegalStateException("cannot set result multiple times " + result);
        }
        resultStore = Pair.of(null, (ExecutionException) result);
        done();
        this.notifyAll();
    }
    
    public void done() {
    }
    
    
}
