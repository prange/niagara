package org.kantega.niagara.eventsourced

import org.kantega.niagara.stream.Fold
import org.kantega.niagara.stream.Fold.Companion.just
import org.kantega.niagara.task.Task
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicLong

class Journal private constructor(path: Path, internal val executor: ExecutorService) {


    private val counter = AtomicLong()

    internal val journal: Connection

    init {
        try {
            Class.forName("org.h2.Driver")
        } catch (e: ClassNotFoundException) {
            throw Error(e)
        }

        val f = path.toFile()
        f.mkdirs()

        if (!f.exists() || !f.isDirectory)
            throw RuntimeException(f.absolutePath + " is not a directrory")


        try {
            journal = DriverManager.getConnection("jdbc:h2:" + f.toPath().toString())
            journal
                    .prepareStatement("create table if not exists journal(id varchar(255), counter bigint auto_increment, topic varchar(255), message clob)")
                    .execute()

            val rs = journal.prepareStatement("select max(counter) as c from journal").executeQuery()

            while (rs.next())
                counter.set(rs.getLong("c"))

        } catch (e: SQLException) {
            throw RuntimeException("Could not create journal", e)
        }

    }

    fun append(producerRecord: ProducerRecord): Task<Unit> {
        return Task {
            try {
                val ps = journal.prepareStatement("insert into journal (id,topic,message) values (?,?,?)")
                val id = UUID.randomUUID()
                ps.setString(1, id.toString())
                ps.setString(2, producerRecord.topic.name)
                ps.setString(3, producerRecord.msg)
                ps.execute()
                counter.incrementAndGet()
                Unit
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException(e)
            }
        }
    }

    fun subscribe(offset: Long): Fold<ConsumerRecord> =
            just(read(offset, 1000)) append { subscribe(offset + 1000) }


    fun subscribeToLast(): Fold<ConsumerRecord> =
            subscribe(counter.get())


    fun subscribeToTopic(offset: Long, topic: String): Fold<ConsumerRecord> =
            just(read(offset, 1000, topic)) append { subscribeToTopic(offset + 1000, topic) }


    fun subscribeToLastTopic(topic: String): Fold<ConsumerRecord> =
            subscribeToTopic(counter.get(), topic)


    private fun read(offset: Long, length: Int): Task<Iterable<ConsumerRecord>> =
            Task {
                val list = ArrayList<ConsumerRecord>()
                val ps = journal.prepareStatement("select id,counter,topic,message from journal where counter >= ? order by counter limit ?")
                ps.setLong(1, offset)
                ps.setInt(2, length)
                val rs = ps.executeQuery()
                while (rs.next()) {
                    list.add(ConsumerRecord(rs.getString("id"), rs.getLong("counter"), TopicName(rs.getString("topic")), rs.getString("message")))
                }
                list
            }


    private fun read(offset: Long, length: Int, topic: String): Task<ArrayList<ConsumerRecord>> =
            Task {
                val list = ArrayList<ConsumerRecord>()
                val ps = journal.prepareStatement("select id,counter,topic,message from journal where counter >= ? and topic like ? order by counter limit ?")
                ps.setLong(1, offset)
                ps.setInt(2, length)
                ps.setString(3, topic)
                val rs = ps.executeQuery()
                while (rs.next()) {
                    list.add(ConsumerRecord(rs.getString("id"), rs.getLong("counter"), TopicName(rs.getString("topic")), rs.getString("message")))
                }
                list
            }


    fun close(): Task<Unit> =
            Task { journal.close() }


    companion object {

        val defaultLogDir =
                Paths.get(System.getProperty("user.home") + "/niagara/data").toAbsolutePath()

        fun invoke(executor: ExecutorService): Journal =
                Journal(defaultLogDir, executor)

    }

}
