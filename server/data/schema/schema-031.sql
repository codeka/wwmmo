

CREATE TABLE api_request_client_metrics (
  request_time TIMESTAMP WITH TIME ZONE NOT NULL,
  empire_id INT NOT NULL,
  url TEXT NOT NULL,
  method TEXT NOT NULL,
  response_code INT NOT NULL,
  request_size INT NOT NULL,
  response_size INT NOT NULL,
  response_time_ms INT NOT NULL
);

CREATE INDEX IX_api_request_client_metrics_time ON api_request_client_metrics (request_time);
CREATE INDEX IX_api_request_client_metrics_empire_id ON api_request_client_metrics (empire_id, request_time);
