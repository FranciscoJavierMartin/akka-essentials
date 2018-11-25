# Akka notes

## Actor basics

Every Actor type derives from
~~~
trait Actor {
    def receive: Receive
}
~~~
The ```receive``` method is the message handler and it is invoked when the actor processes a message.

##### Sending messages

``` myActor ! message ``` means *message* is sent to myActor. The message can be anything immutable and serializable.

##### Actor principles upheld
- Full encapsulation: cannot create actors manually, cannot directly call methods.
- Full parallelism
- non-blocking interaction via messages

##### Actor references
- Can be sent
- the ```self``` reference
- Reply using sender: ```sender() ! "Hello"```

## Changing actor behavior
~~~
context.become(anotherHandler, true)
~~~
*anotherHandler* is a function that returns a Receive.

If you pass *true* to the second parameter, then replace the current handler (this is the default behavior) else if you pass false, then stack the new handler on top.

##### Reverting to the previous behavior
~~~
context.unbecome()
~~~
Pops the current behavior off the stack

##### Rules
- Akka always uses the latest handler on top of the stack
- If the stack is empty, it calls ```receive```

## Child actors
Actors can create other actors
~~~
context.actorOf(Props[MyActor], "child")
~~~

##### Top-level supervisors (guardians)
- /system
- /user
- the root guardian /

##### Actor paths
~~~
/user/parent/child
~~~

##### Actor selections
~~~
system.actorSelection("/user/parent/child")
~~~
works with *context* as well

## Testing actors
### Test suite definition
- recommended: use a companion as well
~~~
class MySuperSpec extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll
~~~

### Test structure
~~~
"The thing being tested" should {
    "do this" in {
        // testing scenario
    }
    
    "do something else" in {
        // another testing scenario
    }
~~~

### Message scenario
~~~
val message = expectMsg("Hello")

expectNoMsg(1 second)

val anotherMessage = expectMsgType[String]

val anyMessage = expectMsgAnyOf("hello", "world")

val messageSeq = expectMagAllOf("hello", "world")

val message = receiven(2)
~~~
Has a default timeout fo 3 seconds (is configurable)

~~~
expectMsgPF(){
    case "hello" =>
}
~~~
We only care that the PF is defined

### TestProbes
TestProbes are useful for interactions with multiple actors
~~~
val probe = TestProbe("TestPRobeName")
~~~

Can send messages or reply
~~~
probe.send(actorUnderTest, "a message")
probe.reply("a message")
~~~
Reply to the last sender

### Timed assertions
Put a time cap on the assertions
~~~
within(500.milles, 1 second) {
    // everything in here must pass
}
~~~

Receive and process messages during a time window
~~~
val results = receiveWhile[Int](max = 2 seconds, idle = Duration.Zero, messages = 10){
    case WorkResult(...) => // some value
}
~~~
then do assertions based on the results

*TestProbes don't listen to ```within``` blocks!*

### Intercepting loggins
Use EventFilters to intercept logs
~~~
EventFilter.info("my log message", occurrences = 1) intercept {
    // your test here
}
~~~
works for all log levels: debug, info, warning, error

##### Intercept exceptions
~~~
EventFilter[RunTimeException](occurrences = 1) intercept {
    // your test here
}
~~~

Good for integration tests where:
- it's hard to do message-based testing
- there are logs to inspect

### Synchronous testing
Synchronous tests: all messages are handled in the calling thread

##### Option 1: TestActorRef
~~~
val syncActor = TestActorRef[MyActor](Props[MyActor])
assert(syncActor.underlyingActor.member == 1)
syncActor.receive(Tick)
~~~
needs an implicit ActorSystem

##### Option 2: CallingThreadDispatcher
~~~
val syncActor = system.actorOf(Props[MyActor].withDispatcher(CallingThreadDispatcher.Id))
~~~

## Fault tolerance
### Stopping Actors
##### Using *context.stop*

~~~
context.stop(child)
~~~
asynchronous - actor may continue to receive messages until actually stopped
~~~
context.stop(self)
~~~
will recursively stop children (asynchronously)

##### Using special messages
~~~
actor ! PoisonPill
~~~
~~~
actor ! Kill
~~~
makes the actor throw ans ActorKilledException

##### Death watch
~~~
context.watch(actor)
~~~
- I will receive a Terminated message when this actor dies.
- Can watch more than one actor, not necessarily children
- I will receive it even if the actor is already dead

### Supervision
Parents decide on their children's failure with a supervision strategy

~~~
override val supervisorStrategy(maxNrOfRetries = 10, maxNrOfRetries = 1 minute) {
    case e: IllegalArgumentException => Restart
    // other cases here
}
~~~
An alternative strategy is AllForOneStrategy

Results:
- fault tolerance
- self-healing

### Backoff Supervisor pattern
Pain: the repeated restarts of actors
- restarting immediately might be useless
- everyone attempting at the same time can kill resources again

~~~
BackoffSupervisor.props(
    Backoff.onFailure( // controls when backoff kicks in
        Props[MyActor],
        "myActor",
        3 seconds, // min and max delay
        30 seconds,
        0.2) // randomness factor
)
~~~

## Akka infrastructure
### Schedulers and timers

- Schedule and action at a defined point in the future
~~~
val schedule = system.scheduler.scheduleOnce(1 second){
    // Your code
}
~~~

- Repeated action
~~~
val schedule = system.scheduler.schedule(1 second, 2 seconds) {
    // your code
}
~~~
1 second means the initial delay and 2 seconds represent the interval duration. To cancel the interval, use ```schedule.cancel()```

- Timers: schedule messages to self, from within
~~~
timers.startSingleTimer(MyTimerKey, MyMessage, 2 seconds)
~~~
~~~
timers.startPeriodicTimer(MyTimerKey, MyMessage, 2 seconds)
~~~
~~~
timers.cancel(MyTimerKey)
~~~

### Routers
Goal: spread/delegate messages in between N identical actors

- Router method #1: manual - ignored

- Router method #2: pool routers
~~~
val router = system.actorOf(RoundRobinPool(5).props(Props[MyActor]),"myRouter")
~~~
~~~
val router = system.actorOf(FromConfig.props(Props[MyActor]), "myPoolRouter")
~~~

- Router method #3: group routers
~~~
val router = system.actorOf(RoundRobinGroup(paths), "myRouter")
~~~
~~~
val router = system.actorOf(FromConfig.props(Props[MyActor]), "myGroupRouter")
~~~
Special messages: Broadcast, PoisonPill, Kill, AddRoute & co

## Akka patterns
### Stash
Put messages aside for later
- mix-in the Stash trait
~~~
extends Actor ... with Stash
~~~
- stash the message away
~~~
stash()
~~~
- empty the stash
~~~
unstashAll()
~~~

Things to be careful about
- potential memory bounds on Stash
- potential mailbox bounds when unstashing
- no stashing twice
- the Stash trait overrides *preRestart* so must be mixed-in last

### Ask pattern
Use ask when you expect a single response
~~~scala
val askFuture = actor ? Read("Daniel")
~~~
Process the future
~~~scala
askFuture.onComplete {
    case ...
}
~~~

Pipe it
~~~scala
askFuture.mapTo[String].pipeTo(actor)
~~~

Be very careful wit ask ans Futures. Never call methods or access mutable state in callbacks

### Finite state machines
Used when context.become gets too complicated
~~~scala
class VendingMachine extends FSM[VendingState, VendingData]
~~~
~~~scala
startWith(InitialState, InitialData)
~~~
~~~scala
when(Operational) { // 1
    case Event(message, currentData) => // 2
        // handle the message
        goto(Operational) using someOtherData // 3
}
~~~
1. Handle each state separately
2. When you receive a message an Event is triggered
3. Change the state and data. May also use ```stay()```

~~~scala
whenUnhandled{
  case ...
}
~~~
~~~scala
onTransition{
  // code
}
~~~
Remember call ```initialize()```

