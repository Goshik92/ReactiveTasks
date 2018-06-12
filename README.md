# Introduction

Sometimes it is necessary to share a resource among its users in such a way that it is accessed sequentially. For example, if you communicate with Arduino via the serial port by sending textual commands, you cannot interleave your messages and need to start a new one only when the previous message is delivered. That may result in complicated code, especially if you need to constantly repeat some messages without blocking the others. This library allows to simplify such resource sharing by taking advantage of RxJava. 

# Examples

## A task that does not produce results

Suppose we need to send commands controlling LEDs to Arduino via the serial port. To make sure that the commands do not interfere let us turn them into tasks:

```Java
// We use Void here because the task does not produce any results
SwitchLedTask implements Task<Void>
{
	private final int ledId;
	private final boolean ledState;
	
	public SwitchLedTask(int ledId, boolean ledState)
	{
		this.ledId = ledId;
		this.ledState = ledState;
	}
	
	@Override
	public void routine(ObservableEmitter<Void> emitter) throws Exception
	{
		// Send the command via a dummy serial port
		System.out.println("LED" + ledId + "=" + ledState);
		
		// Emulate communication delay
		Thread.sleep(1000);
		
		// Emulate communication error that may happen once in a while
		if (Math.random() < 0.1) throw new Exception("Communication error");
	}
}

```

Then, create a TaskExecutor that will sequentially run the tasks that you feed into it:

```Java
TaskExecutor executor = new TaskExecutor();
executor.start();
```

Now we can run the task:
```Java
SwitchLedTask t1 = new SwitchLedTask(1, true);
Completable c1 = executor.prepareTask(t1).ignoreElements();
c1.subscribe(); // at this moment the task is added to the queue for execution
```

Since the task is executed asynchronously, you might want to do something upon its completion:
```Java
c1.subscribe(() -> 
{
	// Do something on completion
	// ...
});
```

You can easily [repeat](http://reactivex.io/documentation/operators/repeat.html) the task as many times as you need:
```Java
c1.repeat(3).subscribe();
```
If no error happens that results in:
```
LED1=true
LED1=true
LED1=true
```
Or you can [retry](http://reactivex.io/documentation/operators/retry.html) to make sure your task completes without errors:

```Java
c1.retry().subscribe(
		// Do nothing on completion
		() -> {},
		// Catch error and print its message
		e -> System.out.println(e.getMessage()));
```
If an error happens that code results in:
```
Communication error
LED1=true
```

To delay repetition or retrial or to examine the exception you get to decide whether or not proceed retrials, you need to use [retryWhen](http://reactivex.io/documentation/operators/retry.html) and [repeatWhen](http://reactivex.io/documentation/operators/repeat.html). 

## A task that produces results

TBD

# Requirements
* Java 8
* RxJava2