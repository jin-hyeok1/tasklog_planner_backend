CREATE TABLE app_user
(
    email                   varchar(255) PRIMARY KEY,

    user_code               varchar(100) NOT NULL UNIQUE,
    username                varchar(100) NOT NULL UNIQUE,
    display_name            varchar(100) NOT NULL,
    password                varchar(255) NOT NULL,

    role                    user_role NOT NULL DEFAULT 'USER',
    timezone                varchar(50) NOT NULL DEFAULT 'Asia/Seoul',

    weekly_target_minutes   integer NOT NULL DEFAULT 3600,
    daily_start_time        time,
    daily_end_time          time,

    notification_enabled    boolean NOT NULL DEFAULT true,

    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now()
);