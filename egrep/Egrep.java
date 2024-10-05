import java.io.*;
import java.util.*;

public class Egrep {

    public static void main(String[] args) {

        // Motif à chercher pour les tests sur le temps d'exécution
        String pattern = ".";
        //String pattern = "(a|j|k|p|z|b)*e*o(f|d|g|h|p)";

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

        File dir = new File("../testbeds"); // Répertoire contenant les fichiers à lire
        
        // Chemin des fichiers CSV de sortie
        String csvFileTime = "./egrep_time_results.csv";
        String csvFileMemory = "./egrep_memory_results.csv";
    	
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
            /*dataTime = new String[files.length][2];
            for (File file : files) {
                if (file.canRead() && file.isFile()) {
                    System.out.println("Fichier " + (i+1) + "/" + files.length + " : "+ file.getName());
                    
                    // Compter le nombre de lignes dans le fichier
                    nbLignes = countLinesInFile(file);

                    startTime = System.nanoTime();
                    for (int j=0; j<nbIterations; j++) {
                        executeEgrep(file.getAbsolutePath(), pattern);
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
            }*/

            // Mesure de la consommation mémoire moyen en octets
            dataMemory = new String[patterns.length][2];
            for (int j = 0; j < patterns.length; j++) {
                System.out.println("Pattern " + (j+1) + "/" + patterns.length + " : "+ patterns[j]);

                consommation = 0;
                for (int k=0; k<nbIterations; k++) {
                    consommation += executeEgrep(files[0].getAbsolutePath(), pattern);
                }
                consommation /= nbIterations;

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

    // Méthode pour compter le nombre de lignes dans un fichier
    public static int countLinesInFile(File file) {
        int lines = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            while (br.readLine() != null) {
                lines++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    // Méthode qui éxecute la commande egrep et renvoie la consommation mémoire
    public static long executeEgrep(String filePath, String pattern) {
        try {
            ProcessBuilder pb = new ProcessBuilder("egrep", pattern, filePath);
            pb.redirectErrorStream(true); // Rediriger les erreurs vers la sortie standard

            System.gc(); // Forcer l'exécution du garbage collector avant de prendre des mesures
            long memoryBefore = getMemoryUsage();

            Process process = pb.start();

            // Lire la sortie (facultatif)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // Pour l'instant, on ne fait rien avec les lignes trouvées
            }

            int exitCode = process.waitFor(); // Attendre que le processus se termine
            if (exitCode == 2) {
                System.err.println("Erreur lors de l'exécution de egrep pour le fichier : " + filePath);
            }

            reader.close();
            return getMemoryUsage() - memoryBefore;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Méthode pour obtenir la quantité de mémoire utilisée par le programme
    public static long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}