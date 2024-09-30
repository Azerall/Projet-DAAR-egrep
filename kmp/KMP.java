import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

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
                return i-j;  // Motif trouvé
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
    public static void searchInFile(String filePath, String pattern) {
        carryOver(pattern);
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 1;

            while ((line = br.readLine()) != null) {
                // Recherche du motif dans la ligne courante avec KMP
                if (rechercheKMP(line, pattern) != -1) {
                    System.out.println(lineNumber + ":" + line);
                }
                lineNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        /*String pattern = "mamamia";
        String text = "maman mamé mia ! mm maaah !";

        int[] retenue = calculRetenue(pattern);
        System.out.println("LTS = " + tabToString(retenue));

        int[] carryOver = carryOver(pattern);
        System.out.println("CarryOver = " + tabToString(carryOver));*/

        String chemin = "41011-0.txt";
        String pattern = "Casey";
        searchInFile(chemin, pattern);
    }

}
