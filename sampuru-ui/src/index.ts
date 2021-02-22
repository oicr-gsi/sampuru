/// <reference types="jquery" />
/// <reference types="bootstrap" />
/// <reference types="bootstrap-table" />

import {
  busyDialog,
  Card,
  collapsibleCard,
  DOMElement,
  elementFromTag,
  navbar
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
  qcables: SearchedQCable[],
  changelogs: SearchedChangelog[],
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

        //const table = oneColumnTable(project.donor_cases, "donor_case", "Donor Case", project.id + "-donor-cases");

        const table = genericTable<string>(project.donor_cases, project.id + "-donor-cases",
          ["Case", (x) => x]);
        const info = elementFromTag("div", "container project-overview", infoItems, table);
        const infoCard: Card = {contents: info.element, header: "Project Overview: " + project.name, title: project.name, tagId: project.id}
        cards.push(collapsibleCard("projects", null, infoCard, false));
      });
  }

  if (qcables && qcables.length) {
    const table = genericTable<SearchedQCable>(qcables, "qcables",
      ["Type", (x) => x.type],
      ["Status", (x) => x.status],
      ["Library Design", (x) => x.library_design],
      ["Parent ID", (x) => x.parent_id],
      ["OICR Alias", (x) => x.alias]);

    const qcablesTable = elementFromTag("div", "container", table);
    const caseCard: Card = {contents: qcablesTable.element, header: "QCables" , title: "QCables", tagId: "all-qcables"}
    cards.push(collapsibleCard("qcables", null, caseCard, false));
  }

  if (changelogs && changelogs.length) {
    const table = genericTable<SearchedChangelog>(changelogs, "changelogs",
      ["Date", (x) => x.change_date],
      ["Project", (x) => x.project_id],
      ["Case", (x) => x.case_id],
      ["QCable", (x) => x.qcable_id],
      ["Content", (x) => x.content]);

    const changelogsTable = elementFromTag("div", "container", table);
    const changelogCard: Card = {contents: changelogsTable.element, header: "Changelogs", title: "Changelogs", tagId: "changelogs"}
    cards.push(collapsibleCard("changelogs", null, changelogCard, false));
  }

  if (deliverables && deliverables.length) {
    const table = genericTable<DeliverableFile>(deliverables, "deliverables",
      ["Project", (x) => x.project_id],
      ["Location", (x) => x.location],
      ["Notes", (x) => x.notes],
      ["Expiry Date", (x) => x.expiry_date]);

    const deliverablesTable = elementFromTag("div", "container", table);
    const deliverableCard: Card = {contents: deliverablesTable.element, header: "Deliverable Information", title: "Deliverables", tagId: "deliverables"}
    cards.push(collapsibleCard("deliverables", null, deliverableCard, false));
  }

  if (notifications && notifications.length) {
    const table = genericTable<Notification>(notifications, "notifications",
      ["Content", (x) => x.content],
      ["Issue Date", (x) => x.issue_date],
      ["Resolved Date", (x) => x.resolved_date]);

    const notificationsTable = elementFromTag("div", "container", table);
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
      return projects;
    })
    .then((projects) => {
      const tableIds = ["donor_case_qcable", "qcables",
        "changelogs", "deliverables", "notifications"];

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