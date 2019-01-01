package org.kantega.niagara.http.broker

import arrow.syntax.function.curried
import org.kantega.niagara.eventsourced.ConsumerRecord
import org.kantega.niagara.eventsourced.Journal
import org.kantega.niagara.eventsourced.ProducerRecord
import org.kantega.niagara.eventsourced.TopicName
import org.kantega.niagara.http.*
import org.kantega.niagara.http.websocket.WebsocketUpgradeEndpoint
import org.kantega.niagara.json.*
import org.kantega.niagara.json.io.JsonParser
import org.kantega.niagara.json.io.JsonWriter
import org.kantega.niagara.task.Task
import org.kantega.niagara.task.TaskExecutor

object Broker {

    val producerRecordDecoder =
      decode(::ProducerRecord.curried())
        .field("topic", decodeString.map(::TopicName))
        .field("msg", decodeString)

    val consumerRecordDecoder =
      decode(::ConsumerRecord.curried())
        .field("id", decodeString)
        .field("offset", decodeLong)
        .field("topic", decodeString.map(::TopicName))
        .field("msg", decodeString)

    val producerRecordEncoder = { rec: ProducerRecord ->
        JsonObject()
          .set("topic", JsonString(rec.topic.name))
          .set("msg", JsonString(rec.msg))
    }

    val consumerRecordEncoder = { rec: ConsumerRecord ->
        JsonObject()
          .set("id", JsonString(rec.id))
          .set("offset", JsonNumber(rec.offset))
          .set("topic", JsonString(rec.topic.name))
          .set("msg", JsonString(rec.msg))
    }



}


