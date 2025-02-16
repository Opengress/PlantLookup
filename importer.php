#!/usr/bin/env php
<?php

/*
It's not morally correct for me to ship the NVS code list in the application due to
the usage/licence terms on the download page.
I did, in the early test version, and this is the script I used to process them.
The script is not used now, and is left here purely for historical reference.

Today, the NVS codes are downloaded and processed on the client devices.
This means slightly more traffic for Landcare Research,
but a truer representation of their data and, critically,
greater respect for their wishes.
*/

// for when there's a "?" in the name and you want the proper string with macron. fetch from nvs
function fix_vernacular_names($id) {
    $url = $cache = '/tmp/nvs_' . $id;
    if (!file_exists($url)) {
        $url = 'https://api-web-nvs.landcareresearch.co.nz/api/authority/taxons/' . $id;
        $dirty = true;
    }

    $xml = file_get_contents($url);
    if (!empty($dirty)) {
        echo 'wrote to disk: ' . file_put_contents($cache, $xml) . ' for ' . $id . PHP_EOL;
        sleep(1);
    }

    return json_decode($xml)->VernacularNames;
}

// reduces a string to a pure lowercase alphabetic representation to normalize lookups in array keys
function norm($str) {
    return preg_replace('@\W@', '', strtolower($str));
}

// Basically just removes any diacritics etc so we can add eg "Kamahi" for searching etc
function clean($str) {
    $normalizedStr = normalizer_normalize($str, Normalizer::FORM_D);
    return preg_replace('/\p{Mn}/u', '', $normalizedStr);
}

$types = [
    'vascular' => 1,
    'nonvascular' => 2
];

$biostatuses = [
    'absent' => 1,
    'exotic' => 2,
    'indigenousendemic' => 3,
    'indigenousnonendemic' => 4,
    'indigenousunspecified' => 5,
    'uncertain' => 6,
    'unknown' => 7
];

$growthforms = [
    'fern' => 1,
    'forb' => 2,
    'graminoid' => 3,
    'grasstree' => 4,
    'herbaceousmixed' => 5,
    'mistletoe' => 6,
    'mixed' => 7,
    'nonvascular' => 8,
    'palm' => 9,
    'shrub' => 10,
    'subshrub' => 11,
    'tree' => 12,
    'treefern' => 13,
    'unknown' => 14,
    'vine' => 15,
    'woodymixed' => 16
];

$threatenedstatuses = [
    'coloniser' => 1,
    'datadeficient' => 2,
    'declining' => 3,
    'nationallycritical' => 4,
    'nationallyendangered' => 5,
    'nationallyvulnerable' => 6,
    'naturallyuncommon' => 7,
    'notthreatened' => 8,
    'notthreatenedassumed' => 9,
    'recovering' => 10,
    'relict' => 11,
    'taxonomicallyindistinct' => 12,
    'unknown' => 13,
    'vagrant' => 14
];

$NVSCode = 0; // TaxonSpeciesCode
$TaxonID = 1; // TaxonID
$SpeciesName = 2; // TaxonSpeciesName
$Family = 3; // RankFamily
$Genus = 4; // RankGenus
$Species = 5; // RankSpecies
$BioStatus = 6; // TaxonBioStatus
$PlantType = 7; // TaxonPlantType
$Palatibility = 8; // TaxonPalatability
$GrowthForm = 9; // TaxonGrowthForm
$PreferredTaxonID = 10; // PreferredTaxonID
$PreferredCode = 11; //
$PreferredName = 12; //
$VernacularNames = 13; // VernacularNames
$ThreatenedStatus = 14; // ThreatenedStatus

$handle = fopen("CurrentNVSNames.csv", "r");
extract(array_flip(fgetcsv($handle, 10000, ",")), EXTR_IF_EXISTS);

$synonyms = [];

while (($d = fgets($handle, 10000)) !== FALSE) {
    $d = str_getcsv(mb_convert_encoding($d, 'UTF-8', 'ISO-8859-1'));
    if (str_contains($d[$VernacularNames], '?')) {
        $d[$VernacularNames] = fix_vernacular_names($d[$TaxonID]);
    }

    if ($d[$TaxonID] == $d[$PreferredTaxonID]) {
        $preferredIDs[] = [
            'NVSCode' => $d[$NVSCode],
            'TaxonID' => $d[$TaxonID],
            'SpeciesName' => $d[$SpeciesName],
            'Family' => $d[$Family],
            'Genus' => $d[$Genus],
            'Species' => $d[$Species],
            'BioStatusID' => $biostatuses[norm($d[$BioStatus])],
            'PlantTypeId' => $types[norm($d[$PlantType])],
            'GrowthFormID' => $growthforms[norm($d[$GrowthForm])],
            'ThreatenedStatusID' => $threatenedstatuses[norm($d[$ThreatenedStatus])]
        ];
    }
    $synonyms[$d[$PreferredCode]] = array_unique(array_merge(array_filter(array_map('trim', [$d[$SpeciesName], $d[$NVSCode], ...str_getcsv($d[$VernacularNames], ';')])), $synonyms[$d[$PreferredCode]] ?? []));

}

try {

    $dbpath = './plantNames.db';
    copy($dbpath, $dbpath . '.bak.' . time());
    $db = new PDO('sqlite:' . $dbpath);

    $db->beginTransaction();
    $idstmt = $db->prepare('INSERT OR REPLACE INTO Plants (NVSCode, TaxonID, SpeciesName, Family, Genus, Species, BioStatusID, PlantTypeID, GrowthFormID, ThreatenedStatusID) VALUES (:NVSCode, :TaxonID, :SpeciesName, :Family, :Genus, :Species, :BioStatusID, :PlantTypeId, :GrowthFormID, :ThreatenedStatusID)');
    $namesstmt = $db->prepare('INSERT OR IGNORE INTO Aliases (PlantNVSCode, AliasName) VALUES (:PlantNVSCode, :AliasName)');

    foreach ($preferredIDs as $plant) {
        $idstmt->execute($plant);
    }

    foreach ($synonyms as $id => $synonym) {
        foreach ($syns as $name) {
            $namesstmt->execute(['PlantNVSCode' => $id, 'AliasName' => $name]);
            if (!normalizer_is_normalized($name, Normalizer::FORM_D)) {
                $namesstmt->execute(['PlantNVSCode' => $id, 'AliasName' => clean($name)]);
            }
        }
    }

    $db->commit();

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage();
}


fclose($handle);

