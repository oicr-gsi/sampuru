export function commonName(response: Response){
  return response.headers.get("X-Common-Name");
}

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
    case "library_qual":
      return "Library Qualification";
    case "library_qualification":
      return "Library Qualification";
    case "full_depth":
      return "Full-Depth";
    case "full_depth_sequencing":
      return "Full-Depth Sequencing";
    case "informatics":
    case "informatics_interpretation":
      return "Informatics + Interpretation";
    case "draft_report":
      return "Draft Report";
    case "final_report":
      return "Final Report";
    default:
      return "";
  }
}

/**
 * Library design codes in MISO
 * */
export function formatLibraryDesigns(libraryDesign: string) {
  switch(libraryDesign) {
    case "AS":
      return "ATAC-Seq";
    case "BS":
      return "Bisulphite Sequencing";
    case "CH":
      return "ChIP-Seq";
    case "CM":
      return "cfMeDIP";
    case "CT":
      return "ctDNA";
    case "EX":
      return "Exome";
    case "NN":
      return "Unknown";
    case "MR":
      return "mRNA";
    case "SC":
      return "Single Cell";
    case "SM":
      return "smRNA";
    case "TR":
      return "Total RNA";
    case "TS":
      return "Targeted Sequencing";
    case "WG":
      return "Whole Genome";
    case "WT":
      return "Whole Transcriptome";
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
