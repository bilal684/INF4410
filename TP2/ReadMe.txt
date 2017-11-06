Mode sécurisé

1) Compiler le projet en allant dans la racine (là ou se trouve le dossier src ainsi que les fichiers d'opérations). Ouvrir un terminal et écrire "ant" pour compiler.
2) Dans le dossier "bin", lancer la commande "rmiregistry <port>&". Un valeur possible du port est, par exemple, 5000.
3) Ouvrir le fichier nommé "config" et configurer le mode securise a "true" ainsi que les adresses IP et ports des différents serveurs. Vous pouvez utiliser le symbole "%" pour commenter une ligne.
4) Revenir à la racine du projet et lancer un serveur. Ceci peut être fait avec la commande suivante : "./server <capacity> <rmiregistry port>". Par exemple, "./server 3 5000".
5) Après le lancement des différents serveurs, vous pouvez lancer le répartiteur avec la commande suivante : "./dispatcher <filename>". Par exemple, "./dispatcher operations-2700".

Mode non-sécurisé

Effectuer les étapes 1 et 2 décrite pour le mode sécurisé.
3) Ouvrir le fichier nommé "config" et configurer le mode sécurisé à "false" ainsi que les adresses IP et ports des différents serveurs. Vous pouvez utiliser le symbole "%" pour commenter une ligne.
4) Revenir à la racine du projet et lancer un serveur. Cette fois, utilisé la commande suivante : "./server <capacity> <rmiregistry port> <lie rate>". La valeur lie rate doit être dans l'intervalle [0, 100]. 
*Dans le cas où on voudrait que le serveur ne mentent jamais, il suffit de lui donné la valeur "-1". Un exemple de commande pour un serveur avec 50% de chance de mentir est : "./server 5 5000 50" 
5) Après le lancement des différents serveurs, vous pouvez lancer le répartiteur avec la commande suivante : "./dispatcher <filename>". Par exemple, "./dispatcher operations-2700".