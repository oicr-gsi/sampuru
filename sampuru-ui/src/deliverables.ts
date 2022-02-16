import {createLabeledTextarea, createLabeledInput} from "./html.js";
import {DeliverableFile} from "./data-transfer-objects.js";

const header = document.createElement("h3");
header.innerText = "Deliverables Portal"

document.body.appendChild(header);

function displayModal(inputText: string){
  const modal = document.createElement("div");
  modal.classList.add("modal");

  const modalContent = document.createElement("div");
  modalContent.classList.add("modal-content");
  modalContent.innerText = inputText;
  modal.appendChild(modalContent);
}

const form = document.createElement("form");
form.id = "deliverables-form";
form.className = "deliverables";
form.onsubmit = () => {
  const formData = new FormData(form);
  // formData.forEach((value => console.log(value as string)));
  // ^ if you want to see all of the form values
  const project = formData.get('projectName') as string;
  const location = formData.get('deliverableLocation') as string;
  const caseIds = formData.get('deliverableCaseIDs') as string;
  const notes = formData.get('deliverableNotes') as string;
  const expiryDate = formData.get('expiryDateSelector') as string;

  const caseIDArray = caseIds.split("\n");

  const deliverable: DeliverableFile = {project_id: project, case_id: caseIDArray, location: location, notes: notes, expiry_date: expiryDate};
  const result: DeliverableFile[] = [deliverable];

  fetch("api/update-deliverables", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(result),
  })
      .then(response => response.json())
      .then(result => {
        displayModal("Deliverable information was successfully submitted.");
        console.log(result);
      })
      .catch((error) => {
        displayModal("You're not authorized to submit deliverable information. Please contact a member of the Sampuru team.");
        console.error(error);
      })


  /* fetch("api/update-deliverables")
      .then(response => response.json())
      .then(result => console.log(result)) */

  // ^ These are all the values you want to send to the API
  // 1. You'll want to turn caseIds into an array of strings where new line is the separator
  // 2. On the terminal, send a request to the endpoint to see if it'll accept expiryDate in the format 2022-01-22T12:12
  // 3. Play with the endpoint to observe how the endpoint reacts to bad data
  // 4. If the endpoint reliably sends an error HTTP response to bad data, look at JavaScript fetch() best practices.
  //    You'll want to convert all of this data into a well-formed JSON array and send a fetch to `api/update-deliverables`
  //    with the JSON array. Depending on the HTTP response code, you'll want to display a modal to the user.
  //    i.e. if the response is 200, modal should say "Deliverable information was successfully submitted."
  //         if response is 503, "You're not authorized to submit deliverable information. Please contact a member of the Sampuru team."
  //         etc for all the usual HTTP response codes.
}

const projectNameTextBox = createLabeledInput("Project Name: ", "text",
  "projectName", true,"form-control", null);
form.appendChild(projectNameTextBox);

const deliverableLocationTextBox = createLabeledInput("Deliverable Location: ",
  "text", "deliverableLocation", true,"form-control", null);
form.appendChild(deliverableLocationTextBox);

const caseIDTextBox = createLabeledTextarea("Case ID: ", "3", "deliverableCaseIDs", true,
  "form-control", "Enter one case ID per new line");
form.appendChild(caseIDTextBox);

const notesTextBox = createLabeledInput("Notes: ", "text", "deliverableNotes", false,
  "form-control", "Enter any notes associated with this deliverable");
form.appendChild(notesTextBox);

const expiryDateSelector = createLabeledInput("Expiry Date: ", "datetime-local", "expiryDateSelector",
  true,"form-control", null);
form.appendChild(expiryDateSelector);

const submitButton = document.createElement("button");
submitButton.className = "btn btn-primary";
submitButton.setAttribute("type", "submit");
submitButton.innerText = "Submit";
form.appendChild(submitButton);

document.body.appendChild(form);






