"use strict";
var __spreadArrays = (this && this.__spreadArrays) || function () {
    for (var s = 0, i = 0, il = arguments.length; i < il; i++) s += arguments[i].length;
    for (var r = Array(s), k = 0, i = 0; i < il; i++)
        for (var a = arguments[i], j = 0, jl = a.length; j < jl; j++, k++)
            r[k] = a[j];
    return r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.elementFromTag = exports.busyDialog = exports.cardContainer = exports.cardContent = exports.progressBar = exports.staticCard = exports.collapsibleCard = exports.createLinkElement = exports.navbar = void 0;
/***
 * Horizontal navbar that becomes vertical on small screens
 */
function navbar() {
    var nav = document.createElement("nav");
    nav.className = "navbar navbar-expand-sm bg-light navbar-light";
    var sampuru = document.createElement("a");
    sampuru.className = "navbar-brand";
    sampuru.innerText = "Sampuru";
    sampuru.href = "#";
    var searchForm = document.createElement("form");
    searchForm.className = "form-inline mx-lg-auto";
    var inputBox = document.createElement("input");
    inputBox.className = "form-control mr-sm-2";
    inputBox.type = "search";
    inputBox.placeholder = "Search";
    var submitButton = document.createElement("button");
    submitButton.className = "btn btn-outline-secondary my-2 my-sm-0";
    submitButton.type = "submit";
    submitButton.innerText = "Search";
    searchForm.appendChild(inputBox);
    searchForm.appendChild(submitButton);
    nav.appendChild(sampuru);
    nav.appendChild(searchForm);
    return nav;
}
exports.navbar = navbar;
function createLinkElement(className, innerText, attributes, url) {
    var link = document.createElement("a");
    link.className = className;
    link.innerText = innerText;
    link.target = "_blank";
    url ? link.href = url : null;
    if (attributes) {
        attributes.forEach(function (value, qualifiedName) { return link.setAttribute(qualifiedName, value); });
    }
    return link;
}
exports.createLinkElement = createLinkElement;
//todo: refactor DOM element creation so less verbose
function collapsibleCard(click, content) {
    var attributes = new Map();
    attributes.set('data-toggle', 'collapse');
    attributes.set('href', "#" + content.tagId);
    var cardLink = createLinkElement("card-link", content.header, attributes, null);
    var cardHeader = document.createElement("div");
    cardHeader.className = "card-header";
    cardHeader.appendChild(cardLink);
    var cardBodyInner = document.createElement("div");
    cardBodyInner.className = "card-body";
    cardBodyInner.appendChild(content.contents);
    var cardBody = document.createElement("div");
    cardBody.id = "#" + content.tagId;
    cardBody.className = "collapse show";
    cardBody.appendChild(cardBodyInner);
    var card = document.createElement("div");
    card.className = "card";
    card.appendChild(cardHeader);
    card.appendChild(cardBody);
    return card;
}
exports.collapsibleCard = collapsibleCard;
function staticCard(content) {
    //todo: populate url with path to project info page
    var cardHeaderLink = createLinkElement("card-link", content.header, null, "project-link");
    var cardHeader = document.createElement("div");
    cardHeader.className = "card-header";
    cardHeader.appendChild(cardHeaderLink);
    var cardBody = document.createElement("div");
    cardBody.className = "card-body";
    cardBody.appendChild(content.contents);
    var card = document.createElement("div");
    card.className = "card";
    card.appendChild(cardHeader);
    card.appendChild(cardBody);
    return card;
}
exports.staticCard = staticCard;
function progressBar(total, completed) {
    //todo: bring css out to a separate file
    var progress = document.createElement("div");
    progress.className = "progress";
    progress.setAttribute("style", "position:relative");
    var progressBar = document.createElement("div");
    progressBar.className = "progress-bar bg-success";
    var casesPercentCompleted = Math.floor((completed / total) * 100);
    progressBar.setAttribute("style", "width:" + casesPercentCompleted.toString() + "%");
    var progressText = document.createElement("div");
    progressText.className = "progress-text";
    progressText.innerText = completed.toString() + "/" + total.toString() + " Completed";
    progressText.setAttribute("style", "position: absolute; line-height: 1rem; text-align: center; right: 0; left: 0;");
    progress.appendChild(progressBar);
    progress.appendChild(progressText);
    return progress;
}
exports.progressBar = progressBar;
function cardContent(cases_total, cases_completed, qcables_total, qcables_completed) {
    //todo: refactor so this is extensible to other pages
    var casesProgress = progressBar(cases_total, cases_completed);
    var qcablesProgress = progressBar(qcables_total, qcables_completed);
    var cases = document.createElement("div");
    cases.className = "cases";
    var casesTitle = document.createElement("h6");
    casesTitle.innerText = "Cases";
    cases.appendChild(casesTitle);
    cases.appendChild(casesProgress);
    var qcables = document.createElement("div");
    qcables.className = "qcables";
    var qcablesTitle = document.createElement("h6");
    qcablesTitle.innerText = "QCables";
    qcables.appendChild(qcablesTitle);
    qcables.appendChild(qcablesProgress);
    var container = document.createElement("div");
    container.className = "card-container";
    //todo: add last update time
    container.appendChild(cases);
    container.appendChild(qcables);
    return container;
}
exports.cardContent = cardContent;
//todo:
function cardContainer() {
    var content = [];
    for (var _i = 0; _i < arguments.length; _i++) {
        content[_i] = arguments[_i];
    }
    var cardContainer = document.createElement("div");
    cardContainer.className = "container";
    content
        .forEach(function (card) {
        var br = document.createElement("br");
        cardContainer.appendChild(br);
        cardContainer.appendChild(card);
    });
    return cardContainer;
}
exports.cardContainer = cardContainer;
/**
 * Return a function to close loader
 */
function busyDialog() {
    var spinner = document.createElement("div");
    spinner.className = "spinner-border";
    spinner.setAttribute("role", "status");
    var loading = document.createElement("span");
    loading.className = "sr-only";
    loading.innerText = "Loading...";
    spinner.appendChild(loading);
    document.body.appendChild(spinner);
    return function () {
        document.body.removeChild(spinner);
    };
}
exports.busyDialog = busyDialog;
function addElements(target) {
    var elements = [];
    for (var _i = 1; _i < arguments.length; _i++) {
        elements[_i - 1] = arguments[_i];
    }
    elements
        .flat(Number.MAX_VALUE)
        .forEach(function (result) {
        if (result === null) {
            target.appendChild(document.createElement("wbr"));
        }
    });
}
function elementFromTag(tag) {
    var elements = [];
    for (var _i = 1; _i < arguments.length; _i++) {
        elements[_i - 1] = arguments[_i];
    }
    var target = document.createElement(tag);
    addElements.apply(void 0, __spreadArrays([target], elements));
    return target;
}
exports.elementFromTag = elementFromTag;
