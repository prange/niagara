package org.kantega.niagara.task;

import fj.Unit;
import org.kantega.niagara.Try;
import org.kantega.niagara.task.Action.Pure;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ActionRunner<A> implements Fiber<A> {

    final RTS rts;
    final Function<Throwable, Action<Unit>> defaultHandler;
    private Function<Try<Object>, Action<Object>> cont = null;
    private Action<Object> currentAction;
    private volatile boolean interrupted = false;
    private boolean running = true;
    private CompletableFuture<Try<A>> callback = new CompletableFuture<>();

    public ActionRunner(RTS rts, Function<Throwable, Action<Unit>> defaultHandler) {
        this.rts = rts;
        this.defaultHandler = defaultHandler;
    }


    public void run() {
        running = true;
        while (running && !interrupted) {
            System.out.println("Evaluating " + currentAction+" with "+callback);
            switch (currentAction.tag()) {
                case pure: {
                    var ca = ActionRunner.<Pure<Object>>as(currentAction);
                    if (cont != null) {
                        currentAction = cont.apply(Try.value(ca.value));
                        cont = null;
                    } else {
                        System.out.println("Calling back with " + ca.value + " on " + callback);
                        callback.complete(Try.value((A) ca.value));
                        running = false;
                    }
                    break;
                }
                case bind: {
                    var ca = ActionRunner.<Action.Bind<Object, Object>>as(currentAction);
                    currentAction = ca.action;
                    if (cont != null) {
                        Function<Try<Object>, Action<Object>> next = cont;
                        Function<Try<Object>, Action<Object>> bindFunc = ca.bindFunction;
                        cont = objectTry -> new Action.Bind<>(bindFunc.apply(objectTry), next);
                    } else {
                        cont = ca.bindFunction;
                    }
                    break;
                }
                case fail: {
                    var ca = ActionRunner.<Action.Fail<Object>>as(currentAction);
                    if (cont == null) {
                        running = false;
                        defaultHandler.apply(ca.t);
                    } else {
                        currentAction = cont.apply(Try.fail(ca.t));
                        cont = null;
                    }
                    break;
                }
                case effect: {
                    var ca = ActionRunner.<Action.SyncrEffect<Object>>as(currentAction);
                    Try<Object> objectTry = Try.call(ca.block);
                    if (cont != null) {
                        currentAction = cont.apply(objectTry);
                        cont = null;
                    } else {
                        running = false;
                        //Done...
                    }
                    break;
                }
                case fork: {
                    var ca = ActionRunner.<Action.Fork<Object, Object>>as(currentAction);
                    rts.submit(new ActionRunner<>(rts, defaultHandler).setInitAction(ca.left));
                    rts.submit(new ActionRunner<>(rts, defaultHandler).setInitAction(ca.right));
                    currentAction = null;
                    cont = null;
                    running = false;
                    break;
                }

                case par: {
                    var ca = ActionRunner.<Action.Par<Object, Object, Object>>as(currentAction);
                    var leftRunner = new ActionRunner<>(rts, defaultHandler);
                    var rightRunner = new ActionRunner<>(rts, defaultHandler);
                    var join = new Join<>(ca.handler, leftRunner, rightRunner, rts, Optional.ofNullable(cont));
                    rts.submit(leftRunner.setInitAction(ca.left.bind(o -> Action.run(() -> join.left(o)))));
                    rts.submit(rightRunner.setInitAction(ca.right.bind(o -> Action.run(() -> join.right(o)))));
                    running = false;
                    currentAction = null;
                    cont = null;
                    break;
                }
                case callback: {
                    var ca = ActionRunner.<Action.Callback<Object>>as(currentAction);
                    currentAction = ca;
                    if (cont != null) {
                        ca.future.thenAccept(objectTry -> rts.submit(new ActionRunner<A>(rts, defaultHandler).setInitAction(cont.apply(objectTry))));
                    }
                    running = false;
                    break;
                }
                case delayed: {
                    var ca = ActionRunner.<Action.Delayed<Object>>as(currentAction);
                    ca.instantOrDelay.left().foreachDoEffect(instant ->
                      rts.schedule(
                        this.setInitAction(ca.delayedAction),
                        Duration.between(Instant.now(), instant)
                      ));

                    ca.instantOrDelay.right().foreachDoEffect(duration ->
                      rts.schedule(
                        this.setInitAction(ca.delayedAction),
                        duration)
                    );
                    running = false;
                    break;
                }

            }

        }
    }

    public <B> ActionRunner<B> setInitAction(Action<B> currentAction) {
        this.currentAction = (Action<Object>) currentAction;
        return (ActionRunner<B>) this;
    }

    @Override
    public Action<Unit> interrupt() {
        return Action.run(() -> {
            System.out.println("Interrupting");
            interrupted = true;
        });
    }

    @Override
    public Action<A> attach() {
        System.out.println("Calling attach with " + callback);
        return new Action.Callback<>(callback);
    }

    private static <T> T as(Action<?> action) {
        return (T) action;
    }
}
