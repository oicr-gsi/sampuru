CREATE OR REPLACE VIEW cases_per_quality_gate AS
SELECT project_id,
       COUNT(DISTINCT (CASE WHEN receipt_inspection_qcable_alias IS NOT NULL AND receipt_inspection_qcable_status = 'passed' AND case_id NOT IN (SELECT DISTINCT case_id FROM qcable WHERE qcable_type = 'receipt_inspection' AND status IN ('failed', 'pending')) THEN case_id END)) AS receipt_inspection_completed_cases, /*We only care about failed and pending cases at this gate*/
       COUNT(DISTINCT (CASE WHEN extraction_qcable_alias IS NOT NULL AND extraction_qcable_status = 'passed' THEN case_id END)) AS extraction_completed_cases,
       COUNT(DISTINCT (CASE WHEN library_preparation_qcable_alias IS NOT NULL AND library_preparation_qcable_status = 'passed' THEN case_id END)) AS library_preparation_completed_cases,
       COUNT(DISTINCT (CASE WHEN library_qualification_qcable_alias IS NOT NULL AND library_qualification_qcable_status = 'passed' THEN case_id END)) AS library_qualification_completed_cases,
       COUNT(DISTINCT (CASE WHEN full_depth_sequencing_qcable_alias IS NOT NULL AND full_depth_sequencing_qcable_status = 'passed' THEN case_id END)) AS full_depth_sequencing_completed_cases,
       COUNT(DISTINCT (CASE WHEN informatics_interpretation_qcable_alias IS NOT NULL AND informatics_interpretation_qcable_status = 'passed' THEN case_id END)) AS informatics_interpretation_completed_cases,
       COUNT(DISTINCT (CASE WHEN draft_report_qcable_alias IS NOT NULL AND draft_report_qcable_status = 'passed' THEN case_id END)) AS draft_report_completed_cases,
       COUNT(DISTINCT (CASE WHEN final_report_qcable_alias IS NOT NULL AND final_report_qcable_status = 'passed' THEN case_id END)) AS final_report_completed_cases
FROM qcable_table
GROUP BY project_id;