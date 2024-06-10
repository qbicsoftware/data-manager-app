ALTER TABLE ontology_classes
    ENGINE = MyISAM;
ALTER TABLE ontology_classes CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
ALTER TABLE ontology_classes
    ADD FULLTEXT `idx_fulltext_name` (name);
ALTER TABLE ontology_classes
    ADD FULLTEXT `idx_fulltext_label` (label);
