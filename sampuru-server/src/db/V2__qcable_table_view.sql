CREATE VIEW qcable_table AS
SELECT
tissue_qcable.project_id AS project_id,
tissue_qcable.case_id AS case_id,
library_preparation_qcable.library_design AS library_design,
tissue_qcable.oicr_alias AS tissue_qcable_alias,
tissue_qcable.external_name AS tissue_qcable_external_name,
tissue_qcable.status AS tissue_qcable_status,
extraction_qcable.oicr_alias AS extraction_qcable_alias,
extraction_qcable.external_name AS extraction_qcable_external_name,
extraction_qcable.status AS extraction_qcable_status,
library_preparation_qcable.oicr_alias AS library_preparation_qcable_alias,
library_preparation_qcable.external_name AS library_preparation_qcable_external_name,
library_preparation_qcable.status AS library_preparation_qcable_status,
low_pass_sequencing_qcable.oicr_alias AS low_pass_sequencing_qcable_alias,
low_pass_sequencing_qcable.external_name AS low_pass_sequencing_qcable_external_name,
low_pass_sequencing_qcable.status AS low_pass_sequencing_qcable_status,
full_depth_sequencing_qcable.oicr_alias AS full_depth_sequencing_qcable_alias,
full_depth_sequencing_qcable.external_name AS full_depth_sequencing_qcable_external_name,
full_depth_sequencing_qcable.status AS full_depth_sequencing_qcable_status,
informatics_interpretation_qcable.oicr_alias AS informatics_interpretation_qcable_alias,
informatics_interpretation_qcable.external_name AS informatics_interpretation_qcable_external_name,
informatics_interpretation_qcable.status AS informatics_interpretation_qcable_status,
final_report_qcable.oicr_alias AS final_report_qcable_alias,
final_report_qcable.external_name AS final_report_qcable_external_name,
final_report_qcable.status AS final_report_qcable_status
FROM (SELECT * FROM qcable WHERE qcable_type = 'receipt_inspection') AS tissue_qcable
LEFT JOIN (SELECT * FROM qcable WHERE qcable_type = 'extraction') AS extraction_qcable ON extraction_qcable.parent_id = tissue_qcable.id
LEFT JOIN (SELECT * FROM qcable WHERE qcable_type = 'library_preparation') AS library_preparation_qcable ON extraction_qcable.id = library_preparation_qcable.parent_id
LEFT JOIN (SELECT * FROM qcable WHERE qcable_type = 'low_pass_sequencing') AS low_pass_sequencing_qcable ON library_preparation_qcable.id = low_pass_sequencing_qcable.parent_id
LEFT JOIN (SELECT * FROM qcable WHERE qcable_type = 'full_depth_sequencing') AS full_depth_sequencing_qcable ON library_preparation_qcable.id = full_depth_sequencing_qcable.parent_id
LEFT JOIN (SELECT * FROM qcable WHERE qcable_type = 'informatics_interpretation') AS informatics_interpretation_qcable ON library_preparation_qcable.id = informatics_interpretation_qcable.parent_id
LEFT JOIN (SELECT * FROM qcable WHERE qcable_type = 'final_report') AS final_report_qcable ON library_preparation_qcable.id = final_report_qcable.parent_id;

