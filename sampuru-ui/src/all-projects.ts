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

  const cards: HTMLElement[] = [];
  const welcomeText = elementFromTag("div",
   null,
   elementFromTag("p", null, "Sampuru is a system for monitoring the quality control ('QC') status of items in the Ontario Institute for Cancer Research ('OICR') labs. You will only be able to see information about samples you provide to OICR, and extractions made from said samples. Sampuru allows you to view information about your projects at varying degrees of granularity and allows you to map the names of your samples to OICR's internal IDs in order to ease communication with OICR. \n\n To access the User Manual about any page, press the '?' on the top bar."),
   elementFromTag("p", null, "Terminology"),
   elementFromTag("p", null, "Case: The collection of: an individual sample received by OICR, and every QCable which is extracted from that sample. A case is considered completed when all of the QCables associated with the case have their QC Status set to Passed QC."),
   elementFromTag("p", null, "QCable: An individual unit of sample processing which can be assessed for quality control. A QCable may be a tissue, an extracted stock, a library aliquot, or something else. The possible QCable statuses are consistently colour-coded throughout Sampuru:"),
   elementFromTag("ul", null, 
    elementFromTag("li", null, "Not yet started: Grey"),
    elementFromTag("li", null, "Pending QC: Yellow"),
    elementFromTag("li", null, "Passed QC: Blue"),
    elementFromTag("li", null, "Failed QC: Red")
   )
   );
  const welcomeCard: Card = {contents: welcomeText.element, header: "Welcome to Sampuru! Click here for help", title: "Help banner", tagId: "welcome-text"};
  cards.push(collapsibleCard("welcome-text", null, welcomeCard, false));

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
    document.body.appendChild(navbar(commonName(response), "all-active-projects"));
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
