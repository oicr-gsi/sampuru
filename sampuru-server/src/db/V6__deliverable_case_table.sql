CREATE TABLE deliverable_case (
    deliverable_id int NOT NULL,
    case_id text NOT NULL
);

ALTER TABLE deliverable_case ADD CONSTRAINT deliverable_id_match FOREIGN KEY (deliverable_id) REFERENCES deliverable_file (id);
ALTER TABLE deliverable_case ADD CONSTRAINT case_id_match FOREIGN KEY (case_id) REFERENCES donor_case (id);