CREATE INDEX idx_plan_user_type_date
    ON plan(user_email, plan_type, start_date);

CREATE INDEX idx_plan_parent
    ON plan(parent_plan_id);

CREATE INDEX idx_task_user_status
    ON task(user_email, status);

CREATE INDEX idx_task_category
    ON task(category_id);

CREATE INDEX idx_task_schedule_user_date
    ON task_schedule(user_email, scheduled_date);

CREATE INDEX idx_task_schedule_task
    ON task_schedule(task_id);

CREATE INDEX idx_task_schedule_plan
    ON task_schedule(plan_id);

CREATE INDEX idx_task_session_user_status
    ON task_session(user_email, status);

CREATE INDEX idx_task_session_task
    ON task_session(task_id);

CREATE INDEX idx_work_log_user_date
    ON work_log(user_email, worked_date);

CREATE INDEX idx_work_log_schedule
    ON work_log(schedule_id);

CREATE INDEX idx_notification_user_read
    ON notification(user_email, read_yn, created_at DESC);
