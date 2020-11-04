export function createLinkElement(className, innerText, attributes, url) {
    const link = document.createElement("a");
    link.className = className;
    link.innerText = innerText;
    link.target = "_blank";
    url ? link.href = url : null;
    if (attributes) {
        attributes.forEach((qualifiedName, value) => link.setAttribute(qualifiedName, value));
    }
    return link;
}
export function collapsibleCard(click, content) {
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
export function staticCard(content) {
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
