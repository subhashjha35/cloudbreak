-- // CDPCP-1533 Add columns for storing operations in
-- //            user sync status
-- Migration SQL that makes the change goes here.

ALTER TABLE usersyncstatus
    ADD COLUMN IF NOT EXISTS lastrequestedfullsync_id bigint
        CONSTRAINT fk_usersyncstatus_lastrequestedoperationid
        REFERENCES operation,
    ADD COLUMN IF NOT EXISTS lastsuccessfulfullsync_id bigint
        CONSTRAINT fk_usersyncstatus_lastsuccessfuloperationid
        REFERENCES operation;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE usersyncstatus
    DROP COLUMN IF EXISTS lastrequestedfullsync_id,
    DROP COLUMN IF EXISTS lastsuccessfulfullsync_id;