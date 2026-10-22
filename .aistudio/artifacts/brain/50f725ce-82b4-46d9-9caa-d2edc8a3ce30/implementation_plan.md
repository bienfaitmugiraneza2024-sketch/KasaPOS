# KasaPOS ERP Pro : Commerce Multi-Magasins, Clôture de Caisse & Traçabilité Complète

Plan d'implémentation consolidé pour **KasaPOS**, combinant les flux multi-succursales, les codes d'invitation d'employés, la clôture journalière de caisse, les transferts, les reçus WhatsApp et la traçabilité des dettes/provisions.

***

### 1. Direction Artistique & Maquettes Visuelles

#### Palette de Couleurs & Identité de Marque
- **Thème Signature "Emerald & Royal Cobalt" :**
  - `Primary` : Vert Émeraude Profond (`#006D5B` / `#004D40`) symbolisant la croissance commerciale et les profits.
  - `Secondary` : Bleu Cobalt (`#006495` / `#0288D1`) pour les outils de caisse, transferts et opérations magasins.
  - `Tertiary / Warning` : Ambre Doré (`#FFA000`) pour les alertes de stock critique et acomptes en attente.
  - `Error / Alerte` : Rouge Corail (`#BA1A1A`) pour les ruptures de stock, pertes et dépenses.
  - `Surfaces` : Cartes modernes à forte lisibilité en plein jour, élévation tonale M3 et coins arrondis à 16.dp.

#### Maquettes de l'Interface Utilisateur
- **Tableau de Bord Consolidé & Sélecteur de Succursale :**
  - En-tête supérieur avec sélecteur de magasin (*Toutes les succursales*, *Boutique Centre-Ville*, *Dépôt Marché*) et commutateur de devises instantané (*USD $* / *FC*).
  - Cartes KPIs à contraste élevé : Bénéfice net réel en direct, chiffre d'affaires cumulé, alertes stock urgentes.
- **Caisse POS & Reçu WhatsApp Partageable :**
  - Modal d'encaissement avec calcul automatique de la monnaie à rendre en FC et en USD.
  - Fiche de commande / dette avec statut (*PAYÉ*, *AVANCE*, *RESTE*).
  - Bouton vert pro `[Partager Reçu WhatsApp]` générant le ticket formaté en 1 clic.

***

### 2. Architecture des Rôles & Sécurité

#### A. Rôle Administrateur (Gérant / Propriétaire)
- Vue consolidée globale de toutes les succursales ou filtrage magasin par magasin.
- Accès aux coûts de revient, marges brutes, bénéfice net réel et rapports financiers.
- Générateur de codes d'invitation d'employés (`KASA-XXXX`) avec affectation de magasin.
- Validation des transferts de stock et consultation des clôtures de caisse du soir.

#### B. Rôle Employé (Caissier / Vendeur)
- Activation par code d'invitation unique et configuration du mot de passe / code PIN personnel.
- Opérations restreintes à son propre magasin : Caisse, Stock local (prix de vente uniquement, marges masquées), Provisions et Réquisitions (+1).
- Clôture de caisse en fin de journée avec comptage physique des billets (FC et USD) et rapport automatique des écarts.

***

### 3. Fonctionnalités Clés Intégrées

1. **Multi-Magasins & Stocks Séparés :** Gestion isolée par boutique avec transferts inter-succursales atomiques.
2. **Invitations d'Employés par Code Unique :** Code d'accès généré côté admin ouvrant le formulaire d'inscription en ligne.
3. **Clôture de Caisse Quotidienne :** Rapprochement fin de journée avec détection des écarts de caisse (FC et USD).
4. **Transferts & Pertes :** Bons de transfert entre magasins et déclaration de casse/avaries.
5. **Reçus WhatsApp Automatiques :** Ticket de vente et reçu d'acompte digital prêt à envoyer au client.
6. **Dettes Détaillées par Article :** Client, produit, quantité, montant total, acompte versé, solde restant, échéance et statuts `PAYÉ` / `AVANCE` / `RESTE`.
7. **Provisions & Réquisitions :** Commandes payées d'avance avec conversion en vente en 1 clic et besoins clients non satisfaits avec compteur `+1`.
8. **Double Devise FC / USD :** Taux de change réglable à tout moment avec conversion instantanée.

***

### 4. Plan de Réalisation Technique

1. **Règles Firestore & Sécurité :**
   - Mise à jour des collections sous `/users/{adminUid}/...` pour `stores`, `invite_codes`, `staff`, `cash_closings`, `stock_transfers`, `stock_losses`, `debts`, `provisions`, `requisitions`.
   - Déploiement des règles via `rpc_action fax.DeployRules`.
2. **Logique Métier & Repository :**
   - Méthodes multi-magasins, transferts de stock atomiques, générateur de codes d'invitation et clôture de caisse.
3. **Composants d'Interface Jetpack Compose :**
   - Navigation adaptative (Mobile & Tablette), sélecteur de succursale, formulaire d'invitation employé, modal WhatsApp, clôture de caisse et registre des dettes.
4. **Vérification & Compilation :**
   - Compilation complète via `compile_applet`.
