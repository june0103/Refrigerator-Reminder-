param(
    [Parameter(Mandatory = $true)]
    [string]$MfdsInput,
    [string]$Targets = "$PSScriptRoot\mfds_targets.json",
    [string]$ManualEntries = "$PSScriptRoot\manual_seed_entries.json",
    [string]$Output = "$PSScriptRoot\..\..\app\src\main\assets\ingredient_dictionary_seed.json",
    [string]$NameField,
    [string]$CodeField,
    [string[]]$LookupFields,
    [switch]$AllowMissingTargets
)

$ErrorActionPreference = "Stop"

function Normalize-IngredientName {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return ""
    }

    return ([regex]::Replace($Value, "[\s\(\)\[\]\{\}\-_/.,]+", "")).ToLowerInvariant()
}

function Build-HashedId {
    param([string]$Value)

    $normalized = Normalize-IngredientName $Value
    $sha1 = [System.Security.Cryptography.SHA1]::Create()
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($normalized)
        $hash = $sha1.ComputeHash($bytes)
        $hex = -join ($hash | ForEach-Object { $_.ToString("x2") })
        return "mfds_$($hex.Substring(0, 12))"
    } finally {
        $sha1.Dispose()
    }
}

function Get-JsonArray {
    param([string]$Path)

    $content = Get-Content -Raw -Encoding utf8 $Path | ConvertFrom-Json
    if ($content -is [System.Array]) {
        return $content
    }
    throw "JSON array expected: $Path"
}

function Get-NestedJsonRows {
    param($Node)

    if ($Node -is [System.Array]) {
        return $Node
    }

    if ($null -eq $Node) {
        return @()
    }

    foreach ($name in @("items", "data", "body", "rows", "result")) {
        $property = $Node.PSObject.Properties[$name]
        if ($null -ne $property) {
            $nested = Get-NestedJsonRows $property.Value
            if ($nested.Count -gt 0) {
                return $nested
            }
        }
    }

    return @()
}

function Get-MfdsRows {
    param([string]$Path)

    $extension = [System.IO.Path]::GetExtension($Path).ToLowerInvariant()
    switch ($extension) {
        ".csv" { return Import-Csv -Path $Path -Encoding utf8 }
        ".xlsx" { return Get-XlsxRows -Path $Path }
        ".json" {
            $json = Get-Content -Raw -Encoding utf8 $Path | ConvertFrom-Json
            $rows = Get-NestedJsonRows $json
            if ($rows.Count -eq 0) {
                throw "Usable row array not found in JSON: $Path"
            }
            return $rows
        }
        default { throw "Unsupported MFDS input format: $extension" }
    }
}

function Resolve-Field {
    param(
        [System.Array]$Rows,
        [string]$Explicit,
        [string[]]$Candidates,
        [string]$Label,
        [switch]$Optional
    )

    $available = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($row in $Rows) {
        foreach ($property in $row.PSObject.Properties.Name) {
            [void]$available.Add($property)
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($Explicit)) {
        if (-not $available.Contains($Explicit)) {
            throw "$Label field '$Explicit' was not found."
        }
        return $Explicit
    }

    foreach ($candidate in $Candidates) {
        if ($available.Contains($candidate)) {
            return $candidate
        }
    }

    if ($Optional) {
        return $null
    }

    throw "Could not detect $Label field. Available fields: $([string]::Join(', ', $available))"
}

function Get-ZipEntryText {
    param(
        [System.IO.Compression.ZipArchive]$Archive,
        [string]$EntryPath
    )

    $entry = $Archive.GetEntry($EntryPath)
    if ($null -eq $entry) {
        throw "Zip entry not found: $EntryPath"
    }

    $stream = $entry.Open()
    $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8, $true)
    try {
        return $reader.ReadToEnd()
    } finally {
        $reader.Close()
        $stream.Close()
    }
}

function Get-FirstWorksheetPath {
    param([System.IO.Compression.ZipArchive]$Archive)

    [xml]$workbook = Get-ZipEntryText -Archive $Archive -EntryPath "xl/workbook.xml"
    [xml]$relationships = Get-ZipEntryText -Archive $Archive -EntryPath "xl/_rels/workbook.xml.rels"

    $workbookNs = New-Object System.Xml.XmlNamespaceManager($workbook.NameTable)
    $workbookNs.AddNamespace("x", "http://schemas.openxmlformats.org/spreadsheetml/2006/main")
    $workbookNs.AddNamespace("r", "http://schemas.openxmlformats.org/officeDocument/2006/relationships")
    $sheet = $workbook.SelectSingleNode("//x:sheets/x:sheet[1]", $workbookNs)
    if ($null -eq $sheet) {
        throw "Worksheet not found in workbook."
    }

    $relationshipId = $sheet.GetAttribute("id", "http://schemas.openxmlformats.org/officeDocument/2006/relationships")
    if ([string]::IsNullOrWhiteSpace($relationshipId)) {
        throw "Worksheet relationship id not found."
    }

    $relsNs = New-Object System.Xml.XmlNamespaceManager($relationships.NameTable)
    $relsNs.AddNamespace("r", "http://schemas.openxmlformats.org/package/2006/relationships")
    $relationship = $relationships.SelectSingleNode("//r:Relationship[@Id='$relationshipId']", $relsNs)
    if ($null -eq $relationship) {
        throw "Worksheet relationship '$relationshipId' not found."
    }

    return "xl/$($relationship.Target)"
}

function Get-XlsxSharedStrings {
    param([System.IO.Compression.ZipArchive]$Archive)

    $sharedEntry = $Archive.GetEntry("xl/sharedStrings.xml")
    if ($null -eq $sharedEntry) {
        return (New-Object "System.Collections.Generic.List[string]")
    }

    [xml]$sharedXml = Get-ZipEntryText -Archive $Archive -EntryPath "xl/sharedStrings.xml"
    $ns = New-Object System.Xml.XmlNamespaceManager($sharedXml.NameTable)
    $ns.AddNamespace("x", "http://schemas.openxmlformats.org/spreadsheetml/2006/main")
    $sharedStrings = New-Object "System.Collections.Generic.List[string]"

    foreach ($node in $sharedXml.SelectNodes("//x:si", $ns)) {
        $parts = New-Object "System.Collections.Generic.List[string]"
        foreach ($textNode in $node.SelectNodes(".//x:t", $ns)) {
            $parts.Add([string]$textNode.InnerText)
        }
        $sharedStrings.Add(($parts -join ""))
    }

    return $sharedStrings
}

function Get-XlsxCellValue {
    param(
        $Cell,
        [System.Collections.Generic.List[string]]$SharedStrings
    )

    $cellType = [string]$Cell.t
    switch ($cellType) {
        "s" {
            if ([string]::IsNullOrWhiteSpace([string]$Cell.v)) {
                return ""
            }
            return $SharedStrings[[int]$Cell.v]
        }
        "inlineStr" { return [string]$Cell.is.t }
        "str" { return [string]$Cell.v }
        default { return [string]$Cell.v }
    }
}

function Get-XlsxRows {
    param([string]$Path)

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $Path))
    try {
        $sharedStrings = Get-XlsxSharedStrings -Archive $archive
        $worksheetPath = Get-FirstWorksheetPath -Archive $archive
        [xml]$worksheet = Get-ZipEntryText -Archive $archive -EntryPath $worksheetPath

        $ns = New-Object System.Xml.XmlNamespaceManager($worksheet.NameTable)
        $ns.AddNamespace("x", "http://schemas.openxmlformats.org/spreadsheetml/2006/main")
        $rowNodes = $worksheet.SelectNodes("//x:sheetData/x:row", $ns)
        if ($rowNodes.Count -eq 0) {
            return @()
        }

        $headersByColumn = @{}
        foreach ($headerCell in $rowNodes[0].SelectNodes("./x:c", $ns)) {
            $columnRef = ([string]$headerCell.r) -replace "\d", ""
            $headersByColumn[$columnRef] = Get-XlsxCellValue -Cell $headerCell -SharedStrings $sharedStrings
        }

        $rows = New-Object "System.Collections.Generic.List[object]"
        for ($rowIndex = 1; $rowIndex -lt $rowNodes.Count; $rowIndex++) {
            $properties = [ordered]@{}
            foreach ($cell in $rowNodes[$rowIndex].SelectNodes("./x:c", $ns)) {
                $columnRef = ([string]$cell.r) -replace "\d", ""
                $header = [string]$headersByColumn[$columnRef]
                if ([string]::IsNullOrWhiteSpace($header)) {
                    continue
                }
                $properties[$header] = Get-XlsxCellValue -Cell $cell -SharedStrings $sharedStrings
            }

            if ($properties.Count -gt 0) {
                $rows.Add([pscustomobject]$properties)
            }
        }

        return $rows.ToArray()
    } finally {
        $archive.Dispose()
    }
}

function ConvertTo-OutputEntry {
    param(
        $Row,
        $Target,
        [string]$NameField
    )

    $rawName = [string]$Row.$NameField
    $displayName = if ([string]::IsNullOrWhiteSpace($Target.displayName)) { $rawName.Trim() } else { ([string]$Target.displayName).Trim() }
    $entryId = if ([string]::IsNullOrWhiteSpace($Target.id)) { Build-HashedId $rawName } else { [string]$Target.id }
    $aliases = @()
    if ($null -ne $Target.aliases) {
        $aliases = @($Target.aliases)
    }

    return [ordered]@{
        id = $entryId
        source = "MFDS"
        rawName = $rawName.Trim()
        displayName = $displayName
        aliases = $aliases
        category = [string]$Target.category
        searchPriority = [int]$Target.searchPriority
    }
}

function Get-RowSelectionScore {
    param(
        $Row,
        [string]$NameField
    )

    $nameValue = [string]$Row.$NameField
    if ([string]::IsNullOrWhiteSpace($nameValue)) {
        return [int]::MinValue
    }

    $delimiterCount = ([regex]::Matches($nameValue, "[,_]")).Count
    return (0 - ($nameValue.Length * 2) - $delimiterCount)
}

function Assert-NoDuplicates {
    param([System.Array]$Entries)

    $idMap = @{}
    $nameMap = @{}

    foreach ($entry in $Entries) {
        $entryId = [string]$entry.id
        $normalizedName = Normalize-IngredientName ([string]$entry.displayName)

        if ($idMap.ContainsKey($entryId)) {
            throw "Duplicate id detected: $entryId"
        }
        $idMap[$entryId] = $true

        if ($nameMap.ContainsKey($normalizedName)) {
            throw "Duplicate displayName/normalizedName detected: $($entry.displayName)"
        }
        $nameMap[$normalizedName] = $true
    }
}

$nameCandidates = @("DESC_KOR", "FOOD_NM_KR", "foodName", "name")
$codeCandidates = @("FOOD_CD", "foodCode", "code", "id")

$rows = Get-MfdsRows $MfdsInput
if ($rows.Count -eq 0) {
    throw "MFDS input is empty."
}

$resolvedNameField = Resolve-Field -Rows $rows -Explicit $NameField -Candidates $nameCandidates -Label "name"
$resolvedCodeField = Resolve-Field -Rows $rows -Explicit $CodeField -Candidates $codeCandidates -Label "code" -Optional
$resolvedLookupFields = @()
if ($null -ne $LookupFields -and $LookupFields.Count -gt 0) {
    foreach ($lookupField in $LookupFields) {
        if ([string]::IsNullOrWhiteSpace($lookupField)) {
            continue
        }
        $resolvedLookupFields += (Resolve-Field -Rows $rows -Explicit $lookupField -Candidates @() -Label "lookup")
    }
}
if ($resolvedLookupFields.Count -eq 0) {
    $resolvedLookupFields = @($resolvedNameField)
}
$targetItems = Get-JsonArray $Targets
$manualItems = Get-JsonArray $ManualEntries

$rowsByLookup = @{}
$rowScoresByLookup = @{}
foreach ($row in $rows) {
    foreach ($lookupField in $resolvedLookupFields) {
        $rawValue = [string]$row.$lookupField
        $normalized = Normalize-IngredientName $rawValue
        if ([string]::IsNullOrWhiteSpace($normalized)) {
            continue
        }

        $candidateScore = Get-RowSelectionScore -Row $row -NameField $resolvedNameField
        if (
            -not $rowsByLookup.ContainsKey($normalized) -or
            $candidateScore -gt $rowScoresByLookup[$normalized]
        ) {
            $rowsByLookup[$normalized] = $row
            $rowScoresByLookup[$normalized] = $candidateScore
        }
    }
}

$mfdsEntries = New-Object System.Collections.Generic.List[object]
$missingTargets = New-Object System.Collections.Generic.List[object]

foreach ($target in $targetItems) {
    $lookupCandidates = New-Object System.Collections.Generic.List[string]
    if (-not [string]::IsNullOrWhiteSpace([string]$target.lookupName)) {
        $lookupCandidates.Add([string]$target.lookupName)
    }
    if ($null -ne $target.aliases) {
        foreach ($alias in @($target.aliases)) {
            $lookupCandidates.Add([string]$alias)
        }
    }
    if ($null -ne $target.lookupAliases) {
        foreach ($alias in @($target.lookupAliases)) {
            $lookupCandidates.Add([string]$alias)
        }
    }

    $matchedRow = $null
    foreach ($candidate in $lookupCandidates) {
        $normalizedCandidate = Normalize-IngredientName $candidate
        if ($rowsByLookup.ContainsKey($normalizedCandidate)) {
            $matchedRow = $rowsByLookup[$normalizedCandidate]
            break
        }
    }

    if ($null -eq $matchedRow) {
        $missingTargets.Add($target)
        continue
    }

    $mfdsEntries.Add((ConvertTo-OutputEntry -Row $matchedRow -Target $target -NameField $resolvedNameField))
}

if ($missingTargets.Count -gt 0 -and -not $AllowMissingTargets) {
    $missingLines = $missingTargets | ForEach-Object { " - $($_.lookupName)" }
    throw "Missing MFDS targets:`n$($missingLines -join [Environment]::NewLine)"
}

$finalEntries = @($mfdsEntries.ToArray()) + @($manualItems)
$finalEntries = $finalEntries | Sort-Object searchPriority, displayName
Assert-NoDuplicates $finalEntries

$outputDirectory = Split-Path -Parent $Output
if (-not (Test-Path $outputDirectory)) {
    New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
}

$finalEntries | ConvertTo-Json -Depth 10 | Set-Content -Encoding utf8 $Output

Write-Host "Generated $($finalEntries.Count) entries ($($mfdsEntries.Count) MFDS, $($manualItems.Count) manual) -> $Output"
if ($missingTargets.Count -gt 0) {
    Write-Warning "Skipped $($missingTargets.Count) missing MFDS targets."
}
