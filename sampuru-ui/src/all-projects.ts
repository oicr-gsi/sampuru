import {
  Card,
  elementFromTag,
  projectCard, collapsibleCard, busyDialog, navbar
} from "./html.js";
import {commonName} from "./common.js";

import {ActiveProject} from "./data-transfer-objects.js";


export function activeProjects(projects: ActiveProject[]): HTMLElement {
  const cardContainer = document.createElement("div");
  cardContainer.className = "container";
  const welcomeText = document.createElement("div");
  welcomeText.appendChild(elementFromTag("div", null, "contents!")); // This doesn't work, and i can't put an elementFromTag directly into collapsibleCard either, help
  const welcomeCard: Card = {contents: welcomeText, header: "Header!", title: "title!", tagId: ""};
  cardContainer.appendChild(collapsibleCard("projects", null, welcomeCard, false));

  const cards: HTMLElement[] = [];
  projects
    .forEach((project) => {
      const cardContent = projectCard(project);
      const card: Card = {contents: cardContent, header: project.name, title: project.name, tagId: project.id};
      cards.push(collapsibleCard("projects", null, card, true));
    })

  // todo: move this out to a function
  cards
    .forEach((card) => {
      const spacing = document.createElement("br");
      cardContainer.appendChild(spacing);
      cardContainer.appendChild(card);
    })

  return cardContainer;
}


/***
 * Fetch active projects and populate the page
 */
export function initialiseActiveProjects() {
  const closeBusy = busyDialog();

  fetch("api/active_projects", {body: null})
  .then(response => {
    document.body.appendChild(navbar(commonName(response)));
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
  .then((response) => response as ActiveProject[])
  .then((data) => {
    document.body.appendChild(activeProjects(data));
  }).finally(closeBusy);
}
