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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

public class Tests
{
	@Test
	public void testPriorities() throws InterruptedException
	{
		assert(Task.PRIORITY_HIGHEST < Task.PRIORITY_HIGHER);
		assert(Task.PRIORITY_HIGHER < Task.PRIORITY_NORMAL);
		assert(Task.PRIORITY_NORMAL < Task.PRIORITY_LOWER);
		assert(Task.PRIORITY_LOWER < Task.PRIORITY_LOWEST);
	}
		
	@RepeatedTest(10)
	public void testTaskOrder() throws InterruptedException
	{
		List<Observable<Integer>> observables = new ArrayList<>();
		TaskExecutor executor = new TaskExecutor();
		
		Task<Integer> t1 = e -> { Thread.sleep(5); e.onNext(1); };
		observables.add(executor.prepareTask(t1, Task.PRIORITY_LOWEST));
		
		Task<Integer> t2 = e -> { Thread.sleep(5); e.onNext(2); };
		observables.add(executor.prepareTask(t2, Task.PRIORITY_NORMAL));
		
		Task<Integer> t3 = e -> { Thread.sleep(5); e.onNext(3); };
		observables.add(executor.prepareTask(t3, Task.PRIORITY_HIGHEST));
		
		Task<Integer> t4 = e -> { Thread.sleep(5); e.onNext(4); };
		observables.add(executor.prepareTask(t4, Task.PRIORITY_HIGHER));
		
		Task<Integer> t5 = e -> { Thread.sleep(5); e.onNext(5); };
		observables.add(executor.prepareTask(t5, Task.PRIORITY_LOWER));
		
		TestObserver<Integer> observer = Observable
				.merge(observables)
				.test()
				.assertSubscribed();
		
		executor.start();
		
		observer.await()
				.assertComplete()
				.assertValues(3, 4, 2, 5, 1);
	}
	
	@RepeatedTest(10)
	public void testErrors() throws InterruptedException
	{
		TaskExecutor executor = new TaskExecutor();
		executor.start();
		
		Task<Integer> t = e -> { throw new IllegalStateException(); };
		executor.prepareTask(t, Task.PRIORITY_LOWEST)
				.test()
				.await()
				.assertError(IllegalStateException.class)
				.assertNotComplete();
	}
}
