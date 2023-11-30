package life.qbic.projectmanagement.domain.model;

/**
 * Describes an ontology with name, short name and description.
 */
public enum Ontology {
  NCBI_TAXONOMY("NCBI organismal classification", "NCBITaxon", "The"
      + "NCBITaxon ontology is an automatic translation of the NCBI taxonomy database. The "
      + "translation treats each taxon as an obo/owl class whose instances (for most branches of "
      + "the ontology) would be individual organisms."),
  BIOASSAY_ONTOLOGY("BioAssay Ontology",       "bao_complete",
      "Contains sample preparations."),
  PLANT_ONTOLOGY("Plant Ontology", "po", "Plant ontology"),
  BRENDA_TISSUE_ONTOLOGY("Brenda Tissue / Enzyme Ontology", "bto",
      "Contains tissues, cell line, cell types, etc.");

  private final String name;
  private final String abbreviation;
  private final String description;

  private Ontology(String name, String abbreviation, String description) {
    this.name = name;
    this.abbreviation = abbreviation;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getAbbreviation() {
    return abbreviation;
  }

  public String getDescription(){
    return description;
  }
}
