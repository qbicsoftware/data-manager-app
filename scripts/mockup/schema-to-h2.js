#!/usr/bin/env node
/**
 * Translates the repository's MariaDB DDL into the subset of it H2 accepts in
 * MODE=MySQL, so the application can run against an embedded database.
 *
 * WHY THIS EXISTS
 *   sql/complete-schema.sql is the authoritative schema, but it is written for
 *   MariaDB. H2 in MySQL mode accepts most of it and rejects a handful of
 *   constructs. Maintaining a second, hand-written schema would drift within a
 *   release; rewriting the real file at load time cannot.
 *
 * IF THIS FAILS
 *   The mock database load prints the offending statement. Add a rule here --
 *   do not change sql/complete-schema.sql, which production depends on.
 */
const fs = require('fs');

const [, , input, output] = process.argv;
if (!input || !output) {
  console.error('usage: schema-to-h2.js <complete-schema.sql> <out.sql>');
  process.exit(1);
}

const NEWLINE = String.fromCharCode(10);

let sql = fs.readFileSync(input, 'utf8');

// Strip whole-line -- comments. Trailing comments are left alone: they only
// appear inside string literals, where removing them would corrupt the value.
sql = sql
  .split(NEWLINE)
  .filter((line) => !line.trim().startsWith('--'))
  .join(NEWLINE);

/**
 * Each rule is [RegExp, replacement]. Order matters: the dangling-comma rule
 * must run last, because the index-removal rules above it are what leave the
 * commas behind.
 */
const rules = [
  // Column-level COMMENT '...'. H2 has no such clause.
  [new RegExp(String.raw`\s+COMMENT\s+'(?:[^'\\]|\\.)*'`, 'gi'), ''],

  // json_valid(...) CHECK constraints. H2's function has different semantics.
  [new RegExp(String.raw`CHECK\s*\(\s*json_valid\([^)]*\)\s*\)`, 'gi'), ''],

  // Index declarations declared inline in CREATE TABLE. H2 builds its own.
  [new RegExp(String.raw`^\s*FULLTEXT\s+KEY[^,\n]*,[?\s]*$`, 'gim'), ''],
  [new RegExp(String.raw`^\s*FULLTEXT\s+KEY[^\n]*$`, 'gim'), ''],
  [new RegExp(String.raw`^\s*(UNIQUE\s+)?KEY\s*\([^)]*\)\s*,?\s*$`, 'gim'), ''],

  // MySQL bit literals.
  [new RegExp(String.raw`b'([01])'`, 'gi'), '$1'],

  // Generated-column storage keywords.
  [new RegExp(String.raw`\s+VIRTUAL\b`, 'gi'), ''],
  [new RegExp(String.raw`\s+STORED\b`, 'gi'), ''],

  // Unsigned integers and display widths.
  [new RegExp(String.raw`BIGINT\s+UNSIGNED`, 'gi'), 'BIGINT'],
  [new RegExp(String.raw`INT\(11\)|INT\(20\)`, 'gi'), 'INT'],

  // MyISAM fulltext tables have no H2 equivalent.
  [new RegExp(String.raw`ENGINE\s*=\s*MyISAM`, 'gi'), 'ENGINE = InnoDB'],

  // A comma immediately before the closing paren, left by the rules above.
  [new RegExp(String.raw`,(\s*\))`, 'g'), '$1'],
];

for (const [pattern, replacement] of rules) {
  sql = sql.replace(pattern, replacement);
}

// The index rules above consume their own newline; this collapses any runs of
// blank lines that leaves behind, so failures point at real statements.
sql = sql.replace(new RegExp(NEWLINE + '{3,}', 'g'), NEWLINE + NEWLINE);

fs.writeFileSync(output, sql);
console.log('wrote ' + output + ' (' + sql.length + ' bytes)');
