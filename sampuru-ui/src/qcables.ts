/// <reference types="jquery" />
/// <reference types="bootstrap" />
/// <reference types="bootstrap-table" />
/// <reference types="file-saver" />

import {
  bootstrapTable,
  busyDialog,
  ComplexElement,
  createLinkElement,
  DOMElement,
  elementFromTag,
  navbar,
  tableBodyFromRows,
  tableRow,
  toSentenceCase,
  createButton, exportToolbar
} from "./html.js";
import {Changelog, QCable} from "./data-transfer-objects.js";
import {commonName, formatLibraryDesigns} from "./common.js";
import {urlConstructor} from "./io.js";

const urlParams = new URLSearchParams(window.location.search);
const filterType = urlParams.get("qcables-filter-type");
const filterId = urlParams.get("qcables-filter-id");

if (filterType && filterId) {
  initialiseQCables(filterType, filterId);
}

/**
 * Convert QCable status to Bootstrap class to color the cell
 * */
function statusToClassName(status: string | null) {
  switch(status) {
    case "passed":
      return "table-primary";
    case "failed":
      return "table-danger";
    case "pending":
      return "table-warning";
    case null:
      return "table-secondary";
    default:
      throw new Error("Unknown QC-able status");
  }
}


function alphaNumSort(a: string, b: string) {
  // Remove non-alphanumeric characters
  a = a.replace(/[\W_]+/g,"");
  b = b.replace(/[\W_]+/g,"");
  if (a > b) return 1;
  if (a < b) return -1;
  return 0;
}

function constructTableRows(external: boolean, qcables: QCable[]): ComplexElement<HTMLTableRowElement>[] {
  const tableRows: ComplexElement<HTMLTableRowElement>[] = [];
  const titleUnknown = "Hasn't started yet";
  qcables
    .forEach((qcable) => {
      tableRows.push(tableRow(null,
        {
          contents: external ? qcable.case_external_name : qcable.case_id,
          className: "qcable-table-donor-case"
        },
        {
          contents: formatLibraryDesigns(qcable.library_design),
          className: "qcable-table-donor-case"
        },
        {
          contents: external ?
            (qcable.receipt_inspection_qcable_external_name ? qcable.receipt_inspection_qcable_external_name: "") :
            (qcable.receipt_inspection_qcable_alias ? qcable.receipt_inspection_qcable_alias: ""),
          className: statusToClassName(qcable.receipt_inspection_qcable_status),
          title: qcable.receipt_inspection_qcable_status? toSentenceCase(qcable.receipt_inspection_qcable_status) : titleUnknown
        },
        {
          contents: external ?
            (qcable.extraction_qcable_external_name ? qcable.extraction_qcable_external_name: "") :
            (qcable.extraction_qcable_alias ? qcable.extraction_qcable_alias: ""),
          className: statusToClassName(qcable.extraction_qcable_status),
          title: qcable.extraction_qcable_status? toSentenceCase(qcable.extraction_qcable_status) : titleUnknown
        },
        {
          contents: external ?
            (qcable.library_preparation_qcable_external_name ? qcable.library_preparation_qcable_external_name: "") :
            (qcable.library_preparation_qcable_alias ? qcable.library_preparation_qcable_alias: ""),
          className: statusToClassName(qcable.library_preparation_qcable_status),
          title: qcable.library_preparation_qcable_status? toSentenceCase(qcable.library_preparation_qcable_status) : titleUnknown
        },
        {
          contents: external ?
            (qcable.library_qualification_qcable_external_name ? qcable.library_qualification_qcable_external_name: "") :
            (qcable.library_qualification_qcable_alias ? qcable.library_qualification_qcable_alias: ""),
          className: statusToClassName(qcable.library_qualification_qcable_status),
          title: qcable.library_qualification_qcable_status? toSentenceCase(qcable.library_qualification_qcable_status) : titleUnknown
        },
        {
          contents: external ?
            (qcable.full_depth_sequencing_qcable_external_name ? qcable.full_depth_sequencing_qcable_external_name: "") :
            (qcable.full_depth_sequencing_qcable_alias ? qcable.full_depth_sequencing_qcable_alias: ""),
          className: statusToClassName(qcable.full_depth_sequencing_qcable_status),
          title: qcable.full_depth_sequencing_qcable_status? toSentenceCase(qcable.full_depth_sequencing_qcable_status) : titleUnknown
        },
        {
          contents: external ?
            (qcable.informatics_interpretation_qcable_external_name ? qcable.informatics_interpretation_qcable_external_name: "") :
            (qcable.informatics_interpretation_qcable_alias ? qcable.informatics_interpretation_qcable_alias: ""),
          className: statusToClassName(qcable.informatics_interpretation_qcable_status),
          title: qcable.informatics_interpretation_qcable_status? toSentenceCase(qcable.informatics_interpretation_qcable_status) : titleUnknown
        },
        {
          contents: external ?
            (qcable.draft_report_qcable_external_name ? qcable.draft_report_qcable_external_name: "") :
            (qcable.draft_report_qcable_alias ? qcable.draft_report_qcable_alias: ""),
          className: statusToClassName(qcable.draft_report_qcable_status),
          title: qcable.draft_report_qcable_status? toSentenceCase(qcable.draft_report_qcable_status) : titleUnknown
        },
        {
          contents: external ?
            (qcable.final_report_qcable_external_name ? qcable.final_report_qcable_external_name: "") :
            (qcable.final_report_qcable_alias ? qcable.final_report_qcable_alias: ""),
          className: statusToClassName(qcable.final_report_qcable_status),
          title: qcable.final_report_qcable_status? toSentenceCase(qcable.final_report_qcable_status) : titleUnknown
        }
      ));
    });
  return tableRows;
}

function constructTableHeaders(external: boolean) {
  return new Map([
    [external ? "case_external_name" : "case_id", "Project:Case"],
    ["library_design", "Library Design"],
    [external ? "receipt_inspection_qcable_external_name" : "receipt_inspection_qcable_alias", "Receipt/Inspection"],
    [external ? "extraction_qcable_external_name" : "extraction_qcable_alias", "Extraction"],
    [external ? "library_preparation_qcable_external_name" : "library_preparation_qcable_alias", "Library Preparation"],
    [external ? "library_qualification_qcable_external_name" : "library_qualification_qcable_alias", "Library Qualification"],
    [external ? "full_depth_sequencing_qcable_external_name" : "full_depth_sequencing_qcable_alias", "Full-Depth Sequencing"],
    [external ? "informatics_interpretation_qcable_external_name" : "informatics_interpretation_qcable_alias", "Informatics Pipeline + Variant Interpretation"],
    [external ? "draft_report_qcable_external_name" : "draft_report_qcable_alias", "Draft Report"],
    [external ? "final_report_qcable_external_name" : "final_report_qcable_alias", "Final Report"]]);
}

export function qcablesTable(
  qcables: QCable[],
  changelogs: Changelog[],
  filterType: string,
  filterId: string): void {
  const pageContainer = document.createElement("div");
  const pageHeader = document.createElement("h3");
  const titleUnknown = "Hasn't started yet";
  if(filterType === "case") {
    const caseExtName = qcables[0].case_external_name;
    pageHeader.innerText = "QC-ables (" + caseExtName.replace(':', '\t') + ")";
  } else {
    pageHeader.innerText = "QC-ables (";
    const projectLink = createLinkElement(
      null,
      filterId,
      null,
      null,
      urlConstructor("project.html", ["project-overview-id"], [filterId]));
    pageHeader.appendChild(projectLink);
    pageHeader.innerHTML += ")";
  }

  const tableRowsInternal = constructTableRows(false, qcables);
  const tableRowsExternal = constructTableRows(true, qcables);

  const tableHeadersInternal = constructTableHeaders(false);
  const tableHeadersExternal = constructTableHeaders(true);

  const sortInternal = new Map();
  const sortExternal = new Map();
  sortInternal.set("case_id", "alphaNumSort");
  sortExternal.set("case_external_name", "alphaNumSort");

  const internalTable = bootstrapTable(tableHeadersInternal, true, true, sortInternal, "internal-table",true, true);
  const internalTableBody = tableBodyFromRows(null, tableRowsInternal);

  const externalTable = bootstrapTable(tableHeadersExternal, true, true, sortExternal, "external-table", true, true);
  const externalTableBody = tableBodyFromRows(null, tableRowsExternal);

  internalTable.setAttribute("data-sort-name", "case_id");
  internalTable.appendChild(internalTableBody);

  externalTable.setAttribute("data-sort-name", "case-external-name");
  externalTable.append(externalTableBody);

  const toolbar = exportToolbar();
  toolbar.classList.add("toolbar");
  toolbar.className = "toolbar";

  const toggleIds = createButton("qcable-id-toggle", "Switch to OICR Identifiers", "identifier");

  pageContainer.appendChild(pageHeader);
  pageContainer.appendChild(toggleIds);
  pageContainer.appendChild(toolbar);
  pageContainer.appendChild(externalTable);
  pageContainer.appendChild(internalTable);
  document.body.appendChild(pageContainer);

  $(function () {
    $('#internal-table,#external-table').bootstrapTable({
      formatSearch: function() {
        return 'Search QC-ables';
      }
    });

    // Default to show external table and hide internal table
    $('div').removeClass('clearfix'); // This is a Bootstrap class that gets preset for all tables that isn't needed
    $('#internal-table').parents().hide();
    $('#external-table').parents().show();

    // Show appropriate table on button click
    // If user clicks on "OICR Identifiers" button, hide external table and change button text to "External identifiers"
    $('#qcable-id-toggle').on('click', function() {
      $(this).text(function(i, text) {
        if(text === "Switch to OICR Identifiers") {
          $('#external-table').parents().hide();
          $('#internal-table').parents().show();
          return "Switch to External Identifiers";
        } else {
          $('#internal-table').parents().hide();
          $('#external-table').parents().show();
          return "Switch to OICR Identifiers";
        }
      });
    });
  });

  const $table = $("#table");

  $(function() {
    $("#toolbar")
        .find("select")
        .change(function() {
          $table.bootstrapTable("refreshOptions", {
            exportDataType: $(this).val()
          });
        });
  });

  $('#internal-table,#external-table').on('click-cell.bs.table', function(event, field, value, row, $element) {
    const filteredChangelogs = changelogs.filter((item) => {
      return item.qcable_id === value
    });

    const displayChangelogs: DOMElement[] = [];
    if(filteredChangelogs.length) {
      displayChangelogs.push(elementFromTag("b", null,
        elementFromTag("br", null), "Changelogs:"));
    }

    filteredChangelogs.map((item) => {
        displayChangelogs.push(elementFromTag("p", null, item.content, null));
    });

    const cellValue = elementFromTag("div", "card",
      elementFromTag("div", "card-body", value, displayChangelogs));

    const selection = window.getSelection();
    const isEmpty = (selection != null) ? selection.toString() : "";
    // Don't register click events when user is selecting text
    if(isEmpty.length <= 0) {
      const childNodes = $element.children();

      if (childNodes.length) {
        childNodes.remove();
      } else {
        if(value != "" && field != "case_external_name" && field != "case_id" && field != "library_design") {
          $element.attr('id', value).append(cellValue.element);
        } else if (field != "case_external_name" && field != "case_id" && field != "library_design") {
          // Let user know QCable hasn't been created yet
          const emptyNotifier = elementFromTag("div", "card",
            elementFromTag("div", "card-body", "Hasn't yet started"));
          $element.attr('id', field).append(emptyNotifier.element);
        }
      }
    }
  });
}

export function initialiseQCables(filterType: string, filterId: string) {
  const closeBusy = busyDialog();

  Promise.all([
    fetch("api/qcables_table/" + filterType + "/" + filterId),
    fetch("api/changelogs/" + filterType + "/" + filterId)
  ])
    .then(responses => {
      document.body.appendChild(navbar(commonName(responses[0]), "qcables"));
      return Promise.all(responses.map(response => response.json()));
    }).then((responses) => {
      const qcables = responses[0] as QCable[]
      const changelogs = responses[1] as Changelog[]

      qcablesTable(qcables, changelogs, filterType, filterId);
    })
    .finally(closeBusy);

}
