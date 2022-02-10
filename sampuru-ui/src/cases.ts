import {busyDialog, Card, caseCard, collapsibleCard, createLinkElement, navbar, createButton} from "./html.js";
import {CaseCard} from "./data-transfer-objects.js";
import {commonName} from "./common.js";
import {urlConstructor} from "./io.js";

const urlParams = new URLSearchParams(window.location.search);
const projectId = urlParams.get("cases-project-id");
const identifier = urlParams.get("identifier");

if (projectId && identifier) {
  initialiseCases(projectId, identifier);
}

function formatCardHeader(caseItem: CaseCard, identifier: string): string {
  if (identifier == "external") {
    return caseItem.name.replace(':','\t');
  } else {
    return caseItem.id.replace(':', '\t');
  }

}

export function casesPage(cases: CaseCard[], project: string, identifier: string): HTMLElement {
  const cardContainer = document.createElement("div");
  cardContainer.className = "container";

  const header = document.createElement("h3");
  header.innerText = "Cases (";
  const projectLink = createLinkElement(
    null,
    project,
    null,
    null,
    urlConstructor("project.html", ["project-overview-id"], [project]));
  header.appendChild(projectLink);
  header.innerHTML += ")";
  cardContainer.appendChild(header);

  const toggleIds = createButton('toggle-case-ids',
    identifier === "external" ? "Switch to OICR Identifiers" : "Switch to External Identifiers",
    "case-identifier");

  toggleIds.onclick = function() {
    if(identifier === "external") {
      window.location.href = urlConstructor("cases.html", ["cases-project-id", "identifier"], [project, "internal"]);
    } else {
      window.location.href = urlConstructor("cases.html", ["cases-project-id", "identifier"], [project, "external"]);
    }
  }

  cardContainer.appendChild(toggleIds);

  const cards: HTMLElement[] = [];
  cases
    .forEach((caseItem) => {
      const cardContent = caseCard(caseItem);
      const card: Card = {contents: cardContent, header: formatCardHeader(caseItem, identifier), title: caseItem.name, cardId: caseItem.id};
      cards.push(collapsibleCard("cases", null, card, true));
    });

  cards
    .forEach((card) => {
      const spacing = document.createElement("br");
      cardContainer.appendChild(spacing);
      cardContainer.appendChild(card);
    })

  return cardContainer;
}

export function initialiseCases(projectId: string, identifier: string) {
  const closeBusy = busyDialog();
  fetch("api/cases_cards/" + projectId, {body: null})
    .then(response => {
      document.body.appendChild(navbar(commonName(response), "cases"));
      if (response.ok) {
        return Promise.resolve(response.json());
      } else if (response.status == 503) {
        return Promise.reject(new Error("Sampuru is currently overloaded."));
      } else {
        return Promise.reject(
          new Error(`Failed to load: ${response.status} ${response.statusText}`)
        );
      }
    })
    .then((response) => response as CaseCard[])
    .then((data) => {
      document.body.appendChild(casesPage(data, projectId, identifier));
    }).finally(closeBusy);
}
