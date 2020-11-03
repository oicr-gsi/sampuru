import {
  UIElement,
  Card,
  staticCard
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
  const project = projects[0];
  const ui = document.createElement("p");
  const card: Card = {contents: ui, header: project.name, title: project.name, tagId: project.name}

  return staticCard(card);
}