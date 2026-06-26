CREATE TYPE user_role AS ENUM (
    'ADMIN',
    'USER'
);

CREATE TYPE plan_type AS ENUM (
    'YEAR',
    'QUARTER',
    'MONTH',
    'WEEK'
);

CREATE TYPE plan_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'COMPLETED',
    'CANCELLED'
);

CREATE TYPE task_status AS ENUM (
    'TODO',
    'IN_PROGRESS',
    'DONE',
    'HOLD',
    'DELAYED'
);

CREATE TYPE task_priority AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'URGENT'
);

CREATE TYPE task_session_status AS ENUM (
    'RUNNING',
    'PAUSED',
    'FINISHED'
);

CREATE TYPE work_log_source AS ENUM (
    'MANUAL',
    'SESSION'
);

CREATE TYPE notification_type AS ENUM (
    'INFO',
    'WARNING',
    'DANGER'
);