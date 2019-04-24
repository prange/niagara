package org.kantega.niagara

import io.vavr.collection.List
import io.vavr.control.Option
import no.supercal.eff.Task
import org.jctools.queues.MessagePassingQueue
import org.kantega.niagara.data.P2
import io.vavr.kotlin.*
import org.kantega.niagara.data.andThen
import org.kantega.niagara.data.p
import java.util.concurrent.atomic.AtomicBoolean

typealias Sink<A> = (A) -> Task<Unit>




interface Source<A> {

    fun step(): Task<P2<List<A>, Source<A>>>

    fun compile(): Task<Unit> =
      compile(Unit, { _, _ -> Unit })

    fun <S> compile(init: S, f: (S, List<A>) -> S): Task<S> =
      step().bind { (output, next) ->
          when {
              next.isDone()  -> Task.just(f(init, output))
              output.isEmpty -> {
                  Thread.sleep(1)
                  next.compile(f(init, output), f)
              }
              else           -> next.compile(f(init, output), f)
          }
      }


    fun bind(f: (Source<A>) -> Source<A>): Source<A> =
      BoundSource(this, f)

    fun <B> map(f: (A) -> B): Source<B> =
      MappedSource(this, f)

    fun <B> eval(f: (A) -> Task<B>): Source<B> =
      EvalSource(this, f)

    fun append(f: () -> Source<A>): Source<A> =
      AppendSource(this, f)

    fun <B> into(sink: (A)-> Task<B>): Source<B> =
      ExecutingSource(this, sink)

    fun <B> flatMap(f: (A) -> Iterable<B>): Source<B> =
      FlatMappedSource(this, f)

    fun <S, B> zipWithState(init: S, f: (S, A) -> P2<S, B>): Source<P2<S, B>> =
      FoldingSource(this, init, f)

    fun <S, B> mapWithState(init: S, f: (S, A) -> P2<S, B>): Source<B> =
      FoldingSource(this, init, f).map { it._2 }

    fun <S, B> accumulate(init: S, f: (S, A) -> S): Source<S> =
      FoldingSource(this, init, { s, a -> p(f(s, a), a) }).map { it._1 }

    fun merge(other: Source<A>): Source<A> =
      merge(this, other)

    fun keep(pred:(A)->Boolean) : Source<A> =
      flatMap { a -> if(pred(a)) Option.some(a) else Option.none() }

    fun drop(pred:(A)->Boolean) : Source<A> =
      flatMap { a -> if(pred(a)) Option.none() else Option.some(a)}

    fun takeWhile(pred:(A)->Boolean) :Source<A> =
      TakeWhileSource(this,pred)

    companion object {

        fun <A> merge(one: Source<A>, other: Source<A>): Source<A> =
          when {
              one.isDone()   -> other
              other.isDone() -> one
              else           -> one.bind { l -> other.bind { r -> merge(l, r) } }
          }

        fun <A> nil(): Source<A> =
          Done()

        fun <A> output1(a: A): Source<A> =
          Output1(a)

        fun <A> output(vararg aas: A): Source<A> =
          IteratorSource(aas.iterator())

        fun <A> iterable(i: Iterable<A>): Source<A> =
          iterator(i.iterator())

        fun <A> iterator(i: Iterator<A>): Source<A> =
          IteratorSource(i.iterator())

        fun <A> queue(q: MessagePassingQueue<A>): Source<A> =
          queue(q, 10)

        fun <A> queue(q: MessagePassingQueue<A>, batch: Int): Source<A> =
          QueueSource(q, batch)
    }
}

fun <A> Source<A>.isDone() =
  this is Done<A>

class Done<A> : Source<A> {
    override fun step(): Task<P2<List<A>, Source<A>>> =
      Task.just(p(List.empty<A>(), Source.nil()))

    override fun <B> map(f: (A) -> B) =
      Done<B>()
}

class BoundSource<A>(val source: Source<A>, val f: (Source<A>) -> Source<A>) : Source<A> {
    override fun step(): Task<P2<List<A>, Source<A>>> =
      source.step().map { (output, next) ->
          p(output, f(next))
      }
}

data class EvalSource<A, B>(val source: Source<A>, val f: (A) -> Task<B>) : Source<B> {
    override fun step(): Task<P2<List<B>, Source<B>>> =
      source.step().bind { (outputa, next) ->
          outputa.map(f).sequence().map { outputb -> p(outputb, next.eval(f)) }
      }

}

data class QueueSource<A>(val queue: MessagePassingQueue<A>, val max: Int) : Source<A> {
    override fun step(): Task<P2<List<A>, Source<A>>> =
      Task.exec {
          var n = 0
          var aas = List.empty<A>()
          var running = true
          while (running && n < max) {
              val a = queue.relaxedPoll()
              if (a != null)
                  aas = aas.prepend(a)
              else
                  running = false
              n++
          }

          p(aas.reverse(), Source.queue(queue, max))
      }
}

data class IteratorSource<A>(val iter: Iterator<A>) : Source<A> {
    override fun step(): Task<P2<List<A>, Source<A>>> =
      Task.exec {
          var aas = List.empty<A>()
          while (iter.hasNext()) {
              val a = iter.next()

              aas = aas.prepend(a)
          }

          p(aas.reverse(), Source.nil<A>())
      }
}

data class Output1<A>(val a: A) : Source<A> {
    override fun step(): Task<P2<List<A>, Source<A>>> =
      Task.exec { p(List.of(a), Source.nil<A>()) }

}

data class AppendSource<A>(val first: Source<A>, val appended: () -> Source<A>) : Source<A> {
    override fun step(): Task<P2<List<A>, Source<A>>> =
      first.step().map { (aas, next) ->
          if (next.isDone())
              p(aas, appended())
          else
              p(aas, next.append(appended))
      }
}

data class ExecutingSource<A,B>(val wrapped: Source<A>, val sink: (A)-> Task<B>) : Source<B> {
    override fun step(): Task<P2<List<B>, Source<B>>> =
      wrapped.step().bind { (outputa, next) ->
          outputa.map(sink).sequence().map { p(it, next.into(sink)) }
      }
}

data class MappedSource<A, B>(val wrapped: Source<A>, val ff: (A) -> B) : Source<B> {
    override fun step(): Task<P2<List<B>, Source<B>>> =
      wrapped.step().map { (list, next) -> p(list.map(ff), next.map(ff)) }


    override fun <C> map(f: (B) -> C): Source<C> {
        return MappedSource(wrapped, ff andThen f)
    }

}

data class FlatMappedSource<A, B>(val wrapped: Source<A>, val f: (A) -> Iterable<B>) : Source<B> {
    override fun step(): Task<P2<List<B>, Source<B>>> =
      wrapped
        .step()
        .map { (outputa, next) -> p(outputa.flatMap { a -> List.ofAll(f(a)) }, next.flatMap(f)) }
}

data class FoldingSource<S, A, B>(val wrapped: Source<A>, val init: S, val f: (S, A) -> P2<S, B>) : Source<P2<S, B>> {
    override fun step(): Task<P2<List<P2<S, B>>, Source<P2<S, B>>>> =
      wrapped.step().map { (aas, wrappedNext) ->
          val output = aas.fold(
            p(init, List.empty<P2<S, B>>()),
            { (s, bs), a ->
                val (sNext, bNext) = f(s, a)
                p(sNext, bs.prepend(p(sNext, bNext)))


            })
          p(output._2, FoldingSource(wrappedNext, output._1, f) as Source<P2<S, B>>)
      }
}

data class TakeWhileSource<A>(val wrapped:Source<A>,val closeSignal:(A)->Boolean):Source<A>{
    override fun step(): Task<P2<List<A>, Source<A>>> =
      wrapped.step().map{(aas,wrappedNext)->
          if(aas.exists(closeSignal)){
              val aasUntil = aas.takeUntil(closeSignal)
              p(aasUntil,Done())
          }else{
              p(aas,wrappedNext)
          }
      }

}

fun <A> Source<Task<A>>.execute(): Source<A> =
  ExecutingSource(this,{t->t})

fun <A> Source<out Iterable<A>>.flatten(): Source<A> =
  FlatMappedSource(this,{i->i})


