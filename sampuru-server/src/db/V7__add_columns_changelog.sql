ALTER TABLE changelog
ADD COLUMN qcable_type text,
ADD COLUMN case_external_name text NOT NULL;