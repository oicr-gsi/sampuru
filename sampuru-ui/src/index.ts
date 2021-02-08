/// <reference types="jquery" />
/// <reference types="bootstrap" />
/// <reference types="bootstrap-table" />

import {
  bootstrapTable, busyDialog, Card, collapsibleCard,
  ComplexElement, DOMElement,
  elementFromTag,
  navbar,
  tableBodyFromRows,
  TableCell,
  tableRow
} from "./html.js";
import {initialiseActiveProjects} from "./all-projects.js";
import {urlConstructor} from "./io.js";
import {
  DeliverableFile,
  Notification,
  SearchedChangelog,
  SearchedProject,
  SearchedQCable
} from "./data-transfer-objects.js";


const urlParams = new URLSearchParams(window.location.search);
const search = urlParams.get("search");

if (search) {
  document.body.appendChild(navbar());
  defaultSearch(search);
} else {
  document.body.appendChild(navbar());
  initialiseActiveProjects();
}

/**
 * Sampuru's global search
 * */
export function genericTable<T>(
  headers: Map<string, string>,
  rows: T[],
  tableId: string
) {
  const tableRows: ComplexElement<HTMLTableRowElement>[] = [];
  rows
    .forEach((row) => {
      const cells: TableCell[] = [];
      Object.values(row).map(x => cells.push({contents: x.toString()}));
      tableRows.push(tableRow(null, ...cells)); //todo: this probably doesn't work
    });

  const table = bootstrapTable(headers, true, true, tableId);
  const tableBody = tableBodyFromRows(null, tableRows);
  table.appendChild(tableBody);

  return elementFromTag("div", null, {type: "complex", element: table});
}

export function oneColumnTable(
  rows: string[],
  headerDataField: string,
  header: string,
  tableId: string
) {
  const tableRows: ComplexElement<HTMLTableRowElement>[] = [];
  rows
    .forEach((row) => {
      let content;
      if (headerDataField == "donor_case") {
        content = document.createElement("a");
        content.href = urlConstructor("qcables.html",
          ["qcables-filter-type", "qcables-filter-id"], ["case", row]);
        content.innerText = row;
      } else {
        content = document.createElement("p");
        content.innerText = row;
      }
      tableRows.push(tableRow(null, {contents: {type: "complex", element: content}}))
    });

  const tableHeader = new Map([[headerDataField, header]]);
  const table = bootstrapTable(tableHeader, true, true, tableId);
  const tableBody = tableBodyFromRows(null, tableRows);
  table.appendChild(tableBody);

  return elementFromTag("div", null, {type: "complex", element: table});
}

export function defaultSearchResults(
  searchString: string,
  projects: SearchedProject[],
  qcables: SearchedQCable[],
  changelogs: SearchedChangelog[],
  notifications: Notification[],
  deliverables: DeliverableFile[]
): HTMLElement {
  const cards : HTMLElement[] = [];

  if (projects && projects.length) {
    const infoItems: DOMElement[] = [];
    projects
      .forEach((project) => {
        //todo: refactor this as it's currently duplicate code from project.ts
        infoItems.push(elementFromTag("div", "row",
          elementFromTag("b", null, "Name: "),
          elementFromTag("p", null,  project.name)));

        if(project.created_date != "null") {
          infoItems.push(elementFromTag("div", "row",
            elementFromTag("b", null, "Creation Date: "),
            elementFromTag("p", null,  project.created_date)));
        }

        if(project.description != "null") {
          infoItems.push(elementFromTag("div", "row",
            elementFromTag("b", null, "Description: "),
            elementFromTag("p", null,  project.description)));
        }

        if(project.pipeline != "null") {
          infoItems.push(elementFromTag("div", "row",
            elementFromTag("b", null, "Pipeline: "),
            elementFromTag("p", null,  project.pipeline)));
        }

        if(project.kits != "[]") {
          infoItems.push(elementFromTag("div", "row",
            elementFromTag("b", null, "Kits: "),
            elementFromTag("p", null,  project.kits.replace(/[\[\]']+/g, ''))));
        }

        infoItems.push(elementFromTag("div", "row",
          elementFromTag("b", null, "Cases: "),
          {type: "a", href: urlConstructor("cases.html", ["cases-project-id"], [project.name]),
          innerText: "All", className: "", title: "All Cases for " + project.name}))

        const table = oneColumnTable(project.donor_cases, "donor_case", "Donor Case", "project_donor_case");
        const info = elementFromTag("div", "container project-overview", infoItems, table);
        const infoCard: Card = {contents: info.element, header: "Project Overview: " + project.name, title: project.name, tagId: project.id}
        cards.push(collapsibleCard("projects", null, infoCard, false));
      });
  }

  if (qcables && qcables.length) {
    const headers = new Map([
      ["type", "Type"],
      ["status", "Status"],
      ["library_design", "Library Design"],
      ["parent_id", "Parent ID"],
      ["alias", "OICR Alias"]]);
    const table = genericTable<SearchedQCable>(headers, qcables, "qcables");
    const caseCard: Card = {contents: table.element, header: "QCables" , title: "QCables", tagId: "all-qcables"}
    cards.push(collapsibleCard("qcables", null, caseCard, false));
  }

  //todo: likely genericTable function is broken
  //todo: don't need to display all these columns??
  if (changelogs && changelogs.length) {
    const headers = new Map([
      ["id", "ID"],
      ["change_date", "Date"],
      ["project_id", "Project"],
      ["case_id", "Case"],
      ["qcable_id", "QCable"],
      ["content", "Content"]
    ]);
    const table = genericTable<SearchedChangelog>(headers, changelogs, "changelogs");
    const changelogCard: Card = {contents: table.element, header: "Changelogs", title: "Changelogs", tagId: "changelogs"}
    cards.push(collapsibleCard("changelogs", null, changelogCard, false));
  }

  //todo: don't need to display id
  if (deliverables && deliverables.length) {
    const headers = new Map([
      ["id", "ID"],
      ["project_id", "Project"],
      ["location", "Location"],
      ["notes", "Notes"],
      ["expiry_date", "Expiry Date"]
    ]);
    const table = genericTable<DeliverableFile>(headers, deliverables, "deliverables");
    const deliverableCard: Card = {contents: table.element, header: "Deliverable Information", title: "Deliverables", tagId: "deliverables"}
    cards.push(collapsibleCard("deliverables", null, deliverableCard, false));
  }

  //todo: can probably filter out some of these columns??
  if (notifications && notifications.length) {
    const headers = new Map([
      ["id", "ID"],
      ["content", "Content"],
      ["issue_date", "Issue Date"],
      ["resolved_date", "Resolved Date"],
      ["user_id", "User"]
    ]);
    const table = genericTable<Notification>(headers, notifications, "notifications");
    const notificationsCard: Card = {contents: table.element, header: "Notifications", title: "Notifications", tagId: "notifications"}
    cards.push(collapsibleCard("notifications", null, notificationsCard, false));
  }

  const cardContainer = document.createElement("div");
  cardContainer.className = "container";

  cards
    .forEach((card) => {
      const spacing = document.createElement("br");
      cardContainer.appendChild(spacing);
      cardContainer.appendChild(card);
    })

  const search = elementFromTag("div", null,
    elementFromTag("h3", null, "Search Results: " + searchString),
    {type: "complex", element: cardContainer});

  return search.element;
}


export function defaultSearch(searchString: string) {
  const closeBusy = busyDialog();

  Promise.all([
    fetch("api/search/project/" + searchString),
    fetch("api/search/qcable/" + searchString),
    fetch("api/search/changelog/" + searchString),
    fetch("api/search/notification/" + searchString),
    fetch("api/search/deliverable/" + searchString)
  ])
    .then(responses => Promise.all(responses.map(response => response.json())))
    .then((responses) => {
      const projects = responses[0] as SearchedProject[];
      const qcables = responses[1] as SearchedQCable[];
      const changelogs = responses[2] as SearchedChangelog[];
      const notifications = responses[3] as Notification[];
      const deliverables = responses[4] as DeliverableFile[];

      document.body.appendChild(defaultSearchResults(searchString, projects, qcables, changelogs, notifications, deliverables));
    })
    .then(() => {
      //todo: when should this callback happen??
      //todo: can call this function with a map of table_ids and no search texts

      const tableIds = ["project_donor_case", "donor_case_qcable", "qcables",
        "changelogs", "deliverables", "notifications"];

      tableIds.map(id => {
        $(function () {
          $(`#${id}`).bootstrapTable({});
        });
      });
    })
    .catch((error) => {
      console.log(error); // todo: log this somewhere permanent
    })
    .finally(closeBusy);
}