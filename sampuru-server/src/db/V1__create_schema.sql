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
    completion_date timestamp
);

CREATE TABLE donor (
    id text PRIMARY KEY,
    oicr_alias text,
    external_name text
);

CREATE TABLE qcable (
    id text PRIMARY KEY,
    oicr_alias text,
    status text NOT NULL,
    failure_reason text
);
 
CREATE TABLE donor_case (
    id text PRIMARY KEY,
    donor_id text NOT NULL,
    assay_name text NOT NULL,
    tissue_origin text NOT NULL,
    tissue_type text NOT NULL,
    timepoint text,
    receipt_qcable_id text,
    informatics_qcable_id text,
    draft_report_qcable_id text,
    final_report_qcable_id text,
    CONSTRAINT donor_case_donor_id_match FOREIGN KEY (donor_id) REFERENCES donor (id),
    CONSTRAINT donor_case_unique
        UNIQUE (donor_id, assay_name, tissue_origin, tissue_type, timepoint),
    CONSTRAINT donor_case_receipt_match FOREIGN KEY (receipt_qcable_id) REFERENCES qcable (id),
    CONSTRAINT donor_case_informatics_match FOREIGN KEY (informatics_qcable_id)
        REFERENCES qcable (id),
    CONSTRAINT donor_case_draft_report_match FOREIGN KEY (draft_report_qcable_id)
        REFERENCES qcable (id),
    CONSTRAINT donor_case_final_report_match FOREIGN KEY (final_report_qcable_id)
        REFERENCES qcable (id)
);

CREATE TABLE case_project (
    case_id text NOT NULL,
    project_id text NOT NULL,
    PRIMARY KEY (case_id, project_id),
    CONSTRAINT case_project_case_match FOREIGN KEY (case_id) REFERENCES donor_case (id)
        ON DELETE CASCADE,
    CONSTRAINT case_project_project_match FOREIGN KEY (project_id) REFERENCES project (id)
);

CREATE TABLE case_test (
    id SERIAL PRIMARY KEY,
    case_id text NOT NULL,
    name text NOT NULL,
    tissue_origin text,
    tissue_type text,
    timepoint text,
    group_id text,
    extraction_qcable_id text,
    library_preparation_qcable_id text,
    library_qualification_qcable_id text,
    full_depth_qcable_id text,
    CONSTRAINT case_test_case_match FOREIGN KEY (case_id) REFERENCES donor_case (id)
        ON DELETE CASCADE,
    CONSTRAINT case_test_unique
        UNIQUE (case_id, name, tissue_origin, tissue_type, timepoint, group_id),
    CONSTRAINT donor_case_extraction_match FOREIGN KEY (extraction_qcable_id)
        REFERENCES qcable (id),
    CONSTRAINT donor_case_library_preparation_match FOREIGN KEY (library_preparation_qcable_id)
        REFERENCES qcable (id),
    CONSTRAINT donor_case_library_qualification_match FOREIGN KEY (library_qualification_qcable_id)
        REFERENCES qcable (id),
    CONSTRAINT donor_case_full_depth_match FOREIGN KEY (full_depth_qcable_id)
        REFERENCES qcable (id)
);

CREATE TABLE deliverable_file (
    id SERIAL PRIMARY KEY,
    project_id text NOT NULL,
    location text NOT NULL,
    notes text,
    expiry_date timestamp
);
-- No foreign key on project_id due to reload procedure deleting projects

CREATE TABLE donor_deliverable (
    deliverable_id int NOT NULL,
    donor_id text NOT NULL,
    PRIMARY KEY (deliverable_id, donor_id),
    CONSTRAINT deliverable_id_match FOREIGN KEY (deliverable_id) REFERENCES deliverable_file (id)
        ON DELETE CASCADE
);
-- No foreign key on donor_id due to reload procedure deleting donors

CREATE TABLE changelog (
    id SERIAL PRIMARY KEY,
    change_date timestamp NOT NULL,
    content text NOT NULL
);

CREATE TABLE project_changelog(
    project_id text NOT NULL,
    changelog_id integer NOT NULL,
    PRIMARY KEY (project_id, changelog_id),
    CONSTRAINT project_changelog_changelog_match FOREIGN KEY (changelog_id)
        REFERENCES changelog (id) ON DELETE CASCADE
);
-- No foreign key on project_id due to reload procedure deleting projects

CREATE TABLE donor_case_changelog(
    case_id text NOT NULL,
    changelog_id integer NOT NULL,
    PRIMARY KEY (case_id, changelog_id),
    CONSTRAINT donor_case_changelog_changelog_match FOREIGN KEY (changelog_id)
        REFERENCES changelog (id) ON DELETE CASCADE
);
-- No foreign key on case_id due to reload procedure deleting donor_cases

CREATE TABLE project_info_item (
    id SERIAL PRIMARY KEY,
    project_id text NOT NULL,
    type text NOT NULL,
    content text,
    expected integer,
    received integer,
    CONSTRAINT project_info_item_project_id_match FOREIGN KEY (project_id) REFERENCES project (id)
        ON DELETE CASCADE
);

-- give user a project called "ADMINISTRATOR" to give them admin privileges
CREATE TABLE user_access (
    username text,
    project text
);
