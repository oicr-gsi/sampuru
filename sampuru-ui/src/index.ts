import { initialiseActiveProjects, Project } from "./all-projects";



export function initialise() {
  const pcsi: Project = {id: 2, name: "PCSI", casesTotal: 12, casesCompleted: 2,
                         qcAblesTotal: 54, qcAblesCompleted: 23, completionDate: Date.now()};

  const hcc: Project = {id: 2, name: "HCC", casesTotal: 12, casesCompleted: 2,
    qcAblesTotal: 54, qcAblesCompleted: 23, completionDate: Date.now()};

  const tgl42: Project = {id: 2, name: "TGL42", casesTotal: 12, casesCompleted: 2,
    qcAblesTotal: 54, qcAblesCompleted: 23, completionDate: Date.now()};

  var arr = [pcsi, hcc, tgl42];

  return initialiseActiveProjects(arr);

}