# Clone de egrep avec support partiel des ERE

## Description
Ce projet contient trois modules :
- **Automaton** : Implémentation des automates DFA et NDFA, ainsi qu'une interprétation des expressions régulières.
- **KMP** : Implémentation de l'algorithme de Knuth-Morris-Pratt (KMP).
- **Egrep** : Clone simplifié de la commande `egrep`.

## Exécution

### Compilation
Pour compiler les fichiers Java, utilisez la commande :
```bash
make
```

### Exécution des tests de performance
Après compilation, vous pouvez exécuter chaque module. Lors de l'exécution, les programmes lancent des tests de performance et créent des fichiers .csv pour enregistrer les résultats.

- Pour exécuter les tests de performance de DFA :
```bash
make run-dfa
```
- Pour exécuter les tests de performance de KMP :
```bash
make run-kmp
```
- Pour exécuter les tests de performance de `egrep` :
```bash
make run-egrep
```

### Nettoyage
Pour supprimer les fichiers compilés :
```bash
make clean
```

## Tracer les graphes
Un fichier `graph.ipynb` est fourni dans le projet pour tracer les graphes de performance basés sur les données des fichiers .csv générés. Vous pouvez l'ouvrir avec Jupyter Notebook et l'exécuter pour visualiser les résultats.