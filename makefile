# Variables
SRC_AUTOMATON = automaton/DFA.java automaton/NDFA.java automaton/RegEx.java
SRC_KMP = kmp/KMP.java
SRC_EGREP = egrep/Egrep.java

# Compile les trois fichiers avec Main
all: compile

compile:
	@echo "Compilation des fichiers Java"
	javac $(SRC_AUTOMATON) $(SRC_EGREP) $(SRC_KMP)

# Exécuter DFA
run-dfa:
	@echo "Exécution des tests de performance de DFA..."
	java automaton.DFA

# Exécuter KMP
run-kmp:
	@echo "Exécution des tests de performance de KMP..."
	java kmp.KMP

# Exécuter Egrep
run-egrep:
	@echo "Exécution des tests de performance de Egrep..."
	java egrep.Egrep

# Nettoyer les fichiers compilés
clean:
	@echo "Nettoyage des fichiers compilés"
	rm -f automaton/*.class egrep/*.class kmp/*.class
