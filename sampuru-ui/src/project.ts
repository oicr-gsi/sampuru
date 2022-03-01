/// <reference types="jquery" />
/// <reference types="bootstrap" />
/// <reference types="bootstrap-table" />
/// <reference types="file-saver" />

import {
  busyDialog,
  Card,
  elementFromTag,
  staticCard,
  navbar,
  DOMElement,
  progressBar,
  createLinkElement,
  ComplexElement,
  tableRow,
  bootstrapTable,
  tableBodyFromRows,
  createButton
} from "./html.js";
import {urlConstructor} from "./io.js";
import {ProjectInfo, Changelog} from "./data-transfer-objects.js";
import { drawSankey } from "./sankey.js";
import {commonName, formatQualityGateNames} from "./common.js";


const urlParams = new URLSearchParams(window.location.search);
const projectId = urlParams.get("project-overview-id");

if(projectId) {
  initialiseProjectOverview(projectId);
}

export function failedChangelog(changelogContent: string): string {
  if(changelogContent.includes("fail")) {
    return "table-danger";
  } else {
    return "";
  }
}

export function changelogTable(changelogs: Changelog[], external: boolean): ComplexElement<HTMLElement> {
  const tableRows: ComplexElement<HTMLTableRowElement>[] = [];

  changelogs
    .forEach((changelog) => {
      tableRows.push(tableRow(null,
        {
          contents: external ? changelog.external_name : changelog.case_id,
          className: failedChangelog(changelog.content)
        },
        {
          contents: changelog.qcable_type == "null" ? "N/A" : formatQualityGateNames(changelog.qcable_type),
          className: failedChangelog(changelog.content)
        },
        {
          contents: external ?
            (changelog.external_name == "null" ? "N/A" : changelog.external_name) :
            (changelog.qcable_oicr_alias == "null" ? "N/A" : changelog.qcable_oicr_alias),
          className: failedChangelog(changelog.content)
        },
        {
          contents: changelog.content,
          className: failedChangelog(changelog.content)
        },
        {
          contents: changelog.change_date,
          className: failedChangelog(changelog.content)
        }))
    });


  const tableHeaders = new Map([
    ["donor_case_name", "Case"],
    ["qcable_type", "Quality Gate"],
    [external ? "oicr_alias" : "external_name", "QC-able"],
    ["content", "Content"],
    ["change_date", "Change Date"]
  ]);

  const table = bootstrapTable(tableHeaders, true, true, null, "project-changelog", true, true);
  const tableBody = tableBodyFromRows(null, tableRows);

  table.appendChild(tableBody);
  table.className = "changelog-table";

  const $table = $("#table"); // "table" accordingly

  $(function() {
    $("#toolbar")
        .find("select")
        .change(function() {
          $table.bootstrapTable("refreshOptions", {
            exportDataType: $(this).val()
          });
        });
  });

  return {type: "complex", element: table};
}


export function project(projectInfo: ProjectInfo, changelogs: Changelog[]): HTMLElement {
  const pageContainer = document.createElement("div");
  const pageHeader = document.createElement("h2");
  pageHeader.innerText = projectInfo.name;

  const cardContainter = document.createElement("div");
  cardContainter.className = "container";

  const cards: HTMLElement[] = [];
  const infoItems: DOMElement[] = [];

  infoItems.push(elementFromTag("div", "row",
    elementFromTag("b", null, "Name: "),
    elementFromTag("p", null,  projectInfo.name)));

  if(projectInfo.created_date != "null") {
    infoItems.push(elementFromTag("div", "row",
      elementFromTag("b", null, "Creation Date: "),
      elementFromTag("p", null,  projectInfo.created_date)));
  }

  if(projectInfo.description != "null") {
    infoItems.push(elementFromTag("div", "row",
      elementFromTag("b", null, "Description: "),
      elementFromTag("p", null,  projectInfo.description)));
  }

  if(projectInfo.pipeline != "null") {
    infoItems.push(elementFromTag("div", "row",
      elementFromTag("b", null, "Pipeline: "),
      elementFromTag("p", null,  projectInfo.pipeline)));
  }

  if(projectInfo.kits != "[]") {
    infoItems.push(elementFromTag("div", "row",
      elementFromTag("b", null, "Kits: "),
      elementFromTag("p", null,  projectInfo.kits.replace(/[\[\]']+/g, ''))));
  }

  projectInfo.info_items.forEach((item) => {
    if (item.content != null) {
      infoItems.push(elementFromTag("div", "row",
        elementFromTag("b", null, item.entry_type + ": "),
        elementFromTag("p", null,  item.content)));
    }
  });

  const info = elementFromTag("div", "container project-overview", infoItems);
  const infoCard: Card = {contents: info.element, header: "Project Information",
    title: projectInfo.name + " Overview", cardId: projectInfo.name + "-overview"};


  const contact = elementFromTag("div", "contact row",
    elementFromTag("div", "col-6",
      elementFromTag("b", null, "Name: "),
      elementFromTag("p", null, (projectInfo.contact_name == "null") ? "None" : projectInfo.contact_name)),
    elementFromTag("div", "col-6",
      elementFromTag("b", null, "Email: "),
      elementFromTag("p", null, (projectInfo.contact_email == "null") ? "None" : projectInfo.contact_email)));


  const contactCard: Card = {contents: contact.element, header: "Contact Information",
    title: projectInfo.name + " Contact Information", cardId: projectInfo.name + "-contact"};

  const qcablesLink = createLinkElement(
    null,
    "QC-ables",
    null,
    null,
    urlConstructor("qcables.html", ["qcables-filter-type", "qcables-filter-id"], ["project", projectInfo.name])

  );

  const qcables = elementFromTag("div", null,
    {type: "complex", element: qcablesLink},
    {type: "complex", element: progressBar(projectInfo.qcables_total, projectInfo.qcables_completed)});

  const casesLink = createLinkElement(
    null,
    "Cases",
    null,
    null,
    urlConstructor("cases.html", ["cases-project-id", "identifier"], [projectInfo.name, "external"])
  );

  const cases = elementFromTag("div", null,
    {type: "complex", element: casesLink},
    {type: "complex", element: progressBar(typeof projectInfo.cases_total !== "undefined" ? projectInfo.cases_total : 0,
        typeof projectInfo.cases_completed !== "undefined" ? projectInfo.cases_completed : 0)});

  const projectSummary = elementFromTag("div", "row",
    elementFromTag("div", "col col-md-8", {type: "complex", element: staticCard(infoCard)}),
    elementFromTag("div", "col col-md-4",
      {type: "complex", element: staticCard(contactCard)},
      {type: "complex", element: qcables.element},
      {type: "complex", element: cases.element}));

  const sankeyContainer = document.createElement("div");
  sankeyContainer.id = "sankey";
  const qcablesCard: Card = {contents: sankeyContainer, header: "QC-ables",
    title: projectInfo.name + " QC-ables", cardId: projectInfo.name + "-qcables"};

  const toggleIds = createButton('toggle-changelog-ids', "Switch to OICR Identifiers", "identifier");
  const changelogTables = elementFromTag("div", null,
    {type: "complex", element: toggleIds},
    changelogTable(changelogs, true),
    changelogTable(changelogs, false));

  const changelogsCard: Card = {contents: changelogTables.element, header: "Changelogs",
    title: projectInfo.name + " Changelogs", cardId: projectInfo.name + "-changelogs"};

  /* TODO: Uncomment once deliverable portal is developed and there is data to display. GR-1334
  const deliverables = elementFromTag("div", null, projectInfo.deliverables.join("\n"));
  const deliverablesCard: Card = {contents: deliverables.element, header: "Files",
    title: projectInfo.name + " Files", cardId: projectInfo.name + "-files"};
  */

  cards.push(projectSummary.element);
  cards.push(staticCard(qcablesCard));
  cards.push(staticCard(changelogsCard));
  //cards.push(staticCard(deliverablesCard)); END TODO

  cards
    .forEach((card) => {
      const spacing = document.createElement("br");
      cardContainter.appendChild(spacing);
      cardContainter.appendChild(card);
    });

  pageContainer.appendChild(pageHeader);
  pageContainer.appendChild(cardContainter);
  return pageContainer;
}


export function initialiseProjectOverview(projectId: string) {
  const closeBusy = busyDialog();

  Promise.all([
    fetch("api/project_overview/" + projectId),
    fetch("api/changelogs/project/" + projectId)
  ])
    .then(responses => {
      document.body.appendChild(navbar(commonName(responses[0]), "project-overview"));
      return Promise.all(responses.map(response => response.json()));
    })
    .then((responses) => {
      const projectInfo = responses[0] as ProjectInfo
      const changelogs = responses[1] as Changelog[]

      document.body.appendChild(project(projectInfo, changelogs));
      return projectInfo;
    })
    .then((data) => {
      drawSankey(data.sankey_transitions);
    })
    .then(() => {
      $(function () {
        $('#external-changelog,#internal-changelog').bootstrapTable({});

        // Default to show external table and hide internal table
        $('div').removeClass('clearfix'); // This is a Bootstrap class that gets preset for all tables that isn't needed
        $('#internal-changelog').parents().hide();
        $('#external-changelog').parents().show();

        // Show appropriate table on button click
        // i.e. If user clicks "OICR Identifiers" button, hide external table and change button text to "External identifiers"
        $('#toggle-changelog-ids').on('click', function() {
          $(this).text(function(i, text) {
            if(text === "Switch to OICR Identifiers") {
              $('#external-changelog').parents().hide();
              $('#internal-changelog').parents().show();
              return "Switch to External Identifiers";
            } else {
              $('#internal-changelog').parents().hide();
              $('#external-changelog').parents().show();
              return "Switch to OICR Identifiers";
            }
          });
        });
      });
    })
    .catch((error) => {
      console.log(error); //todo: write to actual logs
    })
    .finally(closeBusy);
}
