/// <reference types="jquery" />
/// <reference types="bootstrap" />
/// <reference types="bootstrap-table" />

import {
  busyDialog,
  Card,
  collapsibleCard, createLinkElement,
  DOMElement,
  elementFromTag,
  navbar
} from "./html.js";
import {initialiseActiveProjects} from "./all-projects.js";
import {urlConstructor} from "./io.js";
import {
  Changelog,
  DeliverableFile,
  Notification,
  SearchedCase,
  SearchedProject,
  SearchedQCable
} from "./data-transfer-objects.js";
import {
  commonName, 
  formatLibraryDesigns, 
  formatQualityGateNames
} from "./common.js";


const urlParams = new URLSearchParams(window.location.search);
const search = urlParams.get("search");

if (search) {
  defaultSearch(search);
} else {
  document.body.appendChild(navbar("boo"));
  initialiseActiveProjects();
}

/**
 * Sampuru's global search
 * */

export function genericTable<T>(
  rows: T[],
  tableId: string,
  ...headers: [DOMElement, (value: T) => DOMElement][]
) {
  if (rows.length == 0) return [];
  const table =  elementFromTag(
    "table",
    null,
    elementFromTag(
      "thead",
      null,
      elementFromTag(
        "tr",
        null,
        headers.map(([name, _func]) => {
          const header = elementFromTag("th", null, name);
          if (typeof name === "string") {
            header.element.setAttribute("data-field", name.replace(/ /g, '_'));
          }
          return header;
        })
      )
    ),
    elementFromTag(
      "tbody",
      null,
      rows.map((row) =>
        elementFromTag(
          "tr",
          null,
          headers.map(([_name, func]) => elementFromTag("td", null, func(row)))
        )
      )
    )
  );

  table.element.id = tableId;
  table.element.setAttribute("data-toggle", "table");
  table.element.setAttribute("data-pagination", "true");
  table.element.setAttribute("data-search", "true");
  return table;
}

export function defaultSearchResults(
  searchString: string,
  projects: SearchedProject[],
  cases: SearchedCase[],
  qcables: SearchedQCable[],
  changelogs: Changelog[],
  notifications: Notification[],
  deliverables: DeliverableFile[]
): HTMLElement {
  const cards : HTMLElement[] = [];

  if (projects && projects.length) {
    projects
      .forEach((project) => {
        const infoItems: DOMElement[] = [];
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

        const info = elementFromTag("div", "container project-overview", infoItems);
        const infoCard: Card = {contents: info.element, header: "Project Overview: " + project.name, title: project.name, tagId: project.id}
        cards.push(collapsibleCard("projects", null, infoCard, false));
      });
  }

  //todo: make text in search result cards smaller
  if (cases && cases.length) {
    const table = genericTable<SearchedCase>(cases, "donor-case-qcables",
      ["Case Name", (x) => {
      return {type: "complex", element: createLinkElement(
          null,
          x.name,
          null,
          null,
          urlConstructor("qcables.html", ["qcables-filter-type", "qcables-filter-id"], ["case", x.id])
        )}
      }],
      ["QCables", (x) => {
      return elementFromTag("div", null,
        x.qcables.map((qcable) => {
          return elementFromTag("div", null, qcable, null);
        }));
      }]
    );

    const casesTable = elementFromTag("div", "container generic-tables", table);
    const casesCard: Card = {contents: casesTable.element, header: "Cases", title: "Donor Cases", tagId: "donor_cases"}
    cards.push(collapsibleCard("donor_cases", null, casesCard, false));
  }

  if (qcables && qcables.length) {
    const table = genericTable<SearchedQCable>(qcables, "qcables",
      ["ID", (x) => x.id],
      ["OICR Alias", (x) => x.alias],
      ["Type", (x) => formatQualityGateNames(x.type)],
      ["Status", (x) => x.status.charAt(0).toUpperCase() + x.status.slice(1)],
      ["Library Design", (x) => formatLibraryDesigns(x.library_design)],
      ["Parent ID", (x) => x.parent_id == "null" ? "None" : x.parent_id]);

    const qcablesTable = elementFromTag("div", "container generic-tables", table);
    const caseCard: Card = {contents: qcablesTable.element, header: "QCables" , title: "QCables", tagId: "all-qcables"}
    cards.push(collapsibleCard("qcables", null, caseCard, false));
  }

  if (changelogs && changelogs.length) {
    const table = genericTable<Changelog>(changelogs, "searched-changelogs",
      ["Date", (x) => x.change_date],
      ["Content", (x) => x.content]);

    const changelogsTable = elementFromTag("div", "container generic-tables", table);
    const changelogCard: Card = {contents: changelogsTable.element, header: "Changelogs", title: "Changelogs", tagId: "changelogs"}
    cards.push(collapsibleCard("changelogs", null, changelogCard, false));
  }

  if (deliverables && deliverables.length) {
    const table = genericTable<DeliverableFile>(deliverables, "deliverables",
      ["Project", (x) => x.project_id],
      ["Location", (x) => x.location],
      ["Notes", (x) => x.notes],
      ["Expiry Date", (x) => x.expiry_date]);

    const deliverablesTable = elementFromTag("div", "container generic-tables", table);
    const deliverableCard: Card = {contents: deliverablesTable.element, header: "Deliverable Information", title: "Deliverables", tagId: "deliverables"}
    cards.push(collapsibleCard("deliverables", null, deliverableCard, false));
  }

  if (notifications && notifications.length) {
    const table = genericTable<Notification>(notifications, "notifications",
      ["Content", (x) => x.content],
      ["Issue Date", (x) => x.issue_date],
      ["Resolved Date", (x) => x.resolved_date]);

    const notificationsTable = elementFromTag("div", "container generic-tables", table);
    const notificationsCard: Card = {contents: notificationsTable.element, header: "Notifications", title: "Notifications", tagId: "notifications"}
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
    fetch("api/search/case/" + searchString),
    fetch("api/search/qcable/" + searchString),
    fetch("api/search/changelog/" + searchString),
    fetch("api/search/notification/" + searchString),
    fetch("api/search/deliverable/" + searchString)
  ])
    .then(responses => {
      document.body.appendChild(navbar(commonName(responses[0])));
      return Promise.all(responses.map(response => response.json()))
    })
    .then((responses) => {
      const projects = responses[0] as SearchedProject[];
      const cases = responses[1] as SearchedCase[];
      const qcables = responses[2] as SearchedQCable[];
      const changelogs = responses[3] as Changelog[];
      const notifications = responses[4] as Notification[];
      const deliverables = responses[5] as DeliverableFile[];

      document.body.appendChild(defaultSearchResults(searchString, projects, cases, qcables, changelogs, notifications, deliverables));
      const tableIds = ["qcables", "searched-changelogs", "deliverables", "notifications", "donor-case-qcables"];
      projects.map(project => tableIds.push(project.id + "-donor-cases"));

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