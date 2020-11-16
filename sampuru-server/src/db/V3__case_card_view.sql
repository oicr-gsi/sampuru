CREATE VIEW case_card AS
SELECT
case_id,
library_design,
SUM(CASE WHEN tissue_qcable_status = 'passed' AND tissue_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS tissue_completed,
SUM(CASE WHEN tissue_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS tissue_total,
SUM(CASE WHEN extraction_qcable_status = 'passed' AND extraction_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS extraction_completed,
SUM(CASE WHEN extraction_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS extraction_total,
SUM(CASE WHEN library_preparation_qcable_status = 'passed' AND library_preparation_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS library_preparation_completed,
SUM(CASE WHEN library_preparation_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS library_preparation_total,
SUM(CASE WHEN low_pass_sequencing_qcable_status = 'passed' AND low_pass_sequencing_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS low_pass_sequencing_completed,
SUM(CASE WHEN low_pass_sequencing_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS low_pass_sequencing_total,
SUM(CASE WHEN full_depth_sequencing_qcable_status = 'passed' AND full_depth_sequencing_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS full_depth_sequencing_completed,
SUM(CASE WHEN full_depth_sequencing_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS full_depth_sequencing_total,
SUM(CASE WHEN informatics_interpretation_qcable_status = 'passed' AND informatics_interpretation_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS informatics_interpretation_completed,
SUM(CASE WHEN informatics_interpretation_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS informatics_interpretation_total,
SUM(CASE WHEN final_report_qcable_status = 'passed' AND final_report_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS final_report_completed,
SUM(CASE WHEN final_report_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS final_report_total
FROM qcable_table GROUP BY case_id, library_design;