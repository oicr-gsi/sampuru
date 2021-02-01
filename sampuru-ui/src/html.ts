import { Case, Project } from "./data-transfer-objects.js";
import { formatQualityGateNames, formatLibraryDesigns, libDesignSort } from "./common.js";
import {constructURL} from "./io.js";

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

export interface TableCell {
  /**
   * Class name for the table cell
   */
  className?: string;
  /**
   * A callback if the cell is clicked
   */
  click?: ClickHandler;
  /**
   * The elements that should be in the cell.
   */
  contents: DOMElement;
  /**
   * If true, the cell is a header; if false or absent, it is a data cell.
   */
  header?: boolean;
  /**
   * A tooltip for the cell
   */
  title?: string;
}

export interface LinkElement {
  type: "a";

  /** URL to link to*/
  href: string;

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
  | DOMElement[]

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
            element.href = result.href;
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
            //todo: not currently working
            //todo: data-toggle: collapse attribute
            const cardHeader = elementFromTag("div", "card-header",
              {type: "a", innerText: result.header, href: "#", className: "card-link", title: "collapse-card"});

            const cardBodyInner = elementFromTag("div", "card-body", result.contents);

            const cardBody = document.createElement("div");
            cardBody.id = `#${result.id}`;

            if(result.collapse) {
              cardBody.className = "collapse"
              if(result.show) {
                cardBody.className += " show"
              }
            }
            cardBody.appendChild(cardBodyInner.element);

        }
      }
    }
    });
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
 * Display a table from the supplied items.
 * @param rows - the items to use for each row
 * @param headers - a list of columns, each with a title and a function to render that column for each row
 */
export function table<T>(
  rows: T[],
  ...headers: [DOMElement, (value: T) => DOMElement][]
): DOMElement {
  if (rows.length == 0) return [];
  return elementFromTag("table", "table",
    elementFromTag("tr", null,
      headers.map(([name, _func]) => elementFromTag("th", null, name))
    ),
    rows.map((row) =>
      elementFromTag("tr", null,
        headers.map(([_name, func]) => elementFromTag("td", null, func(row)))
      )
    )
  );
}

/**
 * Create a table body from a collection of rows
 */
export function tableBodyFromRows(
  className: string | null,
  rows: ComplexElement<HTMLTableRowElement>[]
): HTMLElement {
  const tbody = elementFromTag("tbody", (typeof className == "string") ? className : null, ...rows);
  return tbody.element;
}

/**
 * Create a single row to put in a table
 * @param click - an optional click handler
 * @param cells - the cells to put in this row
 */
export function tableRow(
  click: ClickHandler | null,
  ...cells: TableCell[]
): ComplexElement<HTMLTableRowElement> {
  const row = elementFromTag("tr", null,
    cells.map(({ className, click, contents, header, title }) => {
      const cell = elementFromTag(
        header ? "th" : "td",
        (typeof className == "string") ? className : null,
        contents);

      if (click) {
        cell.element.style.cursor = "pointer";
        cell.element.addEventListener("click", (e) => {
          e.stopPropagation();
          click(e);
        });
      }

      if (title) {
        cell.element.title = title;
      }
      return cell;
    })
  );
  if (click) {
    row.element.style.cursor = "pointer";
    row.element.addEventListener("click", click);
  }
  return row;
}


/**
 * Instantiate Bootstrap table
 * @param headers -> map of data-field to header innerText
 * @param pagination -> boolean for paginating table
 * @param search -> boolean for adding a search to table
 * */
export function bootstrapTable(
  headers: Map<string, string>,
  pagination: boolean,
  search: boolean
): HTMLElement {
  
  const table = document.createElement("table");
  table.id = "table";
  table.setAttribute("data-toggle", "table");

  if (pagination) {
    table.setAttribute("data-pagination", "true");
  }

  if (search) {
    table.setAttribute("data-search", "true");
  }

  const thead = document.createElement("thead");
  const tr = document.createElement("tr");

  headers
    .forEach((header, data, map) => {
      const cell = document.createElement("th");
      cell.setAttribute("data-field", data);
      cell.innerText = header;
      tr.appendChild(cell);
    });

  thead.appendChild(tr);
  table.appendChild(thead);
  return table;
}

export function caseCard(caseContent: Case): HTMLElement {
  const caseProgess: DOMElement[] = [];
  // sort list so blank library designs are displayed first
  caseContent.bars.sort((a, b) => libDesignSort(a.library_design, b.library_design));
  // each bar represents case data split by library design
  caseContent.bars.forEach((bar) => {
    const progressBarContainer = document.createElement("div");
    progressBarContainer.className = "col-10 cases container"; // col-10 => so progress bars span most of the card-body
    // a step is equivalent to quality gate
    bar.steps.forEach((step) => {
      const qualityGate = document.createElement("div");
      qualityGate.className = "cases quality-gate " + step.status;
      qualityGate.innerText = step.completed.toString() + "/" + step.total.toString();

      const qualityGateName = document.createElement("p");
      qualityGateName.className = "quality-gate-name";
      qualityGateName.innerText = formatQualityGateNames(step.type);

      qualityGate.appendChild(qualityGateName);
      progressBarContainer.appendChild(qualityGate);
    });

    // push each row of case data with the library design to the left and progress bars to the right
    caseProgess.push(elementFromTag("div", "row",
      elementFromTag("div", "col-2 library-design",
        formatLibraryDesigns(typeof bar.library_design === "string" ? bar.library_design : "")),
      { type: "complex", element: progressBarContainer }));
  });

  const container = elementFromTag("div", "container", caseProgess);
  return container.element;
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
  sampuru.href = "/";

  const searchForm = document.createElement("form");
  searchForm.className = "form-inline mx-lg-auto";

  const inputBox = document.createElement("input");
  inputBox.className = "form-control mr-sm-2";
  inputBox.type = "search";
  inputBox.placeholder = "Search Sampuru";

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
  referer: string,
  click: ClickHandler | null,
  content: Card
): HTMLElement {

  // todo: this is for Boostrap card collapsing
  /*
  let attributes = new Map();
  attributes.set('data-toggle', 'collapse');
  attributes.set('href', `#${content.tagId}`);

  const cardLink = createLinkElement("card-link", content.header, attributes, null);*/

  let cardLink;
  if (referer == "active_projects") {
    cardLink = document.createElement("a");
    cardLink.innerText = content.header;
    cardLink.href = constructURL(
      "project.html", ["project-overview-id"], [content.tagId]);
  } else if (referer == "cases") {
    cardLink = document.createElement("a");
    cardLink.innerText = content.header;
    cardLink.href = constructURL(
      "qcables.html", ["qcables-filter-type", "qcables-filter-id"], ["case", content.tagId]);
  }
  else {
    cardLink = document.createElement("p");
    cardLink.innerText = content.header;
  }

  const cardHeader = document.createElement("div");
  cardHeader.className = "card-header";
  cardHeader.appendChild(cardLink);

  const cardBodyInner = document.createElement("div");
  cardBodyInner.className = "card-body";
  cardBodyInner.appendChild(content.contents);

  const cardBody = document.createElement("div");
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

  const cardHeader = elementFromTag("div", "card-header", content.header);

  const cardBody = document.createElement("div");
  cardBody.className = "card-body";
  cardBody.appendChild(content.contents);

  const card = document.createElement("div");
  card.className = "card";
  card.appendChild(cardHeader.element);
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
  progressBar.className = "progress-bar bg-primary";

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

export function projectCard(
  project: Project
): HTMLElement {

  //todo: refactor so it's extensible to other pages
  const casesProgress = progressBar(project.cases_total, project.cases_completed);
  const qcablesProgress = progressBar(project.qcables_total, project.qcables_completed);

  const cases = document.createElement("div");
  cases.className = "cases";

  const casesTitle = document.createElement("a");
  casesTitle.innerText = "Cases";
  casesTitle.href = constructURL("cases.html", ["cases-project-id"], [project.id]);

  cases.appendChild(casesTitle);
  cases.appendChild(casesProgress);

  const qcables = document.createElement("div");
  qcables.className = "qcables";

  const qcablesTitle = document.createElement("a");
  qcablesTitle.innerText = "QCables";
  qcablesTitle.href = constructURL(
    "qcables.html", ["qcables-filter-type", "qcables-filter-id"], ["project", project.id]);

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


