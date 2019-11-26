# computefp
Contains the code for computing fingerprints for CSI:FingerID

## install
```
gradle installDist
```

## usage
```
computefp/bin/computefp [INPUTFILES]

```
where INPUTFILES is a list of files containing SMILES.
The files can either be text files with one SMILES per line, or
TAB separated text files with SMILES as last column. The previous columns
are then treated as metadata (e.g. Ids or names).

## output

For each input file a new file with same path but different ending (.fp) is created. The file is TAB separated, with the first columns are the metadata (if given), first 14 characters of the InChI-Key, the 2D InChI, the SMILES, and the fingerprint.

The fingerprint is given as comma separated list of indizes. Note that the definition of fingerprints might change with 

## fingerprint definitions

To get a list of all indizes with their meaning, run
```
computefp/bin/computefp --info
```