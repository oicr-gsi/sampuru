import {busyDialog, Card, caseCard, collapsibleCard, navbar} from "./html.js";
import {CaseCard} from "./data-transfer-objects.js";
import {commonName} from "./common.js";

const urlParams = new URLSearchParams(window.location.search);
const projectId = urlParams.get("cases-project-id");

if (projectId) {
  initialiseCases(projectId);
}

export function casesPage(cases: CaseCard[]): HTMLElement {
  const cardContainer = document.createElement("div");
  cardContainer.className = "container";

  const header = document.createElement("h3");
  header.innerText = "Cases";
  cardContainer.appendChild(header);

  const cards: HTMLElement[] = [];
  cases
    .forEach((caseItem) => {
      const cardContent = caseCard(caseItem);
      const card: Card = {contents: cardContent, header: caseItem.name.replace(':','\t'), title: caseItem.name, cardId: caseItem.id};
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
      document.body.appendChild(casesPage(data));
    }).finally(closeBusy);
}
