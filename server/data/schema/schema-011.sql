
CREATE TABLE cron (
    job_id BIGSERIAL NOT NULL,
    class_name TEXT NOT NULL,
    params TEXT NOT NULL,
    schedule TEXT NOT NULL,
    last_run_time TIMESTAMP WITH TIME ZONE NOT NULL,
    next_run_time TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IX_cron_ids ON cron(job_id);
