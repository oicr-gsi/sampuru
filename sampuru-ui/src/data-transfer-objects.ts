/**
 * Object returned from api/active_projects
 * */
export interface ActiveProject {
  id: string,
  name: string,
  cases_total: number,
  cases_completed: number,
  qcables_total: number,
  qcables_completed: number,
  last_update: string
}

export interface SankeyTransition {
  receipt: {total: number, pending: number, extraction: number, failed: number},
  extraction: {total: number, pending: number, library_preparation: number, failed: number},
  library_preparation: {total: number, pending: number, low_pass_sequencing: number, failed: number},
  low_pass_sequencing: {total: number, pending: number, full_depth_sequencing: number, failed: number},
  full_depth_sequencing: {total: number, pending: number, informatics_interpretation: number, failed: number},
  informatics_interpretation: {total: number, pending: number, final_report: number, failed: number},
  final_report: {total: number, pending: number, passed: number, failed: number},
}

export interface InfoItem {
  id: number,
  entry_type: string,
  content: string,
  expected: number | string,
  received: number | string
}

export interface BaseProject<I> {
  id: string,
  name: string,
  contact_name: string,
  contact_email: string,
  description: string,
  pipeline: string,
  created_date: string,
  reference_genome: string,
  kits: string,
  info_items: I[],
  deliverables: string[],
  completion_date: string,
}

/**
 * Object returned from api/project_overview
 * */
export interface ProjectInfo extends BaseProject<InfoItem> {
  failures: string[],
  sankey_transitions: SankeyTransition,
  qcables_total: number,
  qcables_completed: number,
  cases_total: number,
  cases_completed: number
}

/**
 * Object returned from api/search/project
 * */
export interface SearchedProject extends BaseProject<string> {
  donor_cases: string[]
}

/**
 * Object returned from api/qcables_table/
 * */
export interface QCable {
  project_id: string,
  case_id: string,
  case_external_name: string,
  tissue_qcable_alias: string | null,
  tissue_qcable_external_name: string | null,
  tissue_qcable_status: string | null,
  extraction_qcable_alias: string | null,
  extraction_qcable_external_name: string | null,
  extraction_qcable_status: string | null,
  library_preparation_qcable_alias: string | null,
  library_preparation_qcable_external_name: string | null,
  library_preparation_qcable_status: string | null,
  low_pass_sequencing_qcable_alias: string | null,
  low_pass_sequencing_qcable_external_name: string | null,
  low_pass_sequencing_qcable_status: string | null,
  full_depth_sequencing_qcable_alias: string | null,
  full_depth_sequencing_qcable_external_name: string | null,
  full_depth_sequencing_qcable_status: string | null,
  informatics_interpretation_qcable_alias: string | null,
  informatics_interpretation_qcable_external_name: string | null,
  informatics_interpretation_qcable_status: string | null,
  final_report_qcable_alias: string | null,
  final_report_qcable_external_name: string | null,
  final_report_qcable_status: string | null
}

/**
 * Object returned from api/search/qcable
 * */
export interface SearchedQCable {
  id: string,
  type: string,
  status: string,
  library_design: string,
  parent_id: string,
  alias: string,
  changelog: string[],
  failure_reason: string
}

export interface Changelog {
  id: number,
  change_date: string,
  content: string
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

export interface BaseCase<C> {
  name: string,
  id: string,
  changelog: C[]
}

/**
 * Object returned from api/case_cards/
 * */
export interface CaseCard extends BaseCase<Changelog> {
  bars: Bar[]
}

/**
 * Object returned from api/search/case/
 * */
export interface SearchedCase extends BaseCase<string> {
  qcables: string[],
  deliverables: string[]
}

export interface Notification {
  id: number,
  user_id: string,
  issue_date: string, //todo: make sure this is a string not date
  resolved_date: string, //todo: make sure this is a string not date
  content: string
}

export interface DeliverableFile {
  id: number,
  project_id: string,
  location: string,
  notes: string,
  expiry_date: string //todo: check that it's string and add todo for better handling of dates
}

