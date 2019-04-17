package org.kantega.niagara.data

import io.vavr.collection.TreeMap

fun <K,V> TreeMap<K,V>.update(key:K, f:(V)->V, defaultValue:()->V):TreeMap<K,V> =
  this.computeIfPresent(key,{_,v->f(v)})._2.computeIfAbsent(key,{ defaultValue()})._2