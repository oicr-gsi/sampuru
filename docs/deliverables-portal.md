# Deliverables Portal
## Overview
Sampuru is a system for monitoring the quality control ('QC') status of items in the Ontario Institute for Cancer Research (OICR) Labs. 

The Deliverables portal will be a page in Sampuru where internal users are able to input/update deliverable information pertinent to a case. The proposed design for the portal is an editable table with the following columns:

| Project*                                                         | Case ID*                                                   | Location* | Notes     | Expiry Date |
|------------------------------------------------------------------|------------------------------------------------------------|-----------|-----------|-------------|
| Single select dropdown, selection populates the Case ID dropdown | Multi select dropdown, empty until the project is selected | Free text | Free text | Date picker |
*indicates a required field

The table will be populated with data from the `/deliverables` endpoint. It's important to note that each row will be editable, meaning for each existing row of deliverables information, the user can modify the cells. With a 'Submit' or 'Save Changes' button, the user should be able to then POST any changes to the `/update_deliverable` endpoint. The rationale behind this design decision is that it allows users to post all of the deliverable information that they want at once, rather than resubmitting for every new piece of information.

This portal was developed using the branch `GR-1376_front-end-deliverables-portal`.
 

## Getting Started
### Sampuru Environment
Below is a breakdown of some of the important files in the Sampuru code base that concern the deliverables portal, and the role that they each play:

- **deliverables.ts**: TypeScript file where the code relevant to the elements on the deliverables page lives. This page is where the table itself is instantiated and populated with data from the /deliverables endpoint.
- **deliverables.js**: JavaScript file which is automatically updated with changes made to `deliverables.ts` by running `tsc -p sampuru-ui && PATH=$(npm bin):$PATH rollup -c`.
- **deliverables.html**: HTML file associated with the deliverables portal. This is where third party libraries are referenced.
- **DeliverableService.java**: Java file where the code relevant to the /deliverables data coming back from the endpoint lives. The current format of the data coming back may need to be tweaked to populate the table. The format of the data can be validated by curling the data from the endpoint (`curl localhost:8088/api/deliverables`).
- **data-transfer-objects.ts**: TypeScript file where data-transfer-objects are created. This file is relevant as it is where the objects: `DeliverableFile[]`, `DonorCase[]`, and `ProjectCase[]` are created. In order for the table to be populated, these objects must match the format of the data coming back from the /deliverables endpoint.

Steps for navigating to the deliverables portal are as follows:

1. Run `tsc -p sampuru-ui && PATH=$(npm bin):$PATH rollup -c` and a `mvn clean install`.
2. Build Sampuru by running the server.
3. Navigate to `http://localhost:8088/deliverables.html`.
### Editable Table Extension
As mentioned above, one of the key features of the deliverables table is the ability for existing cells to be continually updated. As such, there was a need for a third-party library to make the table editable. Thankfully, there exists an editable table extension of Bootstrap Table (https://bootstrap-table.com/docs/extensions/editable/). Steps for using this extension were quite ambiguous, but this is what has been done so far:

1. Downloaded the x-editable plug-in using `npm install x-editable`. This plugin now shows up in `package.json`.
2. The following libraries were downloaded and added to `sampuru/sampuru-server/src/main/resources/static/third-party-libraries/`:
   1. `bootstrap-editable.min.js` (this file was listed on the x-editable website: http://vitalets.github.io/x-editable/)
   2. `bootstrap-table-editable.js` (this file was listed on the Bootstrap website and found in the following bootstrap-table github repository: https://github.com/wenzhixin/bootstrap-table/blob/develop/site/docs/extensions/editable.md)
3. The following css file was added to `sampuru/sampuru-server/src/main/resources/static/third-party-libraries/css/`:
   1. `bootstrap-editable.css` (this file was listed on the Bootstrap website and found in the following bootstrap-table github repository: https://github.com/wenzhixin/bootstrap-table/blob/develop/site/docs/extensions/editable.md)
4. These 3 files were then referenced in `deliverables.html` (which can be found: `sampuru/sampuru-server/src/main/resources/static/deliverables.html`).

This extension hasn't currently been tested robustly given the lack of data issue with the table. To my understanding, as a next step, the bootstrapTable function in `html.ts` should be modified to take an additional parameter, which would be a boolean named editable.
## Next Steps
In terms of next steps for this portal, the main priority would be to get a skeleton outline of the table populated with data from the /deliverables endpoint. This is a current limitation as there seems to be an issue with the format of the data (the table can be populated with dummy data, which indicates that there is an issue with the data from the endpoint). 