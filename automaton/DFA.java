package automaton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

class DFA {
    private static int stateId = 0; // Un ID d'état unique pour chaque nouvel état
    int startState;
    List<Integer> finalStates;

    Map<Integer, Map<Integer, Integer>> transitions; // Map des transitions pour chaque état : (source, (symbol, target))
    List<Integer> symbols;

    public DFA() {
        stateId = 0;
    }

    private static int nextStateId() {
        return stateId++;
    }

    private void addTransition(int sourceState, int symbol, int targetState) {
        if (!transitions.containsKey(sourceState)) {
            transitions.put(sourceState, new HashMap<>());
        }
        transitions.get(sourceState).put(symbol, targetState);
    }

    // Fonction pour calculer la fermeture epsilon d'un ensemble d'états
    private Set<Integer> epsilonClosure(Set<Integer> states, ArrayList<Integer>[] epsilonTransitions) {
        Set<Integer> closure = new HashSet<>(states);
        Stack<Integer> stack = new Stack<>();
    
        // Ajouter tous les états initiaux à la pile
        for (Integer state : states) {
            stack.push(state);
        }
    
        while (!stack.isEmpty()) {
            int currentState = stack.pop();
    
            // Parcourir toutes les transitions epsilon pour l'état courant
            for (Integer nextState : epsilonTransitions[currentState]) {
                // Si l'état suivant n'est pas encore dans la fermeture, l'ajouter
                if (!closure.contains(nextState)) {
                    closure.add(nextState);
                    stack.push(nextState);
                }
            }
        }
    
        return closure;
    }

    // Fonction pour déterminiser le NDFA
    public DFA determinize(NDFA ndfa) {
        this.symbols = ndfa.symbols;
        this.transitions = new HashMap<>();

        Map<Set<Integer>, Integer> states = new HashMap<>(); // Ensemble pour stocker les états du DFA et leur futur ID
        Queue<Set<Integer>> queue = new LinkedList<>();

        // Commencer par la fermeture epsilon de l'état de départ du NDFA
        Set<Integer> startSet = epsilonClosure(new HashSet<>(Collections.singletonList(ndfa.startState)), ndfa.epsilonTransitions);
        states.put(startSet, nextStateId()); 
        this.startState = states.get(startSet); // L'etat initial du DFA est la fermeture epsilon de l'état initial du NDFA
        queue.add(startSet);

        while (!queue.isEmpty()) {
            Set<Integer> currentSet = queue.poll();
            int currentState = states.get(currentSet);

            // Parcourir tous les symboles de l'alphabet
            for (int symbol : symbols) {
                Set<Integer> nextSet = new HashSet<>();
                for (Integer state : currentSet) {
                    // Ajouter les états atteignables par le symbole
                    if (ndfa.transitions[state][symbol] != -1) {
                        nextSet.addAll(epsilonClosure(new HashSet<>(Collections.singletonList(ndfa.transitions[state][symbol])), ndfa.epsilonTransitions));
                    }
                }

                if (!nextSet.isEmpty()) {
                    // Si l'état n'existe pas encore, l'ajouter à la file
                    if (!states.containsKey(nextSet)) {
                        states.put(nextSet, nextStateId());
                        queue.add(nextSet);
                    }
                    addTransition(currentState, symbol, states.get(nextSet));
                }
            }
        }

        // Déterminer les états finaux du DFA
        finalStates = new ArrayList<>();
        for (Map.Entry<Set<Integer>, Integer> entry : states.entrySet()) {
            Set<Integer> stateSet = entry.getKey();
            // Si l'état final du NDFA est dans l'ensemble d'états du DFA, alors c'est un état final
            if (stateSet.contains(ndfa.finalState)) {
                finalStates.add(entry.getValue());
            }
        }

        return this;
    }

    // Fonction pour minimiser le DFA
    public DFA minimize() {
        // Partition initiale des états finaux et non finaux
        Set<Integer> finalStateSet = new HashSet<>(finalStates);
        Set<Integer> nonFinalStateSet = new HashSet<>(transitions.keySet());
        nonFinalStateSet.removeAll(finalStateSet);

        List<Set<Integer>> partitions = new ArrayList<>();
        partitions.add(finalStateSet);
        partitions.add(nonFinalStateSet);

        // Raffiner les partitions jusqu'à stabilisation
        boolean refined;
        do {
            refined = false;
            List<Set<Integer>> newPartitions = new ArrayList<>();

            for (Set<Integer> partition : partitions) {
                Map<Map<Integer, Integer>, Set<Integer>> transitionMapToStates = new HashMap<>(); // Map des transitions vers les nouvelles partitions : ((symbole, partition), nouvelle partition)

                for (Integer state : partition) {
                    Map<Integer, Integer> stateTransitions = new HashMap<>(); // Map des transitions de l'état vers les indices des partitions : (symbole, index de partition)
                    for (int symbol : symbols) {
                        if (!transitions.containsKey(state)) {
                            continue;
                        }
                        int targetState = transitions.get(state).getOrDefault(symbol, -1);
                        int partitionIndex = findPartition(targetState, partitions);
                        stateTransitions.put(symbol, partitionIndex);
                    }

                    transitionMapToStates.computeIfAbsent(stateTransitions, k -> new HashSet<>()).add(state);
                }

                // Ajouter les partitions raffinées
                newPartitions.addAll(transitionMapToStates.values());
                if (transitionMapToStates.size() > 1) {
                    refined = true;
                }
            }

            partitions = newPartitions;
        } while (refined);
    
        // Créer le nouveau DFA minimal
        DFA minimizedDFA = new DFA();
        minimizedDFA.symbols = this.symbols;
        minimizedDFA.transitions = new HashMap<>();
        minimizedDFA.finalStates = new ArrayList<>();
        Map<Set<Integer>, Integer> partitionToStateMap = new HashMap<>();

        for (Set<Integer> partition : partitions) {
            int newState = nextStateId();
            partitionToStateMap.put(partition, newState);

            // Si l'état initial est dans la partition
            if (partition.contains(startState)) {
                minimizedDFA.startState = newState;
            }

            // Si un état final est dans la partition
            for (Integer finalState : finalStates) {
                if (partition.contains(finalState)) {
                    minimizedDFA.finalStates.add(newState);
                    break;
                }
            }
        }
    
        // Ajouter les transitions du DFA minimal
        for (Set<Integer> partition : partitions) {
            int sourceState = partitionToStateMap.get(partition);
            Integer representativeState = partition.iterator().next();

            for (int symbol : symbols) {
                if (!transitions.containsKey(representativeState)) {
                    continue;
                }
                int targetState = transitions.get(representativeState).getOrDefault(symbol, -1);
                if (targetState != -1) {
                    int targetPartitionState = findPartition(targetState, partitions);
                    minimizedDFA.addTransition(sourceState, symbol, partitionToStateMap.get(partitions.get(targetPartitionState)));
                }
            }
        }

        return minimizedDFA;
    }

    // Trouver la partition contenant un état donné
    private int findPartition(int state, List<Set<Integer>> partitions) {
        for (int i = 0; i < partitions.size(); i++) {
            if (partitions.get(i).contains(state)) {
                return i;
            }
        }
        return -1; 
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Start State : ").append(startState).append("\n");
        sb.append("Final States : ");
        for (int finalState : finalStates) {
            sb.append(finalState).append(" ");
        }
        sb.append("\n");

        for (Map.Entry<Integer, Map<Integer, Integer>> entry : transitions.entrySet()) {
            int sourceState = entry.getKey();
            Map<Integer, Integer> transition = entry.getValue();
            for (Map.Entry<Integer, Integer> targetEntry : transition.entrySet()) {
                int symbol = targetEntry.getKey();
                int targetState = targetEntry.getValue();
                if (symbol == NDFA.DOT_INDEX) {
                    sb.append("State ").append(sourceState).append(" - . -> State ").append(targetState).append("\n");
                } else {
                    sb.append("State ").append(sourceState).append(" - ").append((char) symbol).append(" -> State ").append(targetState).append("\n");
                }
            }
        }

        return sb.toString();
    }

    // Fonction pour parcourir un texte et vérifier s'il est accepté par l'automate
    public boolean parcoursTexte(String texte){
        int etat = startState;
        for (int i = 0; i < texte.length(); i++){
            if (finalStates.contains(etat)){
                return true;
            }
            char c = texte.charAt(i);
            if (transitions.get(etat).containsKey((int) c)){
                etat = transitions.get(etat).get((int) c);
            }
            else if (transitions.get(etat).containsKey(NDFA.DOT_INDEX)){
                etat = transitions.get(etat).get(NDFA.DOT_INDEX);
            } else {
                etat = startState;
                continue;
            }
        }
        return false;
    }

    // Fonction pour lire un fichier ligne par ligne et chercher un motif avec l'automate
    public static int searchInFile(File file, String pattern) throws Exception {
        RegEx.setRegEx(pattern);
        RegExTree tree = RegEx.parse();
        NDFA ndfa = new NDFA().treeToNDFA(tree);
        DFA dfa = new DFA().determinize(ndfa);
        DFA minimizedDFA = dfa.minimize();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int nbLignes = 0;

            while ((line = br.readLine()) != null) {
                minimizedDFA.parcoursTexte(line);
                /*if (minimizedDFA.parcoursTexte(line)){
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

    public static void main(String arg[]) throws Exception {

        // Motif à chercher pour les tests sur le temps d'exécution
        //String pattern = ".";
        String pattern = "(a|j|k|p|z|b)*e*o(f|d|g|h|p)";

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
        String csvFileTime = "./automaton/automaton_time_results.csv";
        String csvFileMemory = "./automaton/automaton_memory_results.csv";
    	
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
                    nbLignes = DFA.searchInFile(file, pattern);
                    
                    startTime = System.nanoTime();
                    for (int j=0; j<nbIterations; j++) {
                        DFA.searchInFile(file, pattern);
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
                    DFA.searchInFile(files[0], patterns[j]);
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
