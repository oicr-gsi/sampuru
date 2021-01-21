import {
  Card,
  projectCard, collapsibleCard, busyDialog,
} from "./html";

import {
  decodeProject,
  fetchAsPromise,
} from "./io";
import {Project, ProjectJSON} from "./data-transfer-objects";

export function activeProjects(projects: Project[]): HTMLElement {
  const cardContainer = document.createElement("div");
  cardContainer.className = "container";

  const cards: HTMLElement[] = [];
  projects
    .forEach((project) => {
      const cardContent = projectCard(project);
      const card: Card = {contents: cardContent, header: project.name, title: project.name, tagId: project.id};
      cards.push(collapsibleCard("active_projects", null, card));
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

  fetchAsPromise<ProjectJSON[]>("api/active_projects", {body: null})
    .then((data) => {
      const projects: Project[] = [];
      data.forEach(proj => projects.push(decodeProject(proj)));
      return projects;
    })
    .then((projects) => {
      document.body.appendChild(activeProjects(projects));
    })//todo: catch errors
    .finally(closeBusy);
}