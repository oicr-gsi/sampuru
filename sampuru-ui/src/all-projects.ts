import {
  UIElement,
  Card,
  staticCard,
  cardContainer,
} from "./html";

export type Project = {
  id: number,
  name: string,
  casesTotal: number,
  casesCompleted: number,
  qcAblesTotal: number,
  qcAblesCompleted: number,
  completionDate: number
}

export function initialiseActiveProjects(projects: Project[]): HTMLElement {
  const cardContainer = document.createElement("div");
  cardContainer.className = "container";
  const ui = document.createElement("p");

  const cards: HTMLElement[] = [];
  projects
    .forEach((project) => {
      const card: Card = {contents: ui, header: project.name, title: project.name, tagId: project.name}
      cards.push(staticCard(card));
    })

  // todo: move this out to a function
  cards
    .forEach((card) => {
      cardContainer.appendChild(card);
    })

  return cardContainer;
}