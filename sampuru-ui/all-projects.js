import { staticCard } from "./html";
export function initialiseActiveProjects(projects) {
    const project = projects[0];
    const ui = document.createElement("p");
    const card = { contents: ui, header: project.name, title: project.name, tagId: project.name };
    return staticCard(card);
}
