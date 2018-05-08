package org.kantega.niagara.task;

import fj.Unit;
import org.kantega.niagara.Try;
import org.kantega.niagara.task.Action.Pure;

import java.util.Optional;
import java.util.function.Function;

public class ActionRunner<A> implements Fiber<A> {

    final RTS rts;
    final Function<Throwable, Action<Unit>> defaultHandler;
    private Function<Try<Object>, Action<Object>> cont = null;
    private Action<Object> currentAction;

    public ActionRunner(RTS rts, Function<Throwable, Action<Unit>> defaultHandler, Action<A> initAction) {
        this.rts = rts;
        this.defaultHandler = defaultHandler;
        this.currentAction = (Action<Object>) initAction;
    }


    public void run() {

        while (currentAction != null) {
            switch (currentAction.tag()) {
                case pure: {
                    var ca = currentAction.<Pure<Object>>as();
                    if (cont != null) {
                        currentAction = cont.apply(Try.value(ca.value));
                        cont = null;
                    } else
                        currentAction = null;
                    break;
                }
                case bind: {
                    var ca = currentAction.<Action.Bind<Object, Object>>as();
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
                    var ca = currentAction.<Action.Fail<Object>>as();
                    if (cont == null) {
                        currentAction = null;
                        defaultHandler.apply(ca.t);
                    } else {
                        currentAction = cont.apply(Try.fail(ca.t));
                        cont = null;
                    }
                    break;
                }
                case effect: {
                    var ca = currentAction.<Action.Effect<Object>>as();
                    Try<Object> objectTry = Try.call(ca.block);
                    if (cont != null) {
                        currentAction = cont.apply(objectTry);
                        cont = null;
                    } else {
                        currentAction = null;
                        //Done...
                    }
                    break;
                }
                case fork: {
                    var ca = currentAction.<Action.Fork<A>>as();
                    rts.submit(new ActionRunner<>(rts, defaultHandler, ca.left));
                    rts.submit(new ActionRunner<>(rts, defaultHandler, ca.right));
                    currentAction = null;
                    cont = null;
                    break;
                }
                //TODO nasty!
                case par: {
                    var ca = currentAction.<Action.Par<Object, Object, Object>>as();
                    var leftRunner = new ActionRunner<>(rts, defaultHandler, ca.left);
                    var rightRunner = new ActionRunner<>(rts, defaultHandler, ca.right);
                    var join = new Join<>(ca.handler, leftRunner, rightRunner, rts, Optional.ofNullable(cont));
                    rts.submit(leftRunner.setCurrentAction(ca.left.bind(o -> Action.run(() -> join.left(o)))));
                    rts.submit(rightRunner.setCurrentAction(ca.right.bind(o -> Action.run(() -> join.right(o)))));

                    currentAction = null;
                    cont = null;
                }

            }

        }
    }

    public <B> ActionRunner<B> setCurrentAction(Action<B> currentAction) {
        this.currentAction = (Action<Object>) currentAction;
        return (ActionRunner<B>) this;
    }


    @Override
    public Action<Unit> interrupt() {
        return null;
    }

    @Override
    public Action<A> attach() {
        return null;
    }
}
