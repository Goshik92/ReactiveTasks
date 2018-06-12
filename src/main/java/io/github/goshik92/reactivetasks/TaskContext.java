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

import io.reactivex.ObservableEmitter;

/**
 *  
 * Keeps a {@link Task}, its priority, and an emitter
 * that transfers the results of task execution to the subscriber.
 * @author Goshik (goshik92@gmail.com)
 *
 * @param <R> the type of the task execution result
 */
class TaskContext<R> implements Comparable<TaskContext<?>>
{
	/**
	 * The task that will be executed in {@code this} context
	 */
	private final Task<R> task;
	
	/**
	 * The emitter that transfers the results of task execution to the subscriber
	 */
	private final ObservableEmitter<R> emitter;
	
	/**
	 * The priority of the task (the higher the number the lower the priority)
	 */
	private final int priority;
	
	/** 
	 * @param task the task that will be executed in <i>this</i> context
	 * @param emitter the emitter that transfers the results of task execution to the subscriber
	 * @param priority the priority of the task (the higher the number the lower the priority)
	 */
	public TaskContext(Task<R> task, ObservableEmitter<R> emitter, int priority)
	{
		this.task = task;
		this.emitter = emitter;
		this.priority = priority;
	}

	/**
	 * Compares contexts according to their priorities
	 */
	@Override
	public int compareTo(TaskContext<?> o)
	{
		return Integer.compare(priority, o.priority);
	}
	
	/**
	 * Executes the task in {@code this} context
	 * @throws InterruptedException if current thread was interrupted
	 */
	public void run() throws InterruptedException
	{
		try 
		{		
			// Run the task
			task.routine(emitter);
			
			// Notify the subscriber that the task was completed
			emitter.onComplete();
		}
		
		catch (Exception e)
		{
			// Notify the subscriber about the error
			emitter.onError(e);
			
			// This exception should not occur during
			// normal task execution, so we re-throw it
			if (e instanceof InterruptedException) throw (InterruptedException)e;
		}
	}
}