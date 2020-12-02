import {SankeyTransition} from "./data-transfer-objects";

export interface SankeyCsv {
  source: string,
  target: string,
  value: number
}

/**
 * Return node names that will be displayed in Sankey
 * */
export function nodeNames(key: keyof SankeyTransition): string {
  switch(key) {
    case "receipt":
      return "Receipt";
    case "extraction":
      return "Extraction";
    case "library_preparation":
      return "Library Preparation";
    case "low_pass_sequencing":
      return "Low-Pass Sequencing";
    case "full_depth_sequencing":
      return "Full-Depth Sequencing";
    case "informatics_interpretation":
      return "Informatics Interpretation";
    case "final_report":
      return "Final Report";
  }
}

/**
 * d3 will need a csv with data in the format:
 * source,target,value
 * In order to construct nodes and links
 * */
export function convertSankeyTransitionToCsv(sankey: SankeyTransition) {
  const sankeyCsv: SankeyCsv[] = [];

  // Convert SankeyTransition to SankeyCsv
  (Object.keys(sankey) as Array<keyof SankeyTransition>).forEach(
    (key, index, keyArray) => {
      // Create link between quality gates
      if (sankey[key].passed > 0 && index + 1 < keyArray.length) {
        sankeyCsv.push(
          {
            source: nodeNames(key),
            target: nodeNames(keyArray[index + 1]),
            value: sankey[key].passed
          });
      }
      // Create link for quality gate that is pending
      if (sankey[key].pending > 0) {
        sankeyCsv.push(
          {
            source: nodeNames(key),
            target: "Pending",
            value: sankey[key].pending
          });
      }

      // Create link for quality gate that failed
      if (sankey[key].failed > 0) {
        sankeyCsv.push(
          {
            source: nodeNames(key),
            target: "Failed",
            value: sankey[key].failed
          });
      }
    });

  let csv: string;
  // Create csv string from object
  sankeyCsv.forEach((row, index, csvArray) => {
    let csvKeys = (Object.keys(row) as Array<keyof SankeyCsv>);
    let counter = 0;
    // First row? Generate headings
    if (index == 0) {
      csvKeys.forEach(key => {
        csv += key + (counter+1 < csvKeys.length ? ',' : '\r\n') // Prevent a comma at the last cell
        counter++;
      })
    } else {
      csvKeys.forEach(key => {
        csv += row[key] + (counter+1 < csvKeys.length ? ',' : '\r\n') // Prevent a comma at the last cell
        counter++;
      })
    }
    counter = 0; // Reset counter
  });

}