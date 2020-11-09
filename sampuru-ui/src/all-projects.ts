import {
  UIElement,
  Card,
  staticCard,
  cardContainer, cardContent, collapsibleCard,
} from "./html";

import {
  Project
} from "./io";

export function initialiseActiveProjects(projects: Project[]): HTMLElement {
  const cardContainer = document.createElement("div");
  cardContainer.className = "container";

  const cards: HTMLElement[] = [];
  projects
    .forEach((project) => {
      const card_content = cardContent(project.cases_total, project.cases_completed, project.qcables_total, project.qcables_completed);
      const card: Card = {contents: card_content, header: project.name, title: project.name, tagId: project.name}
      cards.push(collapsibleCard(null, card));
    })

  // todo: move this out to a function
  cards
    .forEach((card) => {
      cardContainer.appendChild(card);
    })

  return cardContainer;
}