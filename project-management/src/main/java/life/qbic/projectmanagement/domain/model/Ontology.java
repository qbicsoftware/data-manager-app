package life.qbic.projectmanagement.domain.model;

import java.util.Arrays;

/**
 * Describes an ontology with name, short name and description.
 */
public enum Ontology {
  NCBI_TAXONOMY("NCBI organismal classification", "NCBITaxon", "The"
      + "NCBITaxon ontology is an automatic translation of the NCBI taxonomy database. The "
      + "translation treats each taxon as an obo/owl class whose instances (for most branches of "
      + "the ontology) would be individual organisms."),
  ALZHEIMER_DISEASE_ONTOLOGY("Alzheimer's disease ontology", "ado",
      "ADO is a first attempt to develop an open, public ontology representing relevant knowledge on Alzheimer’s disease."),
  EXPERIMENTAL_FACTOR_ONTOLOGY("Experimental Factor Ontology", "efo",
      "The Experimental Factor Ontology (EFO) is an application focused ontology modelling the experimental variables in multiple resources at the EBI and Open Targets."),
  MASS_SPECTROMETRY_ONTOLOGY("Mass Spectrometry Ontology", "ms",
      "A structured controlled vocabulary for the annotation of experiments concerned with proteomics mass spectrometry."),
  NATIONAL_CANCER_INSTITUTE_THESAURUS("National Cancer Institute Thesaurus", "ncit",
      "Vocabulary for clinical care, translational and basic research, and public information and administrative activities."),
  BIOASSAY_ONTOLOGY("BioAssay Ontology",       "bao",
      "Contains sample preparations."),
  PLANT_ONTOLOGY("Plant Ontology", "po", "Plant ontology"),
  BRENDA_TISSUE_ONTOLOGY("Brenda Tissue / Enzyme Ontology", "bto",
      "Contains tissues, cell line, cell types, etc."),
  UNKNOWN_ONTOLOGY("Unknown Ontology", "", "We don't have information about this ontology yet.");

  private final String name;
  private final String abbreviation;
  private final String description;

  Ontology(String name, String abbreviation, String description) {
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

  public static Ontology findOntologyByAbbreviation(String abbreviation) {
    return Arrays.stream(Ontology.values()).filter(o ->
            o.getAbbreviation().equalsIgnoreCase(abbreviation)).findFirst()
        .orElse(Ontology.UNKNOWN_ONTOLOGY);
  }
}
