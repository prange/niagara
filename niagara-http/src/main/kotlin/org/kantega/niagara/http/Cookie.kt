package org.kantega.niagara.http

import io.vavr.control.Option
import java.util.*

data class Cookie(

  val name: String,
  val value: String,
  val path: Option<String> = Option.none(),
  val domain: Option<String> = Option.none(),
  val maxAge: Option<Int> = Option.none(),
  val expires: Option<Date> = Option.none(),
  val discard: Boolean = false,
  val secure: Boolean = false,
  val httpOnly: Boolean = false,
  val version: Int = 0,
  val comment: Option<String> = Option.none(),
  val sameSite: Boolean = false,
  val sameSiteMode: SameSiteMode = SameSiteMode.None)

enum class SameSiteMode {
    Strict, Lax, None
}