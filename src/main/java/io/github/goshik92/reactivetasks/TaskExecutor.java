/*******************************************************************************
 * Copyright 2018 Igor Semenov (goshik92@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *******************************************************************************/
package io.github.goshik92.reactivetasks;

import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/**
 * Puts tasks ({@link Task}) into the internal priority queue,
 * takes them according to their priority, and executes them sequentially in a separate thread.
 * @author Goshik (goshik92@gmail.com)
 *
 */
public class TaskExecutor
{
	/**
	 * Counter of objects of this class to identify their threads
	 */
	private static final AtomicLong counter = new AtomicLong(1);
	
	/**
	 * The name of the thread for task execution
	 */
	private final String threadName = this.getClass().getName() + "-" + counter.getAndIncrement();
	
	/**
	 * The thread for task execution
	 */
	private Thread thread;
	
	/**
	 * The state of the executor
	 * (true - tasks are being executed, false - execution is stopped)
	 */
	private boolean running = false;
	
	/**
	 * The queue for the tasks
	 */
	private final PriorityBlockingQueue<TaskContext<?>> queue = new PriorityBlockingQueue<>();
	
	/**
	 * Starts execution of the tasks
	 */
	public final synchronized void start()
	{		
		// Check if execution was already started
		if (running) throw new IllegalStateException("Execution was already started");
		running = true;
		
		// Create the thread for task execution
		thread = new Thread(() -> 
		{
			// Take tasks from the queue until the thread is stopped 
			try { while(!thread.isInterrupted()) queue.take().run(); }
			catch(InterruptedException e){}
		});
		
		// Run the thread
		thread.setName(threadName);
		thread.start();
	}
	
	/**
	 * Stops execution of the tasks
	 * @throws InterruptedException 
	 */
	public final synchronized void stop() throws InterruptedException
	{
		// Check if execution was already stopped
		if (!running) throw new IllegalStateException("Execution was already stopped");
		running = false;
		
		// Interrupt the thread and wait until it stops
		thread.interrupt();
		thread.join();
	}
	
	/**
	 * Makes the {@code task} ready to be added into the priority queue.
	 * The actual adding will happen when after subscription on the returned {@link Observable}.
	 * Warning! Every time you subscribe to the {@link Observable}, the task is added to the queue.
	 * @param task the task to be added into the queue
	 * @param the priority of the task (the higher the number the lower the priority
	 * , see {@link PriorityQueue} for further detail)
	 * @return
	 */
	public <R> Observable<R> prepareTask(Task<R> task, int priority)
	{
		return Observable
				.<R>create(emitter -> queue.add(new TaskContext<>(task, emitter, priority)))
				.observeOn(Schedulers.computation());
	}
	
	/**
	 * @return unique name of the task execution thread
	 */
	public String getThreadName()
	{
		return threadName;
	}
}
