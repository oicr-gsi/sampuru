import {createMultiLineLabeledInput, createSingleLineLabeledInput} from "./html.js";

const header = document.createElement("h3");
header.innerText = "Deliverables Portal"

document.body.appendChild(header);

const form = document.createElement("form");

const projectNameTextBox = createSingleLineLabeledInput("Project Name: ", "text", "projectNameTextBox", "form-control");
projectNameTextBox.style.padding = "15px";
form.appendChild(projectNameTextBox);

const deliverableLocationTextBox = createMultiLineLabeledInput("Deliverable Location: ", "3", "deliverableLocationTextBox", "form-control");
deliverableLocationTextBox.style.padding = "15px";
form.appendChild(deliverableLocationTextBox);

const caseIDTextBox = createMultiLineLabeledInput("Case ID: ", "3", "caseIDTextBox", "form-control");
caseIDTextBox.style.padding = "15px";
form.appendChild(caseIDTextBox);

const notesTextBox = createSingleLineLabeledInput("Notes: ", "text", "notesTextBox", "form-control");
notesTextBox.style.padding = "15px";
form.appendChild(notesTextBox);

const formGroup = document.createElement("div");
formGroup.classList.add("form-group");

const expiryDateLabel = document.createElement("label");
expiryDateLabel.innerText = "Expiry Date: ";
formGroup.appendChild(expiryDateLabel);

const lineBreak = document.createElement("br");
formGroup.appendChild(lineBreak);

const expiryDateTextBox = document.createElement("input");
expiryDateTextBox.setAttribute("type", "datetime-local");
expiryDateTextBox.setAttribute("id", "expiryDateTextBox");
formGroup.appendChild(expiryDateTextBox);
formGroup.style.padding = "15px";
form.appendChild(formGroup);

const submitButton = document.createElement("button");
submitButton.className = "btn btn-primary";
submitButton.setAttribute("type", "submit");
submitButton.innerText = "Submit";
submitButton.style.marginLeft = "15px";
form.appendChild(submitButton);

document.body.appendChild(form);




