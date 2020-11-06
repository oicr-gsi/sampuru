import {fetchOperation, fetchJson, Project} from './io';
import {navbar} from "./html";
import {initialiseActiveProjects} from "./all-projects";

document.body.appendChild(navbar());
document.body.appendChild(initialise());


/*
export function initialise() {
  fetchJson("https://localhost:8088/api/active_projects", {body: null}, (data) => {
    // @ts-ignore
    document.getElementById("test").innerText = JSON.stringify(data);
  });
}*/

export function initialise() {
  const res = '[{"cases_total":845,"last_update":"2020-11-06T09:45:36.184542","name":"TGL11","qcables_completed":0,"id":3,"cases_completed":0,"qcables_total":1808},{"cases_total":41,"last_update":"2020-11-06T09:45:49.524850","name":"TGL45","qcables_completed":0,"id":4,"cases_completed":0,"qcables_total":666},{"cases_total":5,"last_update":"2020-11-06T09:45:50.111577","name":"CAPPT","qcables_completed":0,"id":5,"cases_completed":0,"qcables_total":56}]';
  const projects = <Project[]>JSON.parse(res);
  return initialiseActiveProjects(projects);
}