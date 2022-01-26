import {createMultiLineLabeledInput, createSingleLineLabeledInput} from "./html.js";

const header = document.createElement("h3");
header.innerText = "Deliverables Portal"

document.body.appendChild(header);

const form = document.createElement("form");

const projectNameTextBox = createSingleLineLabeledInput("Project Name: ", "text", "projectNameTextBox", "form-control");
form.appendChild(projectNameTextBox);

const deliverableInfoTextBox = createMultiLineLabeledInput("Deliverable Information: ", "5", "deliverableInfoTextBox", "form-control");
form.appendChild(deliverableInfoTextBox);

const submitButton = document.createElement("button");
submitButton.className = "btn btn-primary";
submitButton.setAttribute("type", "submit");
submitButton.innerText = "Submit";
submitButton.style.marginLeft = "15px";
form.appendChild(submitButton);

document.body.appendChild(form);




