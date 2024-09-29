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

class DFA {
    private static int stateId = 0; // Un ID d'état unique pour chaque nouvel état
    int startState;
    List<Integer> finalStates;

    Map<Integer, Map<Integer, Integer>> transitions; // Map des transitions pour chaque état : (source, (symbol, target))
    List<Integer> symbols;

    private static int nextStateId() {
        return stateId++;
    }

    private void addTransition(int sourceState, int symbol, int targetState) {
        if (!transitions.containsKey(sourceState)) {
            transitions.put(sourceState, new HashMap<>());
        }
        transitions.get(sourceState).put(symbol, targetState);
    }

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
                sb.append("State ").append(sourceState).append(" - ").append((char) symbol).append(" -> State ").append(targetState).append("\n");
            }
        }

        return sb.toString();
    }

    public static void main(String arg[]) throws Exception {
    	RegEx.setRegEx("b|a|c*");
    	RegExTree tree = RegEx.parse();
    	
        NDFA ndfa = new NDFA();
        ndfa.treeToNDFA(tree);
        System.out.println("NDFA :\n" + ndfa);

        DFA dfa = new DFA().determinize(ndfa);
        System.out.println("DFA :\n" + dfa);

        DFA minimizedDFA = dfa.minimize();
        System.out.println("Minimized DFA :\n" + minimizedDFA);
    }
   
}
