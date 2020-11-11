/**
 * The callback for handling mouse events
 */

export type ClickHandler = (e: MouseEvent) => void;

export interface CardElement {
  type: "card";

  attributes?: Map<string, string>;

  /** Header to apply to card */
  header: string;

  /** Tooltip on hover */
  title?: string;

  /** Card body */
  contents: DOMElement;

  /** ID to apply to card body's div so Bootstrap knows which element to collapse/expand */
  id: string;

  /** Cards expanded by default or not. true = expanded, false = collapsed */
  show: boolean;

  /** Collapse cards or static cards. true = collapse, false = static */
  collapse: boolean;
}

export interface LinkElement {
  type: "a";

  /** URL to link to*/
  url: string;

  /** Display text to use*/
  innerText: string;

  /** Tooltip for the link*/
  title: string;

  /** Space-separated list of classes to apply to link*/
  className: string;
}

export interface ComplexElement<T extends HTMLElement> {
  type: "complex";
  /** DOM node*/
  element: T;
}

export interface TextElement {
  type:
    | "p" // paragraph
    | "b" // bold
    | "i"; // italic
  contents: DisplayElement;
  className: string | null;
}

/** Elements not associated with a DOM node */
export type DisplayElement =
  | number
  | string
  | null // for wbr elements
  | TextElement
  | LinkElement

/** Any elements that can be associated with a DOM node*/
export type DOMElement =
  | number
  | string
  | null // for wbr elements
  | LinkElement
  | TextElement
  | DisplayElement
  | CardElement
  | ComplexElement<HTMLElement>

function addElements(
  target: HTMLElement,
  ...elements: DOMElement[]
): void {
  elements
    .flat(Number.MAX_VALUE)
    .forEach((result: Exclude<DOMElement, DOMElement[]>) => {
      if (result === null) {
        target.appendChild(document.createElement("wbr"));
      } else if (typeof result == "string") {
        target.appendChild(document.createTextNode(result));
      } else if (typeof result == "number") {
        target.appendChild(document.createTextNode(result.toString()));
      } else {
        switch (result.type) {
          case "a": {
            const element = document.createElement("a");
            element.innerText = result.innerText;
            element.href = result.url;
            element.title = result.title;
            element.className = result.className;
            target.appendChild(element);
          }
            break;
          case "b":
          case "i":
          case "p": {
            const element = elementFromTag(result.type, result.className, result.contents);
            target.appendChild(element.element);
          }
            break;
          case "complex": {
            target.appendChild(result.element); // Already an HTML element so just append to target
          }
            break;
          case "card": {
            const linkElement = link(result.header, "#", "card-link"); //todo: data-toggle: collapse attribute
            const cardHeader = elementFromTag("div", "card-header", linkElement);

            const cardBodyInner = elementFromTag("div", "card-body", result.contents);

            if(result.collapse) {
              let className = "collapse";
              if(result.show) {
                className = `${className} show`;
              }
            }

            const cardBody = document.createElement("div");
            cardBody.id = `#${result.id}`;




        }
      }
    }
}

);
}

/**
 *
 * @param innerText - label for the link
 * @param url - target of the hyperlink
 * @param title - optional tooltip
 * @param className - class for hyperlink element
 */
export function link(
  innerText: string | number,
  url: string,
  className: string,
  title?: string
): LinkElement {
  return { type: "a", url: url, innerText: innerText.toString(), title: title || "", className: className}
}

export function elementFromTag<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  className: string | null,
  ...elements: DOMElement[]
): ComplexElement<HTMLElementTagNameMap[K]> {
  const target = document.createElement(tag);
  if (typeof className == "string") {
    target.className = className;
  }
  addElements(target, ...elements);
  return {element: target, type: "complex"};
}

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

/***
 * Horizontal navbar that becomes vertical on small screens
 */
export function navbar(): HTMLElement {

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
  if (attributes) {
    attributes.forEach((value, qualifiedName) => link.setAttribute(qualifiedName, value));
  }
  return link;
}

//todo: refactor DOM element creation so less verbose
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
  const casesPercentCompleted = Math.floor((completed / total) * 100);
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

//todo:
export function cardContainer(
  ...content: HTMLElement[]
): HTMLElement {
  const cardContainer = document.createElement("div");
  cardContainer.className = "container";

  content
    .forEach((card) => {
      const br = document.createElement("br");
      cardContainer.appendChild(br);
      cardContainer.appendChild(card);
    })

  return cardContainer;
}

/**
 * Return a function to close loader
 */
export function busyDialog(): () => void {
  const spinner = document.createElement("div");
  spinner.className = "spinner-border";
  spinner.setAttribute("role", "status");

  const loading = document.createElement("span");
  loading.className = "sr-only";
  loading.innerText = "Loading...";

  spinner.appendChild(loading);
  document.body.appendChild(spinner);

  return () => {
    document.body.removeChild(spinner);
  }
}


