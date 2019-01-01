package org.kantega.niagara.eventsourced


class ProducerRecord(val topic: TopicName, val msg: String) {

    override fun toString(): String {
        return "ProducerRecord{" +
                "topic=" + topic +
                ", msg='" + msg + '\''.toString() +
                '}'.toString()
    }

    companion object {

        fun message(topicName: TopicName, msg: String): ProducerRecord {
            return ProducerRecord(topicName, msg)
        }

        fun toMessage(topicName: TopicName): (String)->ProducerRecord {
            return { msg -> message(topicName, msg) }
        }
    }
}
