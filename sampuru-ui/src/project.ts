/// <reference types="jquery" />
/// <reference types="bootstrap" />
/// <reference types="bootstrap-table" />

import {
  busyDialog,
  Card,
  elementFromTag,
  staticCard,
  navbar,
  DOMElement, progressBar,
  createLinkElement, ComplexElement, tableRow, bootstrapTable, tableBodyFromRows
} from "./html.js";
import {fetchAsPromise, urlConstructor} from "./io.js";
import {Case, BaseChangelog, ProjectInfo, Changelog} from "./data-transfer-objects.js";
import { drawSankey } from "./sankey.js";

const urlParams = new URLSearchParams(window.location.search);
const projectId = urlParams.get("project-overview-id");

if(projectId) {
  document.body.appendChild(navbar());
  initialiseProjectOverview(projectId);
}


export function failedChangelog(changelogContent: string): string {
  if(changelogContent.includes("fail")) {
    return "table-danger";
  } else {
    return "";
  }
}

export function changelogTable(changelogs: Changelog[]): ComplexElement<HTMLElement> {
  const tableRows: ComplexElement<HTMLTableRowElement>[] = [];

  changelogs
    .forEach((changelog) => {
      tableRows.push(tableRow(null,
        {
          contents: changelog.case_id,
          className: failedChangelog(changelog.content)
        },
        {
          contents: changelog.content,
          className: failedChangelog(changelog.content)
        },
        {
          contents: changelog.change_date,
          className: failedChangelog(changelog.content)
        }))
    });

  const tableHeaders = new Map([
    ["donor_case_name", "Case"],
    ["content", "Content"],
    ["change_date", "Change Date"]
  ]);

  const table = bootstrapTable(tableHeaders, true, true, "project-changelog");
  const tableBody = tableBodyFromRows(null, tableRows); //todo: want to apply css to this table?

  table.appendChild(tableBody);
  table.className = "changelog-table";

  return {type: "complex", element: table};
}


export function project(projectInfo: ProjectInfo, changelogs: Changelog[]): HTMLElement {
  const pageContainer = document.createElement("div");
  const pageHeader = document.createElement("h2");
  pageHeader.innerText = projectInfo.name;

  const cardContainter = document.createElement("div");
  cardContainter.className = "container";

  const cards: HTMLElement[] = [];
  const infoItems: DOMElement[] = [];

  infoItems.push(elementFromTag("div", "row",
    elementFromTag("b", null, "Name: "),
    elementFromTag("p", null,  projectInfo.name)));

  if(projectInfo.created_date != "null") {
    infoItems.push(elementFromTag("div", "row",
      elementFromTag("b", null, "Creation Date: "),
      elementFromTag("p", null,  projectInfo.created_date)));
  }

  if(projectInfo.description != "null") {
    infoItems.push(elementFromTag("div", "row",
      elementFromTag("b", null, "Description: "),
      elementFromTag("p", null,  projectInfo.description)));
  }

  if(projectInfo.pipeline != "null") {
    infoItems.push(elementFromTag("div", "row",
      elementFromTag("b", null, "Pipeline: "),
      elementFromTag("p", null,  projectInfo.pipeline)));
  }

  if(projectInfo.kits != "[]") {
    infoItems.push(elementFromTag("div", "row",
      elementFromTag("b", null, "Kits: "),
      elementFromTag("p", null,  projectInfo.kits.replace(/[\[\]']+/g, ''))));
  }

  projectInfo.info_items.forEach((item) => {
    if (item.content != null) {
      infoItems.push(elementFromTag("div", "row",
        elementFromTag("b", null, item.entry_type + ": "),
        elementFromTag("p", null,  item.content)));
    }
  });

  const info = elementFromTag("div", "container project-overview", infoItems);
  const infoCard: Card = {contents: info.element, header: "Project Information",
    title: projectInfo.name + " Overview", tagId: projectInfo.name + "-overview"};


  const contact = elementFromTag("div", "contact row",
    elementFromTag("div", "col-6",
      elementFromTag("b", null, "Name: "),
      elementFromTag("p", null, (projectInfo.contact_name == "null") ? "None" : projectInfo.contact_name)),
    elementFromTag("div", "col-6",
      elementFromTag("b", null, "Email: "),
      elementFromTag("p", null, (projectInfo.contact_email == "null") ? "None" : projectInfo.contact_email)));


  const contactCard: Card = {contents: contact.element, header: "Contact Information",
    title: projectInfo.name + " Contact Information", tagId: projectInfo.name + "-contact"};

  const qcablesLink = createLinkElement(
    null,
    "QCables",
    null,
    null,
    urlConstructor("qcables.html", ["qcables-filter-type", "qcables-filter-id"], ["project", projectInfo.name])

  );

  const qcables = elementFromTag("div", null,
    {type: "complex", element: qcablesLink},
    {type: "complex", element: progressBar(projectInfo.qcables_total, projectInfo.qcables_completed)});

  const casesLink = createLinkElement(
    null,
    "Cases",
    null,
    null,
    urlConstructor("cases.html", ["cases-project-id"], [projectInfo.name])
  );

  const cases = elementFromTag("div", null,
    {type: "complex", element: casesLink},
    {type: "complex", element: progressBar(projectInfo.cases_total, projectInfo.cases_completed)});

  const projectSummary = elementFromTag("div", "row",
    elementFromTag("div", "col col-md-8", {type: "complex", element: staticCard(infoCard)}),
    elementFromTag("div", "col col-md-4",
      {type: "complex", element: staticCard(contactCard)},
      {type: "complex", element: qcables.element},
      {type: "complex", element: cases.element}));

  const sankeyContainer = document.createElement("div");
  sankeyContainer.id = "sankey";
  const qcablesCard: Card = {contents: sankeyContainer, header: "QCables",
    title: projectInfo.name + " QCables", tagId: projectInfo.name + "-qcables"};

  const table = elementFromTag("div", null, changelogTable(changelogs));
  const changelogsCard: Card = {contents: table.element, header: "Changelogs",
    title: projectInfo.name + " Changelogs", tagId: projectInfo.name + "-changelogs"};

  const deliverables = elementFromTag("div", null, projectInfo.deliverables.join("\n"));
  const deliverablesCard: Card = {contents: deliverables.element, header: "Files",
    title: projectInfo.name + " Files", tagId: projectInfo.name + "-files"};

  cards.push(projectSummary.element);
  cards.push(staticCard(qcablesCard));
  cards.push(staticCard(changelogsCard));
  cards.push(staticCard(deliverablesCard));

  cards
    .forEach((card) => {
      const spacing = document.createElement("br");
      cardContainter.appendChild(spacing);
      cardContainter.appendChild(card);
    });

  pageContainer.appendChild(pageHeader);
  pageContainer.appendChild(cardContainter);
  return pageContainer;
}


export function initialiseProjectOverview(projectId: string) {
  const closeBusy = busyDialog();

  Promise.all([
    fetch("api/project_overview/" + projectId),
    fetch("api/changelogs/project/" + projectId)
  ])
    .then(responses => Promise.all(responses.map(response => response.json())))
    .then((responses) => {
      const projectInfo = responses[0] as ProjectInfo
      const changelogs = responses[1] as Changelog[]

      document.body.appendChild(project(projectInfo, changelogs));
      return projectInfo;
    })
    .then((data) => {
      drawSankey(data.sankey_transitions);
    })
    .then(() => {
      $(function () {
        $('#project-changelog').bootstrapTable({});
      });
    })
    .catch((error) => {
      console.log(error); //todo: write to actual logs
    })
    .finally(closeBusy);

  /*
  fetchAsPromise<ProjectInfo>("api/project_overview/" + projectId, {body: null})
    .then((data) => {
      document.body.appendChild(project(data));
      return data;
    })
    .then((data) => {
      drawSankey(data.sankey_transitions);
    })
    .finally(closeBusy);*/
}