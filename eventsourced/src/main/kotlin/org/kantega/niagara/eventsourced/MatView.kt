package org.kantega.niagara.eventsourced

import arrow.core.Option
import arrow.core.toOption
import fj.data.List
import org.kantega.niagara.task.Task
import org.kantega.niagara.task.sequence
import org.kantega.niagara.typeclasses.Equal
import java.util.concurrent.CopyOnWriteArrayList


typealias Consumer<A> = (Option<A>) -> Task<Unit>

interface Updateable<K, A> {
    fun update(key: K, value: A): Task<Unit>
    fun remove(key: K): Task<Unit>
    fun get(k: K): Option<A>
}

data class Cancel(val cancellingtask: Task<Unit>)

data class MapUpdateable<K, A>(val map: MutableMap<K, A>) : Updateable<K, A> {
    override fun update(key: K, value: A): Task<Unit> =
            Task { map[key] = value }

    override fun get(k: K): Option<A> =
            map[k].toOption()

    override fun remove(key: K): Task<Unit> =
            Task { map.remove(key);Unit }
}

data class MaterializedView<U, S>(val accum: (Option<S>, U) -> Option<S>, private val store: Updateable<String, S>) {
    val consumers = CopyOnWriteArrayList<Pair<String, Consumer<S>>>()

    fun offer(update: Pair<String, U>): Task<Unit> =
            Task.bind(
                    { Task { accum(store.get(update.first), update.second) } },
                    { maybeUpdated ->
                        maybeUpdated.fold(
                                { store.remove(update.first) },
                                { updated -> store.update(update.first, updated) })
                    },
                    { maybeUpdated, _ ->
                        List.iterableList(consumers)
                                .filter { (key, _) -> Equal.stringEq(key, update.first) }
                                .map { (_, consumer) -> consumer(maybeUpdated) }
                                .sequence()
                                .toUnit()
                    }
            )


    fun subscribe(key: String, s: Consumer<S>): Task<Cancel> =
            Task {
                val pair = Pair(key, s)
                consumers.add(pair)
                pair
            } map { pair -> Cancel(Task { consumers.remove(pair) }.toUnit()) }

}