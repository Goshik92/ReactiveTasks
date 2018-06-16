# Introduction

Sometimes it is necessary to share a resource among its users in such a way that it is accessed sequentially. For example, if you communicate with your Arduino device via the serial port by sending textual commands, you cannot interleave your messages and need to start a new one only when the previous message is delivered. That may result in complicated code, especially if you need to constantly repeat some messages without blocking the others. This library allows to simplify such resource sharing by taking advantage of RxJava. 

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
        
        // Imitate communication delay
        Thread.sleep(1000);
        
        // Imitate communication error that may happen once in a while
        if (Math.random() < 0.1) throw new Exception("Communication error");
    }
}

```

Then, create a TaskExecutor that will sequentially run the tasks that you feed into it:

```Java
TaskExecutor executor = new TaskExecutor();
executor.start();
```

Now we can run our task:
```Java
SwitchLedTask t1 = new SwitchLedTask(1, true);
// http://reactivex.io/RxJava/javadoc/io/reactivex/Completable.html
Completable completable1 = executor.prepareTask(t1).ignoreElements(); 
completable1.subscribe(); // at this moment the task is added to the queue for execution
```

Since the task is executed asynchronously, you might want to do something upon its completion:
```Java
completable1.subscribe(() -> 
{
    // Do something on completion
    // ...
});
```

You can easily [repeat](http://reactivex.io/documentation/operators/repeat.html) the task as many times as you need:
```Java
completable1.repeat(3).subscribe();
```
If no error happens, that results in:
```
LED1=true
LED1=true
LED1=true
```
Or you can [retry](http://reactivex.io/documentation/operators/retry.html) to repeat your task until it completes without errors:

```Java
completable1
        // Print error message
        .doOnError(e -> System.out.println(e.getMessage()))

        // Resubscribe every time an error occurs
        // (this will lead to adding the task in the queue again and again)
        .retry()
        
        // Subscribe to the chain
        .subscribe(() -> System.out.println("Completed"))
```
If an error happens, the code above code results in:
```
Communication error
LED1=true
Completed
```

To delay repetition or retrial, or to examine the exceptions you get for deciding whether or not proceed retrials, you need to use [retryWhen](http://reactivex.io/documentation/operators/retry.html) and [repeatWhen](http://reactivex.io/documentation/operators/repeat.html). 

## A task that produces results

We may want to read button states of our Arduino device:

```Java
ReadButtonTask implements Task<Boolean>
{
    private final int buttonId;
    
    public ReadButtonTask(int buttonId)
    {
        this.buttonId = buttonId;
    }
    
    @Override
    public void routine(ObservableEmitter<Boolean> emitter) throws Exception
    {
        // Read button state from Arduino
        // ...
    
        // Imitate communication delay
        Thread.sleep(1000);
        
        // Using emitter.onNext(...) we can send data to the subscriber
        // Button 1 is always pressed
        if (buttonId == 1) emitter.onNext(true);
        
        // Button 2 randomly changes its state
        else if (buttonId == 2) emitter.onNext(Math.random() > 0.5)
        
        // There are only two buttons
        else throw new NoSuchElementException("There is no button" + buttonId);
    }
}
```

```Java
ReadButtonTask t2 = new ReadButtonTask(1);
// http://reactivex.io/documentation/observable.html
Observable<Boolean> observable2 = executor.prepareTask(t2);
observable2.subscribe(state -> System.out.println("button1=" + state));
```

The above code gives:
```
button1=true
```

## Prioritization
It may be important to prioritize some tasks over others. For example, you may want to do a background task such as watching for the state of a button:
```Java
ReadButtonTask t3 = new ReadButtonTask(2);
Observable<Boolean> observable3 = executor.prepareTask(t3);
observable3
         // Repeat the task endlessly
        .repeat()
        
        // Filter out repeated states so that the subscriber
        // will only be notified when the state changes
        .distinctUntilChanged()
        
        // Subscribe to the chain
        .subscribe(state -> System.out.println("button2=" + state));
```

```
button2=true
button2=false
button2=true
button2=false
...
```

But the problem is that such background tasks can jam the queue and you will not be able to execute an urgent task in time. That is where priorities can help:
```Java
...
Observable<Boolean> observable3 = executor.prepareTask(t3, Task.PRIORITY_LOWEST);
...
```

Now we can do urgent tasks. Notice, however, that prioritized tasks cannot stop a background task when it is already being executed. Prioritization only affects extracting tasks from queue for further execution.
```Java
SwitchLedTask t4 = new SwitchLedTask(1, true);
Completable completable4 = executor.prepareTask(t4, Task.PRIORITY_HIGHEST).ignoreElements();
completable4.subscribe();
```

There are five standard priorities in the ```Task``` interface, but you can make up as many priority levels as ```int``` type allows. 

# Requirements
* Java 8
* [RxJava 2](https://github.com/ReactiveX/RxJava)