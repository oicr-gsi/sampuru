import {
  busyDialog,
  Card,
  elementFromTag,
  staticCard,
  navbar,
  DOMElement, progressBar
} from "./html.js";
import {fetchAsPromise, constructURL} from "./io.js";
import { ProjectInfo } from "./data-transfer-objects.js";
import { drawSankey } from "./sankey.js";


const urlParams = new URLSearchParams(window.location.search);
const projectId = urlParams.get("project-overview-id");

if(projectId) {
  document.body.appendChild(navbar());
  initialiseProjectOverview(projectId);
}

export function project(projectInfo: ProjectInfo): HTMLElement {
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
    if (item.content != "null") {
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

  const qcablesLink = document.createElement("a");
  qcablesLink.innerText = "QCables";
  qcablesLink.href = constructURL(
    "qcables.html", ["qcables-filter-type", "qcables-filter-id"],
    ["project", projectInfo.name], "QCables for project " + projectInfo.name);

  const qcables = elementFromTag("div", null,
    {type: "complex", element: qcablesLink},
    {type: "complex", element: progressBar(projectInfo.qcables_total, projectInfo.qcables_completed)});

  const casesLink = document.createElement("a");
  casesLink.innerText = "Cases";
  casesLink.href = constructURL(
    "cases.html", ["cases-project-id"],
    [projectInfo.name], "Cases for project " + projectInfo.name);

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

  const failures = elementFromTag("div", null, projectInfo.failures.join("\n"));
  const failuresCard: Card = {contents: failures.element, header: "Failures",
    title: projectInfo.name + " Failures", tagId: projectInfo.name + "-failures"};

  const deliverables = elementFromTag("div", null, projectInfo.deliverables.join("\n"));
  const deliverablesCard: Card = {contents: deliverables.element, header: "Files",
    title: projectInfo.name + " Files", tagId: projectInfo.name + "-files"};

  cards.push(projectSummary.element);
  cards.push(staticCard(qcablesCard));
  cards.push(staticCard(failuresCard));
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


export function initialiseProjectOverview(project_id: string) {
  const closeBusy = busyDialog();

  fetchAsPromise<ProjectInfo>("api/project_overview/" + project_id, {body: null})
    .then((data) => {
      document.body.appendChild(project(data));
      return data;
    })
    .then((data) => {
      drawSankey(data.sankey_transitions);
    })
    .finally(closeBusy);
}