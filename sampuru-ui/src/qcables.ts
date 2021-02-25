/// <reference types="jquery" />
/// <reference types="bootstrap" />
/// <reference types="bootstrap-table" />

import {
  bootstrapTable,
  busyDialog,
  ComplexElement, DOMElement, elementFromTag,
  navbar,
  tableBodyFromRows,
  tableRow,
  toSentenceCase
} from "./html.js";
import {Changelog, QCable} from "./data-transfer-objects.js";

const urlParams = new URLSearchParams(window.location.search);
const filterType = urlParams.get("qcables-filter-type");
const filterId = urlParams.get("qcables-filter-id");

if (filterType && filterId) {
  document.body.appendChild(navbar());
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
      throw new Error("Unknown QCable status");
  }
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
    pageHeader.innerText = "QCables (" + caseExtName + ")";
  } else {
    pageHeader.innerText = "QCables (" + filterId + ")";
  }

  const tableRows: ComplexElement<HTMLTableRowElement>[] = [];
  qcables
    .forEach((qcable) => {
      tableRows.push(tableRow(null,
        {
          contents: qcable.case_external_name,
          className: "qcable-table-donor-case"
        },
        {
          contents: qcable.tissue_qcable_alias ? qcable.tissue_qcable_alias: "",
          className: statusToClassName(qcable.tissue_qcable_status),
          title: qcable.tissue_qcable_status? toSentenceCase(qcable.tissue_qcable_status) : titleUnknown
        },
        {
          contents: qcable.extraction_qcable_alias ? qcable.extraction_qcable_alias: "",
          className: statusToClassName(qcable.extraction_qcable_status),
          title: qcable.extraction_qcable_status? toSentenceCase(qcable.extraction_qcable_status) : titleUnknown
        },
        {
          contents: qcable.library_preparation_qcable_alias ? qcable.library_preparation_qcable_alias: "",
          className: statusToClassName(qcable.library_preparation_qcable_status),
          title: qcable.library_preparation_qcable_status? toSentenceCase(qcable.library_preparation_qcable_status) : titleUnknown
        },
        {
          contents: qcable.low_pass_sequencing_qcable_alias ? qcable.low_pass_sequencing_qcable_alias: "",
          className: statusToClassName(qcable.low_pass_sequencing_qcable_status),
          title: qcable.low_pass_sequencing_qcable_status? toSentenceCase(qcable.low_pass_sequencing_qcable_status) : titleUnknown
        },
        {
          contents: qcable.full_depth_sequencing_qcable_alias ? qcable.full_depth_sequencing_qcable_alias: "",
          className: statusToClassName(qcable.full_depth_sequencing_qcable_status),
          title: qcable.full_depth_sequencing_qcable_status? toSentenceCase(qcable.full_depth_sequencing_qcable_status) : titleUnknown
        },
        {
          contents: qcable.informatics_interpretation_qcable_alias ? qcable.informatics_interpretation_qcable_alias: "",
          className: statusToClassName(qcable.informatics_interpretation_qcable_status),
          title: qcable.informatics_interpretation_qcable_status? toSentenceCase(qcable.informatics_interpretation_qcable_status) : titleUnknown
        },
        {
          contents: qcable.final_report_qcable_alias ? qcable.final_report_qcable_alias: "",
          className: statusToClassName(qcable.final_report_qcable_status),
          title: qcable.final_report_qcable_status? toSentenceCase(qcable.final_report_qcable_status) : titleUnknown
        }
      ));

    });

  const tableHeaders = new Map([
    ["case_external_name", "Project:Case"],
    ["tissue_qcable_alias", "Receipt/Inspection"],
    ["extraction_qcable_alias", "Extraction"],
    ["library_preparation_qcable_alias", "Library Preparation"],
    ["low_pass_sequencing_qcable_alias", "Low-Pass Sequencing"],
    ["full_depth_sequencing_qcable_alias", "Full-Depth Sequencing"],
    ["informatics_interpretation_qcable_alias", "Informatics Pipeline + Variant Interpretation"],
    ["final_report_qcable_alias", "Final Report"]])

  const table = bootstrapTable(tableHeaders, true, true, "table");
  const tableBody = tableBodyFromRows(null, tableRows);

  table.appendChild(tableBody);

  pageContainer.appendChild(pageHeader);
  pageContainer.appendChild(table);
  document.body.appendChild(pageContainer);

  $(function () {
    $('#table').bootstrapTable({
      formatSearch: function() {
        return 'Search QCables';
      }
    });
  });


  $('#table').on('click-cell.bs.table', function(event, field, value, row, $element) {
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
        if(value != "" && field != "case_external_name") {
          $element.attr('id', value).append(cellValue.element);
        } else if (field != "case_external_name") {
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
    .then(responses => Promise.all(responses.map(response => response.json())))
    .then((responses) => {
      const qcables = responses[0] as QCable[]
      const changelogs = responses[1] as Changelog[]

      qcablesTable(qcables, changelogs, filterType, filterId);
    })
    .finally(closeBusy);
}