CREATE INDEX idx_audit_event_event_type ON audit_event (event_type);
CREATE INDEX idx_audit_event_actor_user_id ON audit_event (actor_user_id);
CREATE INDEX idx_audit_event_occurred_at ON audit_event (occurred_at);
