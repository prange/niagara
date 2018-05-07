package org.kantega.niagara.task;

import fj.Unit;
import org.kantega.niagara.Try;
import org.kantega.niagara.task.Action.Pure;

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

    public void setCurrentAction(Action<Object> currentAction) {
        this.currentAction = currentAction;
    }

    public void run() {

        while (currentAction != null) {
            switch (currentAction.tag()) {
                case pure: {
                    var ca = currentAction.<Pure<Object>>as();
                    if (cont != null) {
                        currentAction = cont.apply(Try.value(ca.value));
                        cont = null;
                    }
                    else
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
                    rts.submit(new ActionRunner<>(rts, ca.handler.orElse(defaultHandler), ca.forked));
                    break;
                }

            }

        }
    }


}
