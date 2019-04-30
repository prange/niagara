package org.kantega.niagara.data

fun String.appendIfMissing(chars:CharSequence) =
  if(this.endsWith(chars)) this else this + chars