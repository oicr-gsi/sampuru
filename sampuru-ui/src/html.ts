
/**
 * The callback for handling mouse events
 */
export type ClickHandler = (e: MouseEvent) => void;

export interface ComplexElement<T extends HTMLElement> {
  element: T
}

// @ts-ignore
export type UIElement =
  | UIElement[]
  | string
  | number
  | ComplexElement<HTMLElement>;

/**
 * The contents of a card
 */
export interface Card {
  /**
   * A callback if the cell is clicked
   * */
  click?: ClickHandler;
  /**
   * The elements that should be in the card body
   * */
  contents: HTMLElement;

  /**
   * The content of the card header
   * */
  header: string;

  /**
   * A tooltip for the card
   * */
  title?: string;

  /**
   * ID for children elements of this card
   * */
  tagId: string;
}

export function navbar():HTMLElement {
  // horizontal navbar that becomes vertical on small screens
  const nav = document.createElement("nav");
  nav.className = "navbar navbar-expand-sm bg-light navbar-light";

  const sampuru = document.createElement("a");
  sampuru.className = "navbar-brand";
  sampuru.innerText = "Sampuru";
  sampuru.href = "#";

  const searchForm = document.createElement("form");
  searchForm.className = "form-inline mx-lg-auto";

  const inputBox = document.createElement("input");
  inputBox.className = "form-control mr-sm-2";
  inputBox.type = "search";
  inputBox.placeholder = "Search";

  const submitButton = document.createElement("button");
  submitButton.className = "btn btn-outline-secondary my-2 my-sm-0";
  submitButton.type = "submit";
  submitButton.innerText = "Search";

  searchForm.appendChild(inputBox);
  searchForm.appendChild(submitButton);

  nav.appendChild(sampuru);
  nav.appendChild(searchForm);

  return nav;
}

export function createLinkElement(
  className: string,
  innerText: string,
  attributes: Map<string, string> | null,
  url: string | null
): HTMLElement {
  const link = document.createElement("a");
  link.className = className;
  link.innerText = innerText;
  link.target = "_blank";
  url ? link.href = url : null;
  if(attributes) {
    attributes.forEach( (value, qualifiedName) => link.setAttribute(qualifiedName, value));
  }
  return link;
}

export function collapsibleCard(
  click: ClickHandler | null,
  content: Card
): HTMLElement {

  let attributes = new Map();
  attributes.set('data-toggle', 'collapse');
  attributes.set('href', `#${content.tagId}`);

  const cardLink = createLinkElement("card-link", content.header, attributes, null);

  const cardHeader = document.createElement("div");
  cardHeader.className = "card-header";
  cardHeader.appendChild(cardLink);

  const cardBodyInner = document.createElement("div");
  cardBodyInner.className = "card-body";
  cardBodyInner.appendChild(content.contents);

  const cardBody = document.createElement("div");
  cardBody.id = `#${content.tagId}`;
  cardBody.className = "collapse show";
  cardBody.appendChild(cardBodyInner);

  const card = document.createElement("div");
  card.className = "card";
  card.appendChild(cardHeader);
  card.appendChild(cardBody);
  return card;
}

export function staticCard(
  content: Card
): HTMLElement {
  //todo: populate url with path to project info page
  const cardHeaderLink = createLinkElement("card-link", content.header, null, "project-link");

  const cardHeader = document.createElement("div");
  cardHeader.className = "card-header";
  cardHeader.appendChild(cardHeaderLink);

  const cardBody = document.createElement("div");
  cardBody.className = "card-body";
  cardBody.appendChild(content.contents);

  const card = document.createElement("div");
  card.className = "card";
  card.appendChild(cardHeader);
  card.appendChild(cardBody);
  return card;
}

export function progressBar(
  total: number,
  completed: number
): HTMLElement {
  //todo: bring css out to a separate file
  const progress = document.createElement("div");
  progress.className = "progress";
  progress.setAttribute("style", "position:relative");

  const progressBar = document.createElement("div");
  progressBar.className = "progress-bar bg-success";
  const casesPercentCompleted = Math.floor((completed/total) * 100);
  progressBar.setAttribute("style", "width:" + casesPercentCompleted.toString() + "%");

  const progressText = document.createElement("div");
  progressText.className = "progress-text"
  progressText.innerText = completed.toString() + "/" + total.toString() + " Completed";
  progressText.setAttribute("style", "position: absolute; line-height: 1rem; text-align: center; right: 0; left: 0;");

  progress.appendChild(progressBar);
  progress.appendChild(progressText);

  return progress;
}

export function cardContent(
  cases_total: number,
  cases_completed: number,
  qcables_total: number,
  qcables_completed: number
): HTMLElement {

  //todo: refactor so this is extensible to other pages
  const casesProgress = progressBar(cases_total, cases_completed);
  const qcablesProgress = progressBar(qcables_total, qcables_completed);

  const cases = document.createElement("div");
  cases.className = "cases";

  const casesTitle = document.createElement("h6");
  casesTitle.innerText = "Cases";

  cases.appendChild(casesTitle);
  cases.appendChild(casesProgress);

  const qcables = document.createElement("div");
  qcables.className = "qcables";

  const qcablesTitle = document.createElement("h6");
  qcablesTitle.innerText = "QCables";

  qcables.appendChild(qcablesTitle);
  qcables.appendChild(qcablesProgress);

  const container = document.createElement("div");
  container.className = "card-container";
  //todo: add last update time
  container.appendChild(cases);
  container.appendChild(qcables);
  return container;
}

// todo: function for paginated cards or infinite scroll cards
// todo: error not constructable for type HTMLDivElement
export function cardContainer(
  ...content: HTMLElement[]
) {
  const cardContainer = document.createElement("div");
  cardContainer.className = "container";

  content
    .forEach((card) => {
      cardContainer.appendChild(card);
    } )

  return cardContainer;
}

export function addCards(
  target: HTMLElement,
  ...content: HTMLElement[]
): HTMLElement {
  content
    .forEach((card) => {
      target.appendChild(card);
    })

  return target;
}
