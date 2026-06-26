ALTER TABLE task_schedule
    ADD COLUMN IF NOT EXISTS plan_id bigint;

ALTER TABLE task_schedule
    ALTER COLUMN plan_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_task_schedule_plan'
    ) THEN
        ALTER TABLE task_schedule
            ADD CONSTRAINT fk_task_schedule_plan
                FOREIGN KEY (plan_id)
                    REFERENCES plan(plan_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_task_schedule_plan
    ON task_schedule(plan_id);

DROP INDEX IF EXISTS idx_task_plan;

ALTER TABLE task
    DROP CONSTRAINT IF EXISTS fk_task_plan;

ALTER TABLE task
    DROP COLUMN IF EXISTS plan_id;
