import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class NDFA {
    static final int DOT_INDEX = 256;

    private static int counterState = 0; // Un ID d'état unique pour chaque nouvel état
    int startState;
    int finalState;

    int[][] transitions; // Tableau de transitions pour chaque état
    ArrayList<Integer>[] epsilonTransitions; // Tableau de transitions epsilon pour chaque état
    List<Integer> symbols = new ArrayList<>();

    public NDFA() {
        counterState = 0;
    }
    
    private static int nextStateId() {
        return counterState++;
    }

    private List<Integer> getSymbols() {
        return symbols;
    }

    // Calcul du nombre d'états de l'automate
    private int calculNbStates(RegExTree tree) {
        if (tree.root == RegEx.CONCAT) { // La concaténation ne rajoute pas d'état
            return calculNbStates(tree.subTrees.get(0)) + calculNbStates(tree.subTrees.get(1));
        } else if (tree.root == RegEx.ALTERN) { // L'union rajoute 2 états
            return calculNbStates(tree.subTrees.get(0)) + calculNbStates(tree.subTrees.get(1)) + 2;
        } else if (tree.root == RegEx.ETOILE) { // La closure rajoute 2 états
            return calculNbStates(tree.subTrees.get(0)) + 2;
        } else {
            return 2; // Les lettres créent 2 états
        }
    }

    // Convertit un arbre de syntaxe en automate fini non déterministe
    @SuppressWarnings("unchecked")
    public NDFA treeToNDFA(RegExTree tree) {
        int nbStates = calculNbStates(tree);
        transitions = new int[nbStates][257]; // 256 symboles possibles pour ASCII étendu + 1 pour DOT
        for (int i = 0; i < nbStates; i++) {
            for (int j = 0; j < 257; j++) {
                transitions[i][j] = -1;
            }
        }
        epsilonTransitions = new ArrayList[nbStates];
        for (int i = 0; i < nbStates; i++) {
            epsilonTransitions[i] = new ArrayList<>();
        }

        buildTransitions(tree);
        return this;
    }

    public void buildTransitions(RegExTree tree) {
        // Concaténation
        if (tree.root == RegEx.CONCAT) {  
            buildTransitions(tree.subTrees.get(0));
            int oldStart1 = startState;
            int oldFinal1 = finalState;

            buildTransitions(tree.subTrees.get(1));
            int oldStart2 = startState;
            int oldFinal2 = finalState;

            epsilonTransitions[oldFinal1].add(oldStart2);
            this.startState = oldStart1;
            this.finalState = oldFinal2;
        } 
        // Alternation (union)
        else if (tree.root == RegEx.ALTERN) {
            buildTransitions(tree.subTrees.get(0));
            int oldStart1 = startState;
            int oldFinal1 = finalState;

            buildTransitions(tree.subTrees.get(1));
            int oldStart2 = startState;
            int oldFinal2 = finalState;

            this.startState = nextStateId();
            this.finalState = nextStateId();

            epsilonTransitions[startState].add(oldStart1);
            epsilonTransitions[startState].add(oldStart2);
            epsilonTransitions[oldFinal1].add(finalState);
            epsilonTransitions[oldFinal2].add(finalState);
        } 
        // Etoile (closure)
        else if (tree.root == RegEx.ETOILE) {
            buildTransitions(tree.subTrees.get(0));
            int oldStart = startState;
            int oldFinal = finalState;

            this.startState = nextStateId();
            this.finalState = nextStateId();

            epsilonTransitions[startState].add(oldStart);
            epsilonTransitions[startState].add(finalState);
            epsilonTransitions[oldFinal].add(oldStart);
            epsilonTransitions[oldFinal].add(finalState);
        } 
        else {
            this.startState = nextStateId();
            this.finalState = nextStateId();
            // Dot (n'importe quel caractère)
            if (tree.root == RegEx.DOT) {
                transitions[startState][DOT_INDEX] = finalState;
                symbols.add(DOT_INDEX);
            }
            // Symbole
            else {
                transitions[startState][tree.root] = finalState;
                symbols.add(tree.root);
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Start State : ").append(startState).append("\n");
        sb.append("Final State : ").append(finalState).append("\n");

        for (int i = 0; i < transitions.length; i++) {
            for (int symbol : symbols) {
                int targetState = transitions[i][symbol];
                if (targetState != -1) {
                    if (symbol == DOT_INDEX) {
                        sb.append("State ").append(i).append(" - . -> State ").append(targetState).append("\n");
                    } else {
                        sb.append("State ").append(i).append(" - ").append((char) symbol).append(" -> State ").append(targetState).append("\n");
                    }
                }
            }
            if (!epsilonTransitions[i].isEmpty()) {
                for (int targetState : epsilonTransitions[i]) {
                    sb.append("State ").append(i).append(" - ε -> State ").append(targetState).append("\n");
                }
            }
        }

        return sb.toString();
    }

    public static void main(String arg[]) {
        RegExTree tree = RegEx.exampleAhoUllman();
        NDFA ndfa = new NDFA().treeToNDFA(tree);
        System.out.println("NDFA :\n" + ndfa);
    }

}