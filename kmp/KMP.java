package kmp;

import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class KMP {

    private static int[] retenue;

    private static int[] calculRetenue(String pattern) {
        int m = pattern.length();
        retenue = new int[m+1];

        int length = 0;
        retenue[0] = -1; // Ajout de -1 au début du tableau
        retenue[1] = 0; // LPS[1] est toujours 0

        int i = 1;
        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(length)) {
                length++;
                retenue[i+1] = length;
                i++;
            } else {
                if (length != 0) {
                    length = retenue[length];
                } else {
                    retenue[i+1] = 0;
                    i++;
                }
            }
        }

        return retenue;
    }

    public static int[] carryOver(String pattern){
        retenue = calculRetenue(pattern);
        for (int i=1; i<pattern.length(); i++){
            if (retenue[i]>=0 && retenue[i]<pattern.length()){
                if (pattern.charAt(i)==pattern.charAt(retenue[i]) && retenue[retenue[i]]==-1){
                    retenue[i] = -1;
                }
                else if (pattern.charAt(i)==pattern.charAt(retenue[i]) && retenue[retenue[i]]!=-1){
                    retenue[i] = retenue[retenue[i]];
                }
            }
        }
        return retenue;
    }

    public static String tabToString(int[] tab) {
        String s = "[";
        for(int i=0; i < tab.length; i++ ){
            s += tab[i] + ",";
        } 
        s = s.substring(0, s.length()-1);
        s += "]";
        return s;
    }

    // Algorithme KMP pour rechercher un motif dans un texte
    public static int rechercheKMP(String texte, String pattern) {
        int i = 0;  // index pour le texte
        int j = 0;  // index pour le pattern

        while (i < texte.length()) {
            if (pattern.charAt(j) == texte.charAt(i)) {
                i++;
                j++;
            }
            if (j == pattern.length()) {
                return i-j;  // Motif trouvé à l'index i-j
            } else if (i < texte.length() && pattern.charAt(j) != texte.charAt(i)) {
                if (j != 0) {
                    j = retenue[j];
                    if (j == -1) {
                        j = 0;
                    }
                } else {
                    i++;
                }
            }
        }
        return -1;  // Motif non trouvé
    }

    // Fonction pour lire un fichier ligne par ligne et chercher un motif avec KMP
    public static int searchInFile(File file, String pattern) {
        carryOver(pattern); // Calculer la retenue pour le motif
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int nbLignes = 0;

            while ((line = br.readLine()) != null) {
                // Recherche du motif dans la ligne courante avec KMP
                rechercheKMP(line, pattern);
                /*if (rechercheKMP(line, pattern) != -1) {
                    System.out.println(nbLignes+1 + ":" + line);
                }*/
                nbLignes++;
            }

            return nbLignes;
        } catch (IOException e) {
            System.out.println("Une erreur s'est produite lors de la lecture du fichier : " + file.getName());
            e.printStackTrace();
        }
        return 0;
    }

    public static void main(String[] args) {
        
        // Motif à chercher pour les tests sur le temps d'exécution
        //String pattern = " ";
        String pattern = "abcdefghijklmnopqrstuvwxyz";

        // Tableau de motifs pour les tests sur la consommation mémoire
        String patterns[] = {
            "a",
            "abc",
            "abcdef",
            "abcdefgh",
            "abcdefghijkl",
            "abcdefghijklmnop",
            "abcdefghijklmnopqrstuv",
            "abcdefghijklmnopqrstuvwxyz",
            "abcdefghijklmnopqrstuvwxyz123456",
            "abcdefghijklmnopqrstuvwxyz1234567890",
            "abcdefghijklmnopqrstuvwxyz1234567890&éèàç",
            "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmno",
            "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz",
            "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890"
        };

        File dir = new File("./testbeds"); // Répertoire contenant les fichiers à lire

        // Chemin des fichiers CSV de sortie
        String csvFileTime = "./kmp/kmp_time_results.csv";
        String csvFileMemory = "./kmp/kmp_memory_results.csv";
    
        // En-têtes des colonnes du fichier CSV
        String[] headersTime = {
            "Nombre de lignes", 
            "Temps d'exécution",
        };
        String[] headersMemory = {
            "Longueur du pattern", 
            "Consommation mémoire",
        };

        // Tableau pour stocker les données à écrire dans le fichier CSV
        String[][] dataTime;
        String[][] dataMemory;

        // Formater les données
        int nbLignes = 0;
        long startTime, endTime, memoryBefore, memoryAfter;
        double duration, consommation;
        int i = 0;
        int nbIterations = 50;

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles(); // Liste des fichiers dans le répertoire

            // Mesure du temps moyen en µs
            dataTime = new String[files.length][2];
            for (File file : files) {
                if (file.canRead() && file.isFile()) {
                    System.out.println("Fichier " + (i+1) + "/" + files.length + " : "+ file.getName());
                    nbLignes = searchInFile(file, pattern);
                    
                    startTime = System.nanoTime();
                    for (int j=0; j<nbIterations; j++) {
                        searchInFile(file, pattern);
                    }
                    endTime = System.nanoTime();
                    duration = (endTime - startTime) / 1000.0 / nbIterations;

                    // Stocker les résultats dans le tableau de données
                    String[] row = {
                        nbLignes+"", 
                        duration+"",
                    };
                    dataTime[i] = row;
                    i++;
                }
            }
            // Créer le fichier CSV pour les temps d'exécution
            try (FileWriter writer = new FileWriter(csvFileTime)) {
                // Écriture des en-têtes dans le fichier CSV
                writer.append(String.join(",", headersTime));
                writer.append("\n");

                // Écriture des données dans le fichier CSV
                for (String[] row : dataTime) {
                    writer.append(String.join(",", row));
                    writer.append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Mesure de la consommation mémoire moyen en octets
            dataMemory = new String[patterns.length][2];
            for (int j = 0; j < patterns.length; j++) {
                System.out.println("Pattern " + (j+1) + "/" + patterns.length + " : "+ patterns[j]);

                memoryBefore = 0;
                memoryAfter = 0;
                for (int k=0; k<nbIterations; k++) {
                    System.gc(); // Forcer l'exécution du garbage collector avant de prendre des mesures
                    memoryBefore += getMemoryUsage();
                    searchInFile(files[0], patterns[j]);
                    memoryAfter += getMemoryUsage();
                }
                consommation = memoryAfter - memoryBefore / nbIterations;
                
                // Stocker les résultats dans le tableau de données
                String[] row = {
                    patterns[j].length()+"",
                    consommation+"",
                };
                dataMemory[j] = row;
                i++; 
            }
            // Créer le fichier CSV pour la consommation mémoire
            try (FileWriter writer = new FileWriter(csvFileMemory)) {
                // Écriture des en-têtes dans le fichier CSV
                writer.append(String.join(",", headersMemory));
                writer.append("\n");

                // Écriture des données dans le fichier CSV
                for (String[] row : dataMemory) {
                    writer.append(String.join(",", row));
                    writer.append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Fichiers CSV créés avec succès.");
        } else {
            System.out.println("Le répertoire n'existe pas ou n'est pas un répertoire.");
        }
    }

    // Méthode pour obtenir la quantité de mémoire utilisée par le programme
    public static long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
