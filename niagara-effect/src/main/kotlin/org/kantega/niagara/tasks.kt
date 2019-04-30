package org.kantega.niagara

import io.vavr.collection.List
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

fun <A> runTask(task: Task<A>): Unit =
  runTask(task, Executors.newSingleThreadScheduledExecutor())

fun <A> runTask(task: Task<A>, executor: ScheduledExecutorService): Unit {
    executor.execute { task.execute(executor, { result -> result.onFailure { it.printStackTrace() } }) }
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