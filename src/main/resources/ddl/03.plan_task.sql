-- ========================================
-- PLAN
-- ========================================

CREATE TABLE plan
(
    plan_id             bigserial PRIMARY KEY,

    user_email          varchar(255) NOT NULL,
    parent_plan_id      bigint,

    plan_code           varchar(100) NOT NULL UNIQUE,
    plan_type           plan_type NOT NULL,

    title               varchar(200) NOT NULL,
    start_date          date NOT NULL,
    end_date            date NOT NULL,

    status              plan_status NOT NULL DEFAULT 'ACTIVE',
    description         text,

    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_plan_user
        FOREIGN KEY (user_email)
            REFERENCES app_user(email),

    CONSTRAINT fk_plan_parent
        FOREIGN KEY (parent_plan_id)
            REFERENCES plan(plan_id),

    CONSTRAINT uq_plan_natural
        UNIQUE (user_email, plan_type, start_date),

    CONSTRAINT chk_plan_date
        CHECK (start_date <= end_date)
);
-- ========================================
-- TASK CATEGORY
-- ========================================

CREATE TABLE task_category
(
    category_id             bigserial PRIMARY KEY,

    user_email              varchar(255) NOT NULL,
    parent_category_id      bigint,

    category_code           varchar(100) NOT NULL UNIQUE,
    category_name           varchar(100) NOT NULL,
    category_level          smallint NOT NULL,

    sort_order              integer NOT NULL DEFAULT 0,
    enabled                 boolean NOT NULL DEFAULT true,

    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_task_category_user
        FOREIGN KEY (user_email)
            REFERENCES app_user(email),

    CONSTRAINT fk_task_category_parent
        FOREIGN KEY (parent_category_id)
            REFERENCES task_category(category_id),

    CONSTRAINT uq_task_category_name
        UNIQUE (user_email, parent_category_id, category_name),

    CONSTRAINT chk_task_category_level
        CHECK (category_level BETWEEN 1 AND 3)
);
-- ========================================
-- TASK
-- ========================================

CREATE TABLE task
(
    task_id             bigserial PRIMARY KEY,

    user_email          varchar(255) NOT NULL,
    category_id         bigint NOT NULL,

    task_code           varchar(150) NOT NULL UNIQUE,

    title               varchar(200) NOT NULL,
    description         text,

    planned_minutes     integer NOT NULL DEFAULT 0,

    start_date          date,
    due_date            date,

    status              task_status NOT NULL DEFAULT 'TODO',
    priority            task_priority NOT NULL DEFAULT 'MEDIUM',
    is_task             boolean NOT NULL DEFAULT true,

    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_task_user
        FOREIGN KEY (user_email)
            REFERENCES app_user(email),

    CONSTRAINT fk_task_category
        FOREIGN KEY (category_id)
            REFERENCES task_category(category_id),

    CONSTRAINT chk_task_date
        CHECK (start_date IS NULL OR due_date IS NULL OR start_date <= due_date),

    CONSTRAINT chk_task_planned_minutes
        CHECK (planned_minutes >= 0)
);
-- ========================================
-- TASK SCHEDULE
-- ========================================

CREATE TABLE task_schedule
(
    schedule_id         bigserial PRIMARY KEY,

    user_email          varchar(255) NOT NULL,
    task_id             bigint NOT NULL,
    plan_id             bigint NOT NULL,

    schedule_code       varchar(150) NOT NULL UNIQUE,

    scheduled_date      date NOT NULL,
    start_time          time NOT NULL,
    end_time            time NOT NULL,

    planned_minutes     integer NOT NULL,

    memo                text,

    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_task_schedule_user
        FOREIGN KEY (user_email)
            REFERENCES app_user(email),

    CONSTRAINT fk_task_schedule_task
        FOREIGN KEY (task_id)
            REFERENCES task(task_id),

    CONSTRAINT fk_task_schedule_plan
        FOREIGN KEY (plan_id)
            REFERENCES plan(plan_id),

    CONSTRAINT chk_task_schedule_time
        CHECK (start_time < end_time),

    CONSTRAINT chk_task_schedule_planned_minutes
        CHECK (planned_minutes > 0)
);
-- ========================================
-- TASK SESSION
-- ========================================

CREATE TABLE task_session
(
    session_id              bigserial PRIMARY KEY,

    user_email              varchar(255) NOT NULL,
    task_id                 bigint NOT NULL,
    schedule_id             bigint,

    session_code            varchar(150) NOT NULL UNIQUE,

    worked_date             date NOT NULL,

    status                  task_session_status NOT NULL,

    started_at              timestamptz NOT NULL,
    last_started_at         timestamptz,
    paused_at               timestamptz,
    finished_at             timestamptz,

    accumulated_minutes     integer NOT NULL DEFAULT 0,

    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_task_session_user
        FOREIGN KEY (user_email)
            REFERENCES app_user(email),

    CONSTRAINT fk_task_session_task
        FOREIGN KEY (task_id)
            REFERENCES task(task_id),

    CONSTRAINT fk_task_session_schedule
        FOREIGN KEY (schedule_id)
            REFERENCES task_schedule(schedule_id),

    CONSTRAINT chk_task_session_accumulated_minutes
        CHECK (accumulated_minutes >= 0)
);

-- 사용자당 RUNNING 세션은 최대 1개
CREATE UNIQUE INDEX uq_task_session_running_per_user
    ON task_session(user_email)
    WHERE status = 'RUNNING';
-- ========================================
-- WORK LOG
-- ========================================

CREATE TABLE work_log
(
    task_id             bigint NOT NULL,
    worked_date         date NOT NULL,
    sequence_no         integer NOT NULL,

    user_email          varchar(255) NOT NULL,
    schedule_id         bigint,

    started_at          timestamptz NOT NULL,
    ended_at            timestamptz NOT NULL,
    worked_minutes      integer NOT NULL,

    memo                text,
    source              work_log_source NOT NULL,

    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    PRIMARY KEY (task_id, worked_date, sequence_no),

    CONSTRAINT fk_work_log_user
        FOREIGN KEY (user_email)
            REFERENCES app_user(email),

    CONSTRAINT fk_work_log_task
        FOREIGN KEY (task_id)
            REFERENCES task(task_id),

    CONSTRAINT fk_work_log_schedule
        FOREIGN KEY (schedule_id)
            REFERENCES task_schedule(schedule_id),

    CONSTRAINT chk_work_log_time
        CHECK (started_at < ended_at),

    CONSTRAINT chk_work_log_minutes
        CHECK (worked_minutes > 0)
);
-- ========================================
-- NOTIFICATION
-- ========================================

CREATE TABLE notification
(
    notification_id       bigserial PRIMARY KEY,

    user_email            varchar(255) NOT NULL,

    notification_code     varchar(150) NOT NULL UNIQUE,

    title                 varchar(200) NOT NULL,
    message               text NOT NULL,
    notification_type     notification_type NOT NULL,

    read_yn               boolean NOT NULL DEFAULT false,

    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_email)
            REFERENCES app_user(email)
);
