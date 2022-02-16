CREATE OR REPLACE VIEW cases_per_quality_gate AS
SELECT project_id,
       COUNT(DISTINCT (CASE WHEN receipt_inspection_qcable_alias IS NOT NULL AND receipt_inspection_qcable_status = 'passed' THEN case_id END)) AS receipt_inspection_completed_cases,
       COUNT(DISTINCT (CASE WHEN receipt_inspection_qcable_alias IS NOT NULL AND receipt_inspection_qcable_status IN ('pending', 'failed') THEN case_id END)) AS receipt_inspection_incomplete_cases,
       COUNT(DISTINCT (CASE WHEN extraction_qcable_alias IS NOT NULL AND extraction_qcable_status = 'passed' THEN case_id END)) AS extraction_completed_cases,
       COUNT(DISTINCT (CASE WHEN extraction_qcable_alias IS NOT NULL AND extraction_qcable_status IN ('pending', 'failed') THEN case_id END)) AS extraction_incomplete_cases,
       COUNT(DISTINCT (CASE WHEN library_preparation_qcable_alias IS NOT NULL AND library_preparation_qcable_status = 'passed' THEN case_id END)) AS library_preparation_completed_cases,
       COUNT(DISTINCT (CASE WHEN library_preparation_qcable_alias IS NOT NULL AND library_preparation_qcable_status IN ('pending', 'failed') THEN case_id END)) AS library_preparation_incomplete_cases,
       COUNT(DISTINCT (CASE WHEN library_qualification_qcable_alias IS NOT NULL AND library_qualification_qcable_status = 'passed' THEN case_id END)) AS library_qualification_completed_cases,
       COUNT(DISTINCT (CASE WHEN library_qualification_qcable_alias IS NOT NULL AND library_qualification_qcable_status IN ('pending', 'failed') THEN case_id END)) AS library_qualification_incomplete_cases,
       COUNT(DISTINCT (CASE WHEN full_depth_sequencing_qcable_alias IS NOT NULL AND full_depth_sequencing_qcable_status = 'passed' THEN case_id END)) AS full_depth_sequencing_completed_cases,
       COUNT(DISTINCT (CASE WHEN full_depth_sequencing_qcable_alias IS NOT NULL AND full_depth_sequencing_qcable_status IN ('pending', 'failed') THEN case_id END)) AS full_depth_sequencing_incomplete_cases,
       COUNT(DISTINCT (CASE WHEN informatics_interpretation_qcable_alias IS NOT NULL AND informatics_interpretation_qcable_status = 'passed' THEN case_id END)) AS informatics_interpretation_completed_cases,
       COUNT(DISTINCT (CASE WHEN informatics_interpretation_qcable_alias IS NOT NULL AND informatics_interpretation_qcable_status IN ('pending', 'failed') THEN case_id END)) AS informatics_interpretation_incomplete_cases,
       COUNT(DISTINCT (CASE WHEN draft_report_qcable_alias IS NOT NULL AND draft_report_qcable_status = 'passed' THEN case_id END)) AS draft_report_completed_cases,
       COUNT(DISTINCT (CASE WHEN draft_report_qcable_alias IS NOT NULL AND draft_report_qcable_status IN ('pending', 'failed') THEN case_id END)) AS draft_report_incomplete_cases,
       COUNT(DISTINCT (CASE WHEN final_report_qcable_alias IS NOT NULL AND final_report_qcable_status = 'passed' THEN case_id END)) AS final_report_completed_cases,
       COUNT(DISTINCT (CASE WHEN final_report_qcable_alias IS NOT NULL AND final_report_qcable_status IN ('pending', 'failed') THEN case_id END)) AS final_report_incomplete_cases
FROM qcable_table
GROUP BY project_id;

/*todo: do we care about cases with failed or pending qcables or just want to see a sum total*/


/*
To test if the numbers from this view are right
select count(distinct case_id) from qcable where qcable_type = 'receipt_inspection' and project_id = 'CAPPT';*/