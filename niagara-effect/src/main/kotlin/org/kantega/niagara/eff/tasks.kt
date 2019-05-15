package org.kantega.niagara.eff

import io.vavr.collection.List
import io.vavr.concurrent.Future
import io.vavr.concurrent.Promise
import io.vavr.control.Option
import io.vavr.control.Try
import io.vavr.kotlin.Try
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

fun <A> runTask(task: Task<A>): Future<A> =
  runTask(task, Executors.newSingleThreadScheduledExecutor())

fun <A> runTask(task: Task<A>, executor: ScheduledExecutorService): Future<A> {
    val promise = Promise.make<A>()
    executor.execute { task.execute(executor, { result -> promise.complete(result) }) }
    return promise.future()
}


fun <A, B, C> bind(
  at: () -> Task<A>,
  ab: (A) -> Task<B>,
  ac: (A, B) -> Task<C>
): Task<C> =
  at() bind { a -> ab(a) bind { b -> ac(a, b) } }

fun <A, B, C, D> bind(
  at: () -> Task<A>,
  ab: (A) -> Task<B>,
  ac: (A, B) -> Task<C>,
  ad: (A, B, C) -> Task<D>
): Task<D> =
  at() bind { a -> ab(a) bind { b -> ac(a, b) bind { c -> ad(a, b, c) } } }

fun <A, B, C, D, E> bind(
  at: () -> Task<A>,
  ab: (A) -> Task<B>,
  ac: (A, B) -> Task<C>,
  ad: (A, B, C) -> Task<D>,
  ae: (A, B, C, D) -> Task<E>
): Task<E> =
  at() bind { a -> ab(a) bind { b -> ac(a, b) bind { c -> ad(a, b, c) bind { d -> ae(a, b, c, d) } } } }

fun <A, B, C, D, E, F> bind(
  at: () -> Task<A>,
  ab: (A) -> Task<B>,
  ac: (A, B) -> Task<C>,
  ad: (A, B, C) -> Task<D>,
  ae: (A, B, C, D) -> Task<E>,
  af: (A, B, C, D, E) -> Task<F>
): Task<F> =
  at() bind { a -> ab(a) bind { b -> ac(a, b) bind { c -> ad(a, b, c) bind { d -> ae(a, b, c, d) bind { e -> af(a, b, c, d, e) } } } } }

fun <A> List<Task<A>>.sequence(): Task<List<A>> {
    return this.foldLeft(
      Task.just(List.empty<A>()),
      { acc, task -> acc.bind { list -> task.map { a -> list.prepend(a) } } }
    )
}

fun <A, B> ((A) -> B).liftToTask(): (A) -> Task<B> = { a ->
    Task(this.invoke(a))
}

fun <A> Task<Option<A>>.flatten(f: () -> Throwable) =
  this.bind { maybeA -> Task.doTry { Try { maybeA.getOrElseThrow(f) } } }