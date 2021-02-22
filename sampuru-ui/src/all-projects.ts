import {
  Card,
  projectCard, collapsibleCard, busyDialog,
} from "./html.js";

import {
  fetchAsPromise,
} from "./io.js";
import {ActiveProject} from "./data-transfer-objects.js";

export function activeProjects(projects: ActiveProject[]): HTMLElement {
  const cardContainer = document.createElement("div");
  cardContainer.className = "container";

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

  fetchAsPromise<ActiveProject[]>("api/active_projects", {body: null})
    .then((projects) => {
      document.body.appendChild(activeProjects(projects));
    })//todo: catch errors
    .finally(closeBusy);
}