
package org.kantega.niagara.http

data class MediaType(val type: String)

val MEDIA_WILDCARD = MediaType("*")
val APPLICATION_XML = MediaType("application/xml")
val APPLICATION_ATOM_XML = MediaType("application/atom+xml")
val APPLICATION_XHTML_XML = MediaType("application/xhtml+xml")
val APPLICATION_SVG_XML = MediaType("application/svg+xml")
val APPLICATION_JSON = MediaType("application/json")
val APPLICATION_FORM_URLENCODED = MediaType("application/x-www-form-urlencoded")
val MULTIPART_FORM_DATA = MediaType("multipart/form-data")
val APPLICATION_OCTET_STREAM = MediaType("application/octet-stream")
val TEXT_PLAIN = MediaType("text/plain")
val TEXT_XML = MediaType("text/xml")
val TEXT_HTML = MediaType("text/html")
val SERVER_SENT_EVENTS = MediaType("text/event-stream")
val APPLICATION_JSON_PATCH_JSON = MediaType("application/json-patch+json")