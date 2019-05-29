package org.kantega.niagara.data

import io.vavr.control.Option



fun <A, B, C> bind(o1: Option<A>, o2: Option<B>, f: (A, B) -> Option<C>) =
  o1.flatMap { v1 -> o2.map({ v2 -> f(v1, v2) }) }