/// <reference types="jquery" />
/// <reference types="bootstrap" />
/// <reference types="bootstrap-table" />
/// <reference types="x-editable" />
/// <reference types="file-saver" />

import {
  bootstrapTable, busyDialog,
  ComplexElement,
  tableBodyFromRows, tableRow,
} from "./html.js";

import {DeliverableObject, ProjectCase} from "./data-transfer-objects.js";

initialiseDeliverables();

function constructTableRows(deliverableObjects: DeliverableObject): ComplexElement<HTMLTableRowElement>[] {
  const tableRows: ComplexElement<HTMLTableRowElement>[] = [];

  //todo: will need to reformat case_ids
  //todo: will need to reformat expiry_date
  //todo: need logic for adding new rows
  deliverableObjects.deliverables.forEach((deliverable) => {
    tableRows.push(tableRow(null,
      {
        contents: deliverable.project_id,
        className: "select-project",
        pk: deliverable.id
      },
      {
        contents: deliverable.case_ids,
        className: "select-case-ids",
        pk: deliverable.id
      },
      {
        contents: deliverable.location,
        className: "textarea",
        pk: deliverable.id
      },
      {
        contents: deliverable.notes,
        className: "textarea",
        pk: deliverable.id
      },
      {
        contents: deliverable.expiry_date,
        className: "date",
        pk: deliverable.id
      }
    ))
  });

  return tableRows;
}

export function deliverablesTable(deliverables: DeliverableObject): HTMLElement {
  const pageContainer = document.createElement("div");
  const pageHeader = document.createElement("h3");
  pageHeader.innerText = "Deliverables Portal";

  const tableHeaders = new Map([
    ["project_id", "Project ID"],
    ["case_id", "Case ID"],
    ["location", "Location"],
    ["notes", "Notes"],
    ["expiry_date", "Expiry Date"]]);

  const table = bootstrapTable(tableHeaders, null, "deliverable-table", true, true, true, true);
  const tableRows = constructTableRows(deliverables);
  const tableBody = tableBodyFromRows(null, tableRows);
  table.appendChild(tableBody);
  table.className = "generic-tables";
  pageContainer.appendChild(pageHeader);
  pageContainer.appendChild(table);
  document.body.appendChild(pageContainer);

  return pageContainer;
}

function getCasesForProject(project: string, project_cases: ProjectCase[]): string[] {
  const cases: string[] = [];
  project_cases.map(project_case => {
    if(project_case.project === project) {
      project_case.cases.forEach((donor_case) => {
        cases.push(donor_case.id);
      })
    }
  })

  return cases;
}

export function initialiseDeliverables() {
  const closeBusy = busyDialog();

  Promise.all([
    fetch("api/deliverables")
  ])
    .then(responses => {
      return Promise.all(responses.map(response => response.json()));
    })
    .then((responses) => {
      const deliverables = responses[0] as DeliverableObject;
      document.body.appendChild(deliverablesTable(deliverables));
      return deliverables;
    })
    .then((deliverables) => {
      const project_cases = deliverables.project_cases;
      const projects: string[] = []
      project_cases.map(project_case => projects.push(project_case.project));

      $(function () {
        $('#deliverables').bootstrapTable({});
        $('#deliverable-table').bootstrapTable({
          exportDataType: 'all'
        });
        //todo: need to set them up as dependent selects
        $('.select-project').editable({
          autotext: 'always', // Allows to automatically set element's text bsaed on its value
          highlight: true, // Highlight cell when value gets updated
          onblur: 'ignore', // Setting ignore allows to have several editing containers open
          type: 'select',
          source: function() {
            return projects;
          },
          //todo: should these be strings?
          success: function(response: string, newValue: string) {
            const cases: string[] = getCasesForProject(newValue, project_cases);
            //todo: unsure if this is how it should accept the source params
            $('.select-case-ids').editable('option', {'source': cases});
            $('.select-case-ids').editable('setValue', null);
          }
        });
        $('.select-case-ids').editable({
          autotext: 'always',
          highlight: true,
          type: 'select',
          onblur: 'ignore',
          sourceError: 'Please select project first.'
        });
        $('.text').editable({
          autotext: 'always',
          highlight: true,
          onblur: 'ignore',
          type: 'text'
        });
        $('.textarea').editable({
          autotext: 'always',
          highlight: true,
          onblur: 'ignore',
          type: 'textarea',
          placement: 'bottom'
        });
        // For case-ids we want something like this To get currently selected items use $.fn.editableutils.itemsByValue(value, sourceData).
        //
        //     display: function(value, sourceData) {
        //        //display checklist as comma-separated values
        //        var html = [],
        //            checked = $.fn.editableutils.itemsByValue(value, sourceData);
        //
        //        if(checked.length) {
        //            $.each(checked, function(i, v) { html.push($.fn.editableutils.escape(v.text)); });
        //            $(this).html(html.join(', '));
        //        } else {
        //            $(this).empty();
        //        }
        //     }
        $('.date').editable({
          autotext: 'always',
          highlight: true,
          onblur: 'ignore',
          format: 'yyyy-MM-dd HH:mm:ss',
          viewformat: 'yyyy-MM-dd HH:mm:ss',
          datetimepicker: {
            todayBtn: 'linked',
            weekStart: 1
          },
          placement: 'bottom'
        });
      })
    })
    .catch((error) => {
      console.log(error); //todo: write to actual logs
    })
    .finally(closeBusy);
}
