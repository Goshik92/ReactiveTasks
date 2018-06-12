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
 * Represents a block of code that will executed by {@link TaskExecutor}
 * in sequential manner relating to other such blocks.
 * @author Goshik (goshik92@gmail.com)
 *
 * @param <R> the type of the task execution result
 */
public interface Task<R>
{
	/**
	 * The lowest possible priority
	 */
	public static final int PRIORITY_LOWEST = Integer.MAX_VALUE;
	
	/**
	 * This priority is higher than {@link #PRIORITY_LOWEST},
	 * but lower than {@link #PRIORITY_NORMAL}
	 */
	public static final int PRIORITY_LOWER = 0x3fffffff;
	
	/**
	 * This priority is halfway between {@link #PRIORITY_LOWER} and {@link #PRIORITY_HIGHER}
	 */
	public static final int PRIORITY_NORMAL = 0;
	
	/**
	 * This priority is halfway between {@link #PRIORITY_NORMAL} and {@link #PRIORITY_HIGHEST}
	 */
	public static final int PRIORITY_HIGHER = 0xc0000000;
	
	/**
	 * The highest possible priority
	 */
	public static final int PRIORITY_HIGHEST = Integer.MIN_VALUE;
	
	/**
	 * An implementation of this method should contain the program code of the task.
	 * The exceptions thrown by this method will be automatically delivered
	 * to the subscriber through emitter.onError(...).
	 * You can call emitter.onNext(...) (probably more than once) to send
	 * the results that your task produces.
	 * You should not normally call emitter.onComplete() inside this method
	 * because it is be automatically called after the method finishes.
	 * @param emitter the emitter to deliver the results of task execution to the subscriber.
	 * @throws Exception any exception that prevents your task from further execution
	 */
	public abstract void routine(ObservableEmitter<R> emitter) throws Exception;
}
