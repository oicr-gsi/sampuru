export function formatQualityGateNames(name: string) {
  switch(name){
    case "receipt":
    case "receipt_inspection":
      return "Receipt/Inspection";
    case "extraction":
      return "Extraction";
    case "library_prep":
    case "library_preparation":
      return "Library Preparation";
    case "low_pass":
      return "Low-Pass";
    case "low_pass_sequencing":
      return "Low-Pass Sequencing";
    case "full_depth":
      return "Full-Depth";
    case "full_depth_sequencing":
      return "Full-Depth Sequencing";
    case "informatics":
    case "informatics_interpretation":
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

