/// <reference types="jquery" />
/// <reference types="bootstrap" />
/// <reference types="bootstrap-table" />
/// <reference types="file-saver" />

import {
    bootstrapTable, busyDialog,
    ComplexElement,
    tableBodyFromRows, tableRow,
} from "./html.js";

import {DeliverableObject} from "./data-transfer-objects.js";

initialiseDeliverables();

function constructTableRows(deliverableObjects: DeliverableObject[]): ComplexElement<HTMLTableRowElement>[]{
    const tableRows: ComplexElement<HTMLTableRowElement>[] = [];


       /* Array.from(deliverableObjects).forEach((deliverable) => {
            deliverable.project_cases.forEach((project_case) => {
                project_case.cases.forEach((donor_case) => {
                    deliverable.deliverables.forEach((deliverables) => { */
                        tableRows.push(tableRow(null,
                            {contents: "testDummyData"},
                            {contents: "testDummyData"},
                            {contents: "testDummyData"},
                            {contents: "testDummyData"},
                            {contents: "testDummyData"}))
               /*     })
                })
            })
        }); */

    return tableRows;
}

export function deliverablesTable(deliverables: DeliverableObject[]): HTMLElement {
    const pageContainer = document.createElement("div");
    const pageHeader = document.createElement("h3");
    pageHeader.innerText = "Deliverables Portal";

    const tableHeaders = new Map([
        ["project_id", "Project ID"],
        ["case_id", "Case ID"],
        ["location", "Location"],
        ["notes", "Notes"],
        ["expiry_date", "Expiry Date"]]);

    const table = bootstrapTable(tableHeaders, null, "table", true, true, true, true);
    const tableRows = constructTableRows(deliverables);
    const tableBody = tableBodyFromRows(null, tableRows);
    table.appendChild(tableBody);
    table.className = "deliverables-table";
    pageContainer.appendChild(pageHeader);
    pageContainer.appendChild(table);
    document.body.appendChild(pageContainer);

    return pageContainer;
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
        const deliverables = responses[0] as DeliverableObject[]
        document.body.appendChild(deliverablesTable(deliverables));
        return deliverables;
    })
        .then(() => {

            $(function () {
                $('#deliverables').bootstrapTable({});
                $('#table').bootstrapTable({
                    exportDataType: 'all'
                });
            })
        })
        .catch((error) => {
            console.log(error); //todo: write to actual logs
        })
        .finally(closeBusy);
}
