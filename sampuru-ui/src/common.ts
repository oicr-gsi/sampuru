import {busyDialog} from "./html.js";
import {fetchAsPromise} from "./io.js";
import {Changelog, DeliverableFile, SearchedCase, SearchedProject, SearchedQCable} from "./data-transfer-objects";

export function formatQualityGateNames(name: string) {
  switch(name){
    case "receipt":
      return "Receipt + Inspection";
    case "extraction":
      return "Extraction";
    case "library_prep":
      return "Library Preparation";
    case "low_pass":
      return "Low-Pass";
    case "full_depth":
      return "Full-Depth";
    case "informatics":
      return "Informatics + Interpretation";
    case "final_report":
      return "Final Report";
    default:
      return "";
  }
}

export function formatLibraryDesigns(libraryDesign: string) {
  switch(libraryDesign) {
    case "TS":
      return "Targeted Sequencing";
    case "WG":
      return "Whole Genome";
    case "CM":
      return "cfMeDIP";
    case "EX":
      return "Exome";
    case "WT":
      return "Whole Transcriptome";
    case "MR":
      return "mRNA";
    default:
      return "";
  }
}

/**
 * Float null values to the top and sort everything else alphabetically
 * Used to ensure that progress bars with blank library designs in the Case page
 * are displayed first.
 */

export function libDesignSort(a: string | null, b: string | null) {
  // equal items sort equally
  if (a == b) {
    return 0;
  }
  // sort null's before anything else so blank library designs float to the top
  else if (a === null) {
    return -1;
  }
  else if (b === null) {
    return 1;
  }
  // otherwise, sort alphabetically
  else  {
    return a < b ? -1 : 1;
  }
}

export function defaultSearchResults(
  projects: SearchedProject[],
  qcables: SearchedQCable[],
  donor_cases: SearchedCase[],
  changelogs: Changelog[],
  notifications: Notification[],
  deliverables: DeliverableFile[]
): HTMLElement {
  const container = document.createElement("div");

  return container;
}





export function defaultSearch(searchString: string) {
  const closeBusy = busyDialog();

  Promise.all([
    fetch("api/search/project/" + searchString),
    fetch("api/search/qcable/" + searchString),
    fetch("api/search/case/" + searchString),
    fetch("api/search/changelog/" + searchString),
    fetch("api/search/notification/" + searchString),
    fetch("api/search/deliverable/" + searchString)
  ])
    .then(responses => Promise.all(responses.map(response => response.json())))
    .then((responses) => {
      const projects = responses[0] as SearchedProject[];
      const qcables = responses[1] as SearchedQCable[];
      const donor_cases = responses[2] as SearchedCase[];
      const changelogs = responses[3] as Changelog[];
      const notifications = responses[4] as Notification[];
      const deliverables = responses[5] as DeliverableFile[];

      document.body.appendChild(defaultSearchResults(projects, qcables, donor_cases, changelogs, notifications, deliverables));
    })
    .catch((error) => {
      console.log(error); // todo: log this somewhere permanent
    })
    .finally(closeBusy);

}