CREATE VIEW qcable_table AS
SELECT
receipt_inspection_qcable.project_id       AS project_id,
receipt_inspection_qcable.case_id          AS case_id,
donor_case.name                            AS case_external_name,
library_preparation_qcable.library_design  AS library_design,
receipt_inspection_qcable.oicr_alias       AS receipt_inspection_qcable_alias,
receipt_inspection_qcable.external_name    AS receipt_inspection_qcable_external_name,
receipt_inspection_qcable.status           AS receipt_inspection_qcable_status,
extraction_qcable.oicr_alias               AS extraction_qcable_alias,
extraction_qcable.external_name            AS extraction_qcable_external_name,
extraction_qcable.status                   AS extraction_qcable_status,
library_preparation_qcable.oicr_alias      AS library_preparation_qcable_alias,
library_preparation_qcable.external_name   AS library_preparation_qcable_external_name,
library_preparation_qcable.status          AS library_preparation_qcable_status,
library_qualification_qcable.oicr_alias    AS library_qualification_qcable_alias,
library_qualification_qcable.external_name AS library_qualification_qcable_external_name,
library_qualification_qcable.status        AS library_qualification_qcable_status,
full_depth_sequencing_qcable.oicr_alias    AS full_depth_sequencing_qcable_alias,
full_depth_sequencing_qcable.external_name AS full_depth_sequencing_qcable_external_name,
full_depth_sequencing_qcable.status AS full_depth_sequencing_qcable_status,
informatics_interpretation_qcable.oicr_alias AS informatics_interpretation_qcable_alias,
informatics_interpretation_qcable.external_name AS informatics_interpretation_qcable_external_name,
informatics_interpretation_qcable.status AS informatics_interpretation_qcable_status,
draft_report_qcable.oicr_alias AS draft_report_qcable_alias,
draft_report_qcable.external_name AS draft_report_qcable_external_name,
draft_report_qcable.status AS draft_report_qcable_status,
final_report_qcable.oicr_alias AS final_report_qcable_alias,
final_report_qcable.external_name AS final_report_qcable_external_name,
final_report_qcable.status AS final_report_qcable_status
FROM (SELECT * FROM qcable WHERE qcable_type = 'receipt_inspection') AS receipt_inspection_qcable
LEFT JOIN (SELECT * FROM donor_case) AS donor_case ON receipt_inspection_qcable.case_id = donor_case.id
LEFT JOIN (SELECT * FROM qcable WHERE qcable_type = 'extraction') AS extraction_qcable ON receipt_inspection_qcable.id = extraction_qcable.parent_id
LEFT JOIN (SELECT * FROM qcable WHERE qcable_type = 'library_preparation') AS library_preparation_qcable ON receipt_inspection_qcable.id = library_preparation_qcable.parent_id
LEFT JOIN (SELECT * FROM qcable WHERE qcable_type = 'library_qualification') AS library_qualification_qcable ON receipt_inspection_qcable.id = library_qualification_qcable.parent_id
LEFT JOIN (SELECT * FROM qcable WHERE qcable_type = 'full_depth_sequencing') AS full_depth_sequencing_qcable ON receipt_inspection_qcable.id = full_depth_sequencing_qcable.parent_id
LEFT JOIN (SELECT * FROM qcable WHERE qcable_type = 'informatics_interpretation') AS informatics_interpretation_qcable ON receipt_inspection_qcable.id = informatics_interpretation_qcable.parent_id
LEFT JOIN (SELECT * FROM qcable WHERE qcable_type = 'draft_report') AS draft_report_qcable ON receipt_inspection_qcable.id = draft_report_qcable.parent_id
LEFT JOIN (SELECT * FROM qcable WHERE qcable_type = 'final_report') AS final_report_qcable ON receipt_inspection_qcable.id = final_report_qcable.parent_id;

