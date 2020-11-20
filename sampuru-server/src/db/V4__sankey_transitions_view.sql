CREATE VIEW sankey_transition AS
SELECT
project_id,

-- receipt to extraction
SUM(CASE WHEN tissue_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS receipt_total,
SUM(CASE WHEN extraction_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS receipt_extraction,
SUM(CASE WHEN tissue_qcable_status = 'failed' THEN 1 ELSE 0 END) AS receipt_failed,

-- extraction to lib prep
SUM(CASE WHEN extraction_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS extraction_total,
SUM(CASE WHEN library_preparation_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS extraction_library_preparation,
SUM(CASE WHEN extraction_qcable_status = 'failed' THEN 1 ELSE 0 END) AS extraction_failed,
SUM(CASE WHEN extraction_qcable_status = 'pending' THEN 1 ELSE 0 END) AS extraction_pending,

-- lib prep to low pass
SUM(CASE WHEN library_preparation_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS library_preparation_total,
SUM(CASE WHEN low_pass_sequencing_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS library_preparation_low_pass_sequencing,
SUM(CASE WHEN library_preparation_qcable_status = 'failed' THEN 1 ELSE 0 END) AS library_preparation_failed,
SUM(CASE WHEN library_preparation_qcable_status = 'pending' THEN 1 ELSE 0 END) AS library_preparation_pending,

-- low pass to full depth
SUM(CASE WHEN low_pass_sequencing_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS low_pass_sequencing_total,
SUM(CASE WHEN full_depth_sequencing_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS low_pass_sequencing_full_depth_sequencing,
SUM(CASE WHEN low_pass_sequencing_qcable_status = 'failed' THEN 1 ELSE 0 END) AS low_pass_sequencing_failed,
SUM(CASE WHEN low_pass_sequencing_qcable_status = 'pending' THEN 1 ELSE 0 END) AS low_pass_sequencing_pending,

-- full depth to informatics
SUM(CASE WHEN full_depth_sequencing_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS full_depth_sequencing_total,
SUM(CASE WHEN informatics_interpretation_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS full_depth_sequencing_informatics_interpretation,
SUM(CASE WHEN full_depth_sequencing_qcable_status = 'failed' THEN 1 ELSE 0 END) AS full_depth_sequencing_failed,
SUM(CASE WHEN full_depth_sequencing_qcable_status = 'pending' THEN 1 ELSE 0 END) AS full_depth_sequencing_pending,

-- informatics to final report
SUM(CASE WHEN informatics_interpretation_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS informatics_interpretation_total,
SUM(CASE WHEN final_report_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS informatics_interpretation_final_report,
SUM(CASE WHEN informatics_interpretation_qcable_status = 'failed' THEN 1 ELSE 0 END) AS informatics_interpretation_failed,
SUM(CASE WHEN informatics_interpretation_qcable_status = 'pending' THEN 1 ELSE 0 END) AS informatics_interpretation_pending,

-- final report to completion
SUM(CASE WHEN final_report_qcable_alias IS NOT NULL THEN 1 ELSE 0 END) AS final_report_total,
SUM(CASE WHEN final_report_qcable_status = 'passed' THEN 1 ELSE 0 END) AS final_report_passed,
SUM(CASE WHEN final_report_qcable_status = 'failed' THEN 1 ELSE 0 END) AS final_report_failed,
SUM(CASE WHEN final_report_qcable_status = 'pending' THEN 1 ELSE 0 END) AS final_report_pending

FROM qcable_table
GROUP BY project_id;