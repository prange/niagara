package org.kantega.niagara.eventsourced

data class ConsumerRecord(val id: String, val offset: Long, val topic: TopicName, val msg: String) {

    override fun toString(): String {
        val sb = StringBuilder("ConsumerRecord{")
        sb.append("id='").append(id).append('\'')
        sb.append(", topic=").append(topic.name)
        sb.append(", offset=").append(offset)
        sb.append(", msg='").append(msg).append('\'')
        sb.append('}')
        return sb.toString()
    }
}
