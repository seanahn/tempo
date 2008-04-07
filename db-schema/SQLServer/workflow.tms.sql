CREATE TABLE OPENJPA_SEQUENCE_TABLE (ID TINYINT NOT NULL, SEQUENCE_VALUE BIGINT, PRIMARY KEY (ID));
CREATE TABLE tempo_acl (id BIGINT NOT NULL, action VARCHAR(255), DTYPE VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE tempo_acl_map (TASK_ID BIGINT, ELEMENT_ID BIGINT);
CREATE TABLE tempo_attachment (id BIGINT NOT NULL, payload_url VARCHAR(255), METADATA_ID BIGINT, PRIMARY KEY (id));
CREATE TABLE tempo_attachment_map (PATASK_ID BIGINT, ELEMENT_ID BIGINT);
CREATE TABLE tempo_attachment_meta (id BIGINT NOT NULL, creation_date DATETIME, description VARCHAR(255), file_name VARCHAR(255), mime_type VARCHAR(255), title VARCHAR(255), widget VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE tempo_notification (id BIGINT NOT NULL, failure_code VARCHAR(255), failure_reason VARCHAR(255), input_xml TEXT, priority INT, state SMALLINT, PRIMARY KEY (id));
CREATE TABLE tempo_pa (id BIGINT NOT NULL, complete_soap_action VARCHAR(255), deadline DATETIME, failure_code VARCHAR(255), failure_reason VARCHAR(255), input_xml TEXT, is_chained_before BIT, output_xml TEXT, previous_task_id VARCHAR(255), priority INT, process_id VARCHAR(255), state SMALLINT, PRIMARY KEY (id));
CREATE TABLE tempo_pipa (id BIGINT NOT NULL, init_message VARCHAR(255), init_soap VARCHAR(255), process_endpoint VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE tempo_role (ACL_ID BIGINT, element VARCHAR(255), TASK_ID BIGINT);
CREATE TABLE tempo_task (id BIGINT NOT NULL, creation_date DATETIME, description VARCHAR(255), form_url VARCHAR(255), tid VARCHAR(255), internal_id INT, PRIMARY KEY (id));
CREATE TABLE tempo_user (ACL_ID BIGINT, element VARCHAR(255), TASK_ID BIGINT);
CREATE INDEX I_TMPO_CL_DTYPE ON tempo_acl (DTYPE);
CREATE INDEX I_TMP__MP_ELEMENT ON tempo_acl_map (ELEMENT_ID);
CREATE INDEX I_TMP__MP_TASK_ID ON tempo_acl_map (TASK_ID);
CREATE INDEX I_TMP_MNT_METADATA ON tempo_attachment (METADATA_ID);
CREATE INDEX I_TMP__MP_ELEMENT1 ON tempo_attachment_map (ELEMENT_ID);
CREATE INDEX I_TMP__MP_PATASK_ID ON tempo_attachment_map (PATASK_ID);
CREATE INDEX I_TMP_ROL_ACL_ID ON tempo_role (ACL_ID);
CREATE INDEX I_TMP_ROL_TASK_ID ON tempo_role (TASK_ID);
CREATE INDEX I_TMP_USR_ACL_ID ON tempo_user (ACL_ID);
CREATE INDEX I_TMP_USR_TASK_ID ON tempo_user (TASK_ID);
