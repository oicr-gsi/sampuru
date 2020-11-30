/**
 * Data returned from api/active_projects
 * */
export interface Project {
  id: string,
  name: string,
  cases_total: number,
  cases_completed: number,
  qcables_total: number,
  qcables_completed: number,
  last_update: Date,
}

export interface ProjectJSON {
  id: string,
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

export interface SankeyTransition {
  receipt: {total: number, pending: number, passed: number, failed: number},
  extraction: {total: number, pending: number, passed: number, failed: number},
  library_preparation: {total: number, pending: number, passed: number, failed: number},
  low_pass_sequencing: {total: number, pending: number, passed: number, failed: number},
  full_depth_sequencing: {total: number, pending: number, passed: number, failed: number},
  informatics_interpretation: {total: number, pending: number, passed: number, failed: number},
  final_report: {total: number, pending: number, passed: number, failed: number},
}

export interface ProjectInfo {
  id: string,
  name: string,
  contact_name: string,
  contact_email: string,
  info_items: string[],
  failures: string[],
  sankey_transitions: SankeyTransition,
  deliverables: string[],
  qcables_total: number,
  qcables_completed: number,
  cases_total: number,
  cases_completed: number,
  completion_date: string
}

export interface QCable {
  project_id: string,
  case_id: string,
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

export interface Step {
  total: number,
  completed: number,
  type: string,
  status: string
}

export interface Bar {
  library_design: string | null,
  steps: Step[]
}

export interface Case {
  name: string,
  id: number,
  bars: Bar[]
}