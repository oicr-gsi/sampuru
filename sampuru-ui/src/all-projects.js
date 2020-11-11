"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.initialiseActiveProjects = exports.activeProjects = void 0;
var html_js_1 = require("./html.js");
var io_js_1 = require("./io.js");
function activeProjects(projects) {
    var cardContainer = document.createElement("div");
    cardContainer.className = "container";
    var cards = [];
    projects
        .forEach(function (project) {
        var card_content = html_js_1.cardContent(project.cases_total, project.cases_completed, project.qcables_total, project.qcables_completed);
        var card = { contents: card_content, header: project.name, title: project.name, tagId: project.name };
        cards.push(html_js_1.collapsibleCard(null, card));
    });
    // todo: move this out to a function
    cards
        .forEach(function (card) {
        var spacing = document.createElement("br");
        cardContainer.appendChild(spacing);
        cardContainer.appendChild(card);
    });
    return cardContainer;
}
exports.activeProjects = activeProjects;
/***
 * Fetch active projects and populate the page
 */
function initialiseActiveProjects() {
    var closeBusy = html_js_1.busyDialog();
    io_js_1.fetchAsPromise("api/active_projects", { body: null })
        .then(function (data) {
        var projects = [];
        data.forEach(function (proj) { return projects.push(io_js_1.decodeProject(proj)); });
        return projects;
    })
        .then(function (projects) {
        document.body.appendChild(activeProjects(projects));
    }) //todo: catch errors
        .finally(closeBusy);
}
exports.initialiseActiveProjects = initialiseActiveProjects;
