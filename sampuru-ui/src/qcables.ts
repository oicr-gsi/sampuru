import {busyDialog} from "./html";
import {fetchAsPromise} from "./io";
import {QCable} from "./data-transfer-objects";

export function qcablesTable(qcables: QCable[], projectName: string): HTMLElement {
  const pageContainer = document.createElement("div");
  const pageHeader = document.createElement("h2");
  pageHeader.innerText = "QCables (" + projectName + ")";

  return pageContainer;
}

export function initialiseQCables(filterType: string, filterId: number, projectName: string){
  const closeBusy = busyDialog();

  //todo: how to convert qcables_table object to a typescript object

  fetchAsPromise<QCable[]>("api/qcables_table/" + filterType + "/" + filterId.toString(), {body: null})
    .then((data) => {
      document.body.appendChild(qcablesTable(data, projectName));
    })
    .finally(closeBusy);
}