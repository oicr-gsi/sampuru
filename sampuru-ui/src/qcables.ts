/// <reference types="jquery" />
/// <reference types="bootstrap" />
/// <reference types="bootstrap-table" />

import {
  bootstrapTable,
  busyDialog,
  ComplexElement, elementFromTag,
  navbar,
  tableBodyFromRows,
  tableRow
} from "./html.js";
import {fetchAsPromise} from "./io.js";
import {QCable} from "./data-transfer-objects.js";

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

export function qcablesTable(qcables: QCable[], projectName: string): void {
  const pageContainer = document.createElement("div");
  const pageHeader = document.createElement("h3");
  pageHeader.innerText = "QCables (" + projectName + ")";

  //todo: click on a cell and show alias??
  const tableRows: ComplexElement<HTMLTableRowElement>[] = [];
  qcables
    .forEach((qcable) => {
      tableRows.push(tableRow(null,
        {
          contents: qcable.case_id,
          className: "qcable-table-donor-case"
        },
        {
          contents: qcable.tissue_qcable_alias ? qcable.tissue_qcable_alias: "",
          className: statusToClassName(qcable.tissue_qcable_status)
        },
        {
          contents: qcable.extraction_qcable_alias ? qcable.extraction_qcable_alias: "",
          className: statusToClassName(qcable.extraction_qcable_status)
        },
        {
          contents: qcable.library_preparation_qcable_alias ? qcable.library_preparation_qcable_alias: "",
          className: statusToClassName(qcable.library_preparation_qcable_status)
        },
        {
          contents: qcable.low_pass_sequencing_qcable_alias ? qcable.low_pass_sequencing_qcable_alias: "",
          className: statusToClassName(qcable.low_pass_sequencing_qcable_status)
        },
        {
          contents: qcable.full_depth_sequencing_qcable_alias ? qcable.full_depth_sequencing_qcable_alias: "",
          className: statusToClassName(qcable.full_depth_sequencing_qcable_status)
        },
        {
          contents: qcable.informatics_interpretation_qcable_alias ? qcable.informatics_interpretation_qcable_alias: "",
          className: statusToClassName(qcable.informatics_interpretation_qcable_status)
        },
        {
          contents: qcable.final_report_qcable_alias ? qcable.final_report_qcable_alias: "",
          className: statusToClassName(qcable.final_report_qcable_status)
        }
      ));

    });

  const tableHeaders = new Map([
    ["case_id", "Case"],
    ["tissue_qcable_alias", "Receipt/Inspection"],
    ["extraction_qcable_alias", "Extraction"],
    ["library_preparation_qcable_alias", "Library Preparation"],
    ["low_pass_sequencing_qcable_alias", "Low-Pass Sequencing"],
    ["full_depth_sequencing_qcable_alias", "Full-Depth Sequencing"],
    ["informatics_interpretation_qcable_alias", "Informatics Pipeline + Variant Interpretation"],
    ["final_report_qcable_alias", "Final Report"]])

  const table = bootstrapTable(tableHeaders, true, true);
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
    const cellValue = elementFromTag("div", "card",
      elementFromTag("div", "card-body", value));

    const childNodes = $element.children();
    if (childNodes.length) {
      childNodes.remove();
    } else {
      if(value != "" && field != "case_id") {
        $element.attr('id', value).append(cellValue.element);
      } else if (field != "case_id") {
        // Let user know QCable hasn't been created yet
        const emptyNotifier = elementFromTag("div", "card",
          elementFromTag("div", "card-body", "Hasn't yet started"));
        $element.attr('id', field).append(emptyNotifier.element);
      }
    }
  });
}

export function initialiseQCables(filterType: string, filterId: string) {
  const closeBusy = busyDialog();

  fetchAsPromise<QCable[]>("api/qcables_table/" + filterType + "/" + filterId, {body: null})
    .then((data) => {
      qcablesTable(data, filterId);
    })
    .finally(closeBusy);
}