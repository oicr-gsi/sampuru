CREATE TABLE project (
    id SERIAL PRIMARY KEY,
    name varchar(10) UNIQUE NOT NULL,
    contact_name text,
    contact_email text,
    completion_date timestamp);
 
CREATE TABLE donor_case (
    id SERIAL PRIMARY KEY,
    project_id integer NOT NULL,
    name text UNIQUE NOT NULL);
 
CREATE TABLE qcable (
    id SERIAL PRIMARY KEY,
    qcable_type text NOT NULL,
    project_id integer NOT NULL,
    case_id integer NOT NULL,
    oicr_alias text UNIQUE NOT NULL,
    status text NOT NULL,
    failure_reason text,
    library_design text);
 
CREATE TABLE deliverable_file (
    id SERIAL PRIMARY KEY,
    project_id integer NOT NULL,
    case_id integer NOT NULL,
    content text NOT NULL,
    expiry_date timestamp);
 
CREATE TABLE changelog (
    id SERIAL PRIMARY KEY,
    qcable_id integer NOT NULL,
    case_id integer NOT NULL,
    change_date timestamp NOT NULL,
    content text NOT NULL);
 
CREATE TABLE notification (
    id SERIAL PRIMARY KEY,
    user_id integer NOT NULL,
    issue_date timestamp NOT NULL,
    resolved_date timestamp,
    content text NOT NULL);
 
CREATE TABLE project_info_item (
    id SERIAL PRIMARY KEY,
    project_id integer NOT NULL,
    type text NOT NULL,
    content text,
    expected integer,
    received integer);
 
ALTER TABLE donor_case ADD CONSTRAINT case_project_id_match FOREIGN KEY (project_id) REFERENCES project (id);
ALTER TABLE qcable ADD CONSTRAINT qcable_project_id_match FOREIGN KEY (project_id) REFERENCES project (id);
ALTER TABLE qcable ADD CONSTRAINT qcable_case_id_match FOREIGN KEY (case_id) REFERENCES donor_case (id);
ALTER TABLE deliverable_file ADD CONSTRAINT deliverable_project_id_match FOREIGN KEY (project_id) REFERENCES project (id);
ALTER TABLE deliverable_file ADD CONSTRAINT deliverable_case_id_match FOREIGN KEY (case_id) REFERENCES donor_case (id);
ALTER TABLE changelog ADD CONSTRAINT changelog_case_id_match FOREIGN KEY (case_id) REFERENCES donor_case (id);
ALTER TABLE changelog ADD CONSTRAINT changelog_qcable_id_match FOREIGN KEY (qcable_id) REFERENCES qcable (id);
ALTER TABLE project_info_item ADD CONSTRAINT project_info_item_project_id_match FOREIGN KEY (project_id) REFERENCES project (id);
