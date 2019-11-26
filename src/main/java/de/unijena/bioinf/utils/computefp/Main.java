package de.unijena.bioinf.utils.computefp;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MolecularProperty;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

import static net.sf.jniinchi.INCHI_RET.OKAY;
import static net.sf.jniinchi.INCHI_RET.WARNING;

public class Main {

    public static void main(String[] args) {
        final CdkFingerprintVersion cdk = CdkFingerprintVersion.getDefault();
        FixedFingerprinter fingerprinter = new FixedFingerprinter(cdk);
        if (args.length==0 || args[0].startsWith("-h") || args[0].startsWith("--help")) {
            System.out.println("Usage: java -jar computefp.jar [INPUTFILES]\nwith INPUTFILES is a list of text files containing one SMILES per line.\nOutputs one file per input file with ending .fp that contains InChI, InChI-key, SMILES and fingerprint");
            return;
        }
        final HashMap<String,String> reports = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--info")) {
                for (int k=0; k < cdk.size(); ++k) {
                    System.out.println(k + "\t" + cdk.getMolecularProperty(k).getDescription());
                }
                if (args.length==1) return;
                continue;
            }
            int computed = 0;
            int total = 0;
            final File source = new File(arg);
            if (!source.isFile()) {
                System.err.println("Cannot read file '" + source + "'");
                continue;
            }
            File out = new File(arg + ".fp");
            System.out.println("Write " + out);
            try (final BufferedWriter bw = Files.newBufferedWriter(out.toPath())) {
                try (final BufferedReader r = Files.newBufferedReader(source.toPath())) {
                    String line;
                    final SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
                    final InChIGeneratorFactory inChIGeneratorFactory = InChIGeneratorFactory.getInstance();
                    while ((line=r.readLine())!=null) {
                        ++total;

                        int tab = line.lastIndexOf('\t');
                        final String prefix, smiles;
                        if (tab>=0) {
                            prefix = line.substring(0,tab)+"\t";
                            smiles = line.substring(tab+1,line.length());
                        } else {
                            prefix="";
                            smiles = line;
                        }
                        try {
                            IAtomContainer molecule = parser.parseSmiles(smiles);
                            ArrayFingerprint fp = fingerprinter.computeFingerprint(molecule);
                            InChIGenerator gen = inChIGeneratorFactory.getInChIGenerator(molecule);
                            if (gen.getReturnStatus()!=OKAY) {
                                if (gen.getReturnStatus()==WARNING) {
                                    System.err.println(smiles + ": " + gen.getMessage());
                                } else {
                                    System.err.println("Cannot compute InChI for instance '" + smiles + "'");
                                    continue;
                                }
                            }
                            InChI inchi = new InChI(gen.getInchiKey(),gen.getInchi());
                            bw.write(prefix);
                            bw.write(inchi.key2D());
                            bw.write('\t');
                            bw.write(inchi.in2D);
                            bw.write('\t');
                            bw.write(smiles);
                            bw.write('\t');
                            bw.write(fp.toCommaSeparatedString());
                            bw.newLine();
                            ++computed;
                        } catch (InvalidSmilesException e) {
                            System.err.println("Invalid SMILES: '" + smiles + "'");
                            e.printStackTrace();
                        }

                    }
                } catch (CDKException e) {
                    System.err.println("Error while loading InChI library.");
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            int missingCompounds = total-computed;
            final String report = "Computed fingerprints for " + computed + " of " + total + " SMILES in file " + arg + ". " + missingCompounds + " are missing.";
            System.out.println(report);
            reports.put(arg, report);
        }
        System.out.println("Done.");
        for (String report : reports.values()) {
            System.out.println(report);
        }

    }

}
