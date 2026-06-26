package personal.jinhyeok.tasklog_planner_backend.repository.plan

import personal.jinhyeok.jooq.Tables.PLAN
import personal.jinhyeok.jooq.enums.PlanType as JPlanType
import personal.jinhyeok.jooq.tables.records.PlanRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException
import java.time.LocalDate

@Repository
class PlanRepository(private val dsl: DSLContext) {
    fun findByCode(userEmail: String, planCode: String): PlanRecord =
        dsl.selectFrom(PLAN).where(PLAN.USER_EMAIL.eq(userEmail)).and(PLAN.PLAN_CODE.eq(planCode)).fetchOne()
            ?: throw ApiException(ApiCode.NOT_FOUND, "Plan not found: $planCode")

    fun findById(planId: Long?): PlanRecord? =
        planId?.let { dsl.selectFrom(PLAN).where(PLAN.PLAN_ID.eq(it)).fetchOne() }

    fun findById(userEmail: String, planId: Long): PlanRecord =
        dsl.selectFrom(PLAN).where(PLAN.USER_EMAIL.eq(userEmail)).and(PLAN.PLAN_ID.eq(planId)).fetchOne()
            ?: throw ApiException(ApiCode.NOT_FOUND, "Plan not found: $planId")

    fun findByIds(planIds: Collection<Long>): List<PlanRecord> {
        if (planIds.isEmpty()) return emptyList()
        return dsl.selectFrom(PLAN).where(PLAN.PLAN_ID.`in`(planIds)).fetch()
    }

    fun findTreeCandidates(userEmail: String, year: Int?): List<PlanRecord> =
        dsl.selectFrom(PLAN)
            .where(PLAN.USER_EMAIL.eq(userEmail))
            .let { if (year == null) it else it.and(overlapsYear(year)) }
            .orderBy(PLAN.START_DATE.asc(), PLAN.PLAN_TYPE.asc())
            .fetch()

    fun findAll(userEmail: String, condition: Condition): List<PlanRecord> =
        dsl.selectFrom(PLAN).where(PLAN.USER_EMAIL.eq(userEmail)).and(condition).orderBy(PLAN.START_DATE.asc(), PLAN.PLAN_CODE.asc()).fetch()

    fun findByTypeBetween(userEmail: String, planType: JPlanType, start: LocalDate, end: LocalDate): List<PlanRecord> =
        dsl.selectFrom(PLAN)
            .where(PLAN.USER_EMAIL.eq(userEmail))
            .and(PLAN.PLAN_TYPE.eq(planType))
            .and(PLAN.START_DATE.le(end))
            .and(PLAN.END_DATE.ge(start))
            .orderBy(PLAN.START_DATE.asc(), PLAN.PLAN_CODE.asc())
            .fetch()

    fun findYearPlan(userEmail: String, year: Int): PlanRecord? {
        val yearStart = LocalDate.of(year, 1, 1)
        val yearEnd = LocalDate.of(year, 12, 31)
        return dsl.selectFrom(PLAN)
            .where(PLAN.USER_EMAIL.eq(userEmail))
            .and(PLAN.PLAN_TYPE.eq(JPlanType.YEAR))
            .and(PLAN.START_DATE.between(yearStart, yearEnd))
            .fetchOne()
    }

    fun idByCode(userEmail: String, planCode: String): Long = findByCode(userEmail, planCode).get(PLAN.PLAN_ID)

    fun descendantIds(parentId: Long): List<Long> =
        dsl.selectFrom(PLAN).where(PLAN.PARENT_PLAN_ID.eq(parentId)).fetch().flatMap {
            descendantIds(it.get(PLAN.PLAN_ID)) + it.get(PLAN.PLAN_ID)
        }

    fun deleteByIds(ids: List<Long>): List<String> {
        val codes = dsl.select(PLAN.PLAN_CODE).from(PLAN).where(PLAN.PLAN_ID.`in`(ids)).fetch(PLAN.PLAN_CODE)
        dsl.deleteFrom(PLAN).where(PLAN.PLAN_ID.`in`(ids)).execute()
        return codes
    }

    fun overlapsYear(year: Int): Condition =
        PLAN.START_DATE.le(LocalDate.of(year, 12, 31)).and(PLAN.END_DATE.ge(LocalDate.of(year, 1, 1)))

    fun insert(command: PlanSaveCommand): PlanRecord =
        dsl.insertInto(PLAN)
            .set(PLAN.USER_EMAIL, command.userEmail)
            .set(PLAN.PARENT_PLAN_ID, command.parentPlanId)
            .set(PLAN.PLAN_CODE, command.planCode)
            .set(PLAN.PLAN_TYPE, command.planType)
            .set(PLAN.TITLE, command.title)
            .set(PLAN.START_DATE, command.startDate)
            .set(PLAN.END_DATE, command.endDate)
            .set(PLAN.STATUS, command.status)
            .set(PLAN.DESCRIPTION, command.description)
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.INTERNAL_ERROR, "Failed to create plan")

    fun update(planCode: String, command: PlanUpdateCommand): PlanRecord =
        dsl.update(PLAN)
            .set(PLAN.PARENT_PLAN_ID, command.parentPlanId)
            .set(PLAN.PLAN_TYPE, command.planType)
            .set(PLAN.TITLE, command.title)
            .set(PLAN.START_DATE, command.startDate)
            .set(PLAN.END_DATE, command.endDate)
            .set(PLAN.STATUS, command.status)
            .set(PLAN.DESCRIPTION, command.description)
            .set(PLAN.UPDATED_AT, command.updatedAt)
            .where(PLAN.PLAN_CODE.eq(planCode))
            .and(PLAN.USER_EMAIL.eq(command.userEmail))
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.NOT_FOUND, "Plan not found: $planCode")
}

data class PlanSaveCommand(
    val userEmail: String,
    val parentPlanId: Long?,
    val planCode: String,
    val planType: JPlanType,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: personal.jinhyeok.jooq.enums.PlanStatus,
    val description: String?,
)

data class PlanUpdateCommand(
    val userEmail: String,
    val parentPlanId: Long?,
    val planType: JPlanType,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: personal.jinhyeok.jooq.enums.PlanStatus,
    val description: String?,
    val updatedAt: java.time.OffsetDateTime,
)
