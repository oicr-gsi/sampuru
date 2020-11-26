import {busyDialog, Card, cardContent, elementFromTag, staticCard} from "./html";
import {fetchAsPromise} from "./io";
import {ProjectInfo} from "./data-transfer-objects";

export function projectOverview(projectInfo: ProjectInfo[]): HTMLElement {
  const pageContainer = document.createElement("div");
  const pageHeader = document.createElement("h2");
  pageHeader.innerText = "Project Overview"

  const cardContainter = document.createElement("div");
  cardContainter.className = "container";

  const cards: HTMLElement[] = [];
  projectInfo
    .forEach((projectInfo) => {
      const info = elementFromTag("p", null, projectInfo.info_items.join("\n")); //todo: should info_items be a map once there is data?
      const infoCard: Card = {contents: info.element, header: "Project Information",
        title: projectInfo.name + " Overview", tagId: projectInfo.name + "-overview"};

      const contact = elementFromTag("p", null, projectInfo.contact_name + "\n" + projectInfo.contact_email);
      const contactCard: Card = {contents: contact.element, header: "Contact Information",
        title: projectInfo.name + " Contact Information", tagId: projectInfo.name + "-contact"};

      const qcables = elementFromTag("p", null, projectInfo.sankey_transitions[Symbol.toStringTag]);
      const qcablesCard: Card = {contents: qcables.element, header: "QCables",
        title: projectInfo.name + " QCables", tagId: projectInfo.name + "-qcables"};

      const failures = elementFromTag("div", null, projectInfo.failures.join("\n"));
      const failuresCard: Card = {contents: failures.element, header: "Failures",
        title: projectInfo.name + " Failures", tagId: projectInfo.name + "-failures"};

      const deliverables = elementFromTag("div", null, projectInfo.deliverables.join("\n"));
      const deliverablesCard: Card = {contents: deliverables.element, header: "Files",
        title: projectInfo.name + " Files", tagId: projectInfo.name + "-files"};

      cards.push(staticCard(infoCard));
      cards.push(staticCard(contactCard));
      cards.push(staticCard(qcablesCard));
      cards.push(staticCard(failuresCard));
      cards.push(staticCard(deliverablesCard));
    })

  cards
    .forEach((card) => {
      const spacing = document.createElement("br");
      cardContainter.appendChild(spacing);
      cardContainter.appendChild(card);
    })

  pageContainer.appendChild(pageHeader);
  pageContainer.appendChild(cardContainter);
  return pageContainer;
}

export function initialiseProjectOverview(project_id: number) {
  const closeBusy = busyDialog();

  fetchAsPromise<ProjectInfo[]>("api/project_overview/" + project_id.toString(), {body: null})
    .then((data) => {
      document.body.appendChild(projectOverview(data));
    })
    .finally(closeBusy);
}