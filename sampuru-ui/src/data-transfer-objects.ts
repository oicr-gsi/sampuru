/**
 * Data returned from api/active_projects
 * */
export interface Project {
  id: number,
  name: string,
  cases_total: number,
  cases_completed: number,
  qcables_total: number,
  qcables_completed: number,
  last_update: Date,
}

export interface ProjectJSON {
  id: number,
  name: string,
  cases_total: number,
  cases_completed: number,
  qcables_total: number,
  qcables_completed: number,
  last_update: string,
}


/**
 * Data returned from api/project_overview
 *
 */

export interface ProjectInfo {
  id: number,
  name: string,
  contact_name: string,
  contact_email: string,
  info_items: string[],
  failures: string[],
  sankey_transitions: Map<string, Map<string, string>>,
  deliverables: string[],
  qcables_total: number,
  qcables_completed: number,
  cases_total: number,
  cases_completed: number,
  completion_date: string
}

export interface QCable {
  project_id: number,
  case_id: number,
  tissue_qcable_alias: string | null,
  tissue_qcable_status: string | null,
  extraction_qcable_alias: string | null,
  extraction_qcable_status: string | null,
  library_preparation_qcable_alias: string | null,
  library_preparation_qcable_status: string | null,
  low_pass_sequencing_qcable_alias: string | null,
  low_pass_sequencing_qcable_status: string | null,
  full_depth_sequencing_qcable_alias: string | null,
  full_depth_sequencing_qcable_status: string | null,
  informatics_interpretation_qcable_alias: string | null,
  informatics_interpretation_qcable_status: string | null,
  final_report_qcable_alias: string | null,
  final_report_qcable_status: string | null
}