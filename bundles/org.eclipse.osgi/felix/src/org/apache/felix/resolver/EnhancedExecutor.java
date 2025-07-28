/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.resolver;

import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

class EnhancedExecutor
{
    private final Executor executor;
    private final Queue<Future<Void>> awaiting = new ConcurrentLinkedQueue<Future<Void>>();
    private final AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();

    public EnhancedExecutor(Executor executor)
    {
        this.executor = executor;
    }

    public void execute(final Runnable runnable)
    {
        FutureTask<Void> task = new FutureTask<Void>(new Runnable()
        {
            public void run()
            {
                try
                {
                    runnable.run();
                }
                catch (Throwable t)
                {
                    throwable.compareAndSet(null, t);
                }
            }
        }, (Void) null);
        // must have a happens-first to add the task to awaiting
        awaiting.add(task);
        try
        {
            executor.execute(task);
        }
        catch (Throwable t)
        {
            // if the task did not get added successfully to the executor we must cancel
            // the task so we don't await on it
            task.cancel(false);
            throwable.compareAndSet(null, t);
        }
    }

    public void await()
    {
        Future<Void> awaitTask;
        while (throwable.get() == null && (awaitTask = awaiting.poll()) != null)
        {
            if (!awaitTask.isDone() && !awaitTask.isCancelled())
            {
                try
                {
                    awaitTask.get();
                }
                catch (CancellationException e)
                {
                    // ignore; will have throwable set
                }
                catch (InterruptedException e)
                {
                    throw new IllegalStateException(e);
                }
                catch (ExecutionException e)
                {
                    throw new RuntimeException(e.getCause());
                }
            }
        }
        Throwable t = throwable.get();
        if (t != null)
        {
            if (t instanceof Runnable)
            {
                throw (RuntimeException) t;
            }
            else if (t instanceof Error)
            {
                throw (Error) t;
            }
            throw new RuntimeException(t);
        }
    }
}