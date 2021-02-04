import {busyDialog, Card, caseCard, collapsibleCard, navbar} from "./html.js";
import {fetchAsPromise} from "./io.js";
import {Case} from "./data-transfer-objects.js";

const urlParams = new URLSearchParams(window.location.search);
const projectId = urlParams.get("cases-project-id");

if (projectId) {
  document.body.appendChild(navbar());
  initialiseCases(projectId);
}

export function casesPage(cases: Case[]): HTMLElement {
  const cardContainer = document.createElement("div");
  cardContainer.className = "container";

  const header = document.createElement("h3");
  header.innerText = "Cases";
  cardContainer.appendChild(header);

  const cards: HTMLElement[] = [];
  cases
    .forEach((caseItem) => {
      const cardContent = caseCard(caseItem);
      const card: Card = {contents: cardContent, header: caseItem.name, title: caseItem.name, tagId: caseItem.id};
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

export function initialiseCases(projectId: string) {
  const closeBusy = busyDialog();

  fetchAsPromise<Case[]>("api/cases_cards/" + projectId, {body: null})
    .then((data) => {
      document.body.appendChild(casesPage(data));
    })
    .finally(closeBusy);
}