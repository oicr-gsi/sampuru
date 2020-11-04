
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
    attributes.forEach( (qualifiedName, value) => link.setAttribute(qualifiedName, value));
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
  // TODO: Pass in body contents

  const cardBody = document.createElement("div");
  cardBody.id = `#${content.tagId}`;
  cardBody.className = "collapse";
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
  // TODO: Pass in body contents

  const card = document.createElement("div");
  card.className = "card";
  card.appendChild(cardHeader);
  card.appendChild(cardBody);
  return card;
}

// todo: function for paginated cards or infinite scroll cards
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