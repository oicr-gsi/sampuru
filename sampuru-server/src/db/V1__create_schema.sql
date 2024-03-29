CREATE TABLE project (
    id text PRIMARY KEY,
    name varchar(10) UNIQUE NOT NULL,
    contact_name text,
    contact_email text,
    description text,
    pipeline text,
    kits text[],
    reference_genome text,
    created_date timestamp,
    completion_date timestamp);
 
CREATE TABLE donor_case (
    id text PRIMARY KEY,
    project_id text NOT NULL,
    name text NOT NULL);
 
CREATE TABLE qcable (
    id text PRIMARY KEY,
    qcable_type text NOT NULL,
    project_id text NOT NULL,
    case_id text NOT NULL,
    oicr_alias text NOT NULL,
    external_name text,
    status text NOT NULL,
    failure_reason text,
    library_design text,
    parent_id text);

-- If the writes can be condensed into one transaction, consider changing case_id to its own table (case_id, deliverable_id)
-- see https://www.postgresql.org/docs/current/arrays.html "searching for specific array elements can be a sign of
-- database misdesign"
CREATE TABLE deliverable_file (
    id SERIAL PRIMARY KEY,
    project_id text NOT NULL,
    location text NOT NULL,
    notes text,
    expiry_date timestamp);
 
CREATE TABLE changelog (
    id SERIAL PRIMARY KEY,
    project_id text NOT NULL,
    qcable_id text,
    case_id text NOT NULL,
    change_date timestamp NOT NULL,
    content text NOT NULL);
 
CREATE TABLE notification (
    id SERIAL PRIMARY KEY,
    user_id text NOT NULL,
    issue_date timestamp NOT NULL,
    resolved_date timestamp,
    content text NOT NULL);
 
CREATE TABLE project_info_item (
    id SERIAL PRIMARY KEY,
    project_id text NOT NULL,
    type text NOT NULL,
    content text,
    expected integer,
    received integer);
 
ALTER TABLE donor_case ADD CONSTRAINT case_project_id_match FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE;
ALTER TABLE qcable ADD CONSTRAINT qcable_project_id_match FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE;
ALTER TABLE qcable ADD CONSTRAINT qcable_case_id_match FOREIGN KEY (case_id) REFERENCES donor_case (id) ON DELETE CASCADE;
ALTER TABLE deliverable_file ADD CONSTRAINT deliverable_project_id_match FOREIGN KEY (project_id) REFERENCES project (id);
ALTER TABLE project_info_item ADD CONSTRAINT project_info_item_project_id_match FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE;
ALTER TABLE qcable ADD CONSTRAINT qcable_parent_id_match FOREIGN KEY (parent_id) REFERENCES qcable (id);

CREATE INDEX ON qcable(id);
CREATE INDEX ON qcable(project_id);
CREATE INDEX ON qcable(case_id);
CREATE INDEX ON qcable(parent_id);
CREATE INDEX ON donor_case(project_id);
