# Ingredient Seed Generator

This tool builds the app seed file at `app/src/main/assets/ingredient_dictionary_seed.json`
from two sources:

- curated MFDS lookup targets in `mfds_targets.json`
- manual 생활형 식재료 entries in `manual_seed_entries.json`

## Files

- `generate_seed.ps1`
  - PowerShell generator
- `mfds_targets.json`
  - MFDS-backed ingredient targets to match against the official raw-material dataset
- `manual_seed_entries.json`
  - manual-only entries for household staples that are not covered well by the MFDS raw-material export
- `sample_mfds_export.csv`
  - small sample input for local verification
- `mfds_raw_materials.xlsx`
  - one-time downloaded official MFDS raw-material workbook snapshot

## Supported Input Formats

- `.csv`
- `.json`
- `.xlsx`

## Typical Usage

Sample CSV:

```powershell
.\tools\ingredient_seed\generate_seed.ps1 `
  -MfdsInput .\tools\ingredient_seed\sample_mfds_export.csv
```

Official MFDS raw-material workbook:

```powershell
.\tools\ingredient_seed\generate_seed.ps1 `
  -MfdsInput .\tools\ingredient_seed\mfds_raw_materials.xlsx `
  -NameField '식품명' `
  -CodeField '식품코드' `
  -LookupFields '식품명','대표식품명'
```

## Matching Rules

- target matching uses `lookupName` first, then `lookupAliases`
- the generator normalizes spaces, brackets, separators, and casing before comparison
- when `LookupFields` is provided, a target can match any of those worksheet columns
- if an exact normalized match is not found for a target, generation fails by default

## Notes

- the repo intentionally does not import the entire MFDS dataset into the app
- the app seed stays limited to `selected raw ingredients + manual household staples`
- output entries are sorted by `searchPriority`, then `displayName`
