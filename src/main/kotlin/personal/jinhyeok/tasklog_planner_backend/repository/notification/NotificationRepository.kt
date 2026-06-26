package personal.jinhyeok.tasklog_planner_backend.repository.notification

import personal.jinhyeok.jooq.Tables.NOTIFICATION
import personal.jinhyeok.jooq.tables.records.NotificationRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class NotificationRepository(private val dsl: DSLContext) {
    fun findAll(userEmail: String, readYn: Boolean?, limit: Int?): List<NotificationRecord> {
        val query = dsl.selectFrom(NOTIFICATION)
            .where(NOTIFICATION.USER_EMAIL.eq(userEmail))
            .let { if (readYn == null) it else it.and(NOTIFICATION.READ_YN.eq(readYn)) }
            .orderBy(NOTIFICATION.CREATED_AT.desc())
        return if (limit == null) query.fetch() else query.limit(limit).fetch()
    }

    fun unreadCount(userEmail: String): Int =
        dsl.fetchCount(NOTIFICATION, NOTIFICATION.USER_EMAIL.eq(userEmail).and(NOTIFICATION.READ_YN.isFalse))

    fun unreadCountBetween(userEmail: String, start: OffsetDateTime, end: OffsetDateTime): Int =
        dsl.fetchCount(
            NOTIFICATION,
            NOTIFICATION.USER_EMAIL.eq(userEmail)
                .and(NOTIFICATION.READ_YN.isFalse)
                .and(NOTIFICATION.CREATED_AT.ge(start))
                .and(NOTIFICATION.CREATED_AT.lt(end)),
        )
}
