import {busyDialog} from "./html";
import {fetchAsPromise} from "./io";
import {QCable} from "./data-transfer-objects";

const filterType = sessionStorage.getItem("qcables-filter-type");
const filterId = sessionStorage.getItem("qcables-filter-id");
const filterName = sessionStorage.getItem("qcables-filter-name");

if(filterType && filterId && filterName) {
  initialiseQCables(filterType, filterId, filterName);
}

export function qcablesTable(qcables: QCable[], projectName: string): HTMLElement {
  const pageContainer = document.createElement("div");
  const pageHeader = document.createElement("h2");
  pageHeader.innerText = "QCables (" + projectName + ")";

  pageContainer.appendChild(pageHeader);
  return pageContainer;
}

export function initialiseQCables(filterType: string, filterId: string, filterName: string){
  const closeBusy = busyDialog();

  //todo: how to convert qcables_table object to a typescript object

  fetchAsPromise<QCable[]>("api/qcables_table/" + filterType + "/" + filterId.toString(), {body: null})
    .then((data) => {
      document.body.appendChild(qcablesTable(data, filterName));
    })
    .finally(closeBusy);
}