package com.example.petcare

import com.example.petcare.model.Animal
import com.example.petcare.model.CompteurSoins
import com.example.petcare.work.PetCareWorker
import android.app.Notification
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    // EXERCICE 1 : données de démonstration, pas de saisie ni de persistance en M1.
    private val animaux = listOf(
        Animal(1L, "Nala", "cat"),
        Animal(2L, "Oslo", "dog")
    )
    private var indexAnimal = 0
    private var animalDuRappel: Animal? = null
    private val animal: Animal get() = animaux[indexAnimal]

    // Démonstration M1 : compteurs en mémoire, sans persistance.
    private val compteurs = CompteurSoins()
    private lateinit var root: View
    private lateinit var animalText: TextView
    private lateinit var countText: TextView
    private lateinit var workText: TextView

    private val permissionNotification = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { accordee ->
        if (accordee) afficherNotification() else {
            Toast.makeText(this, R.string.notification_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        root = findViewById(R.id.root)
        animalText = findViewById(R.id.animalText)
        countText = findViewById(R.id.countText)
        workText = findViewById(R.id.workText)

        // Conserve les marges du XML et évite les barres système.
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)

        findViewById<Button>(R.id.nextButton).setOnClickListener {
            indexAnimal = (indexAnimal + 1) % animaux.size
            afficherAnimal()
        }
        findViewById<Button>(R.id.careButton).setOnClickListener { confirmerAvecToast() }
        findViewById<Button>(R.id.snackButton).setOnClickListener { confirmerAvecAnnulation() }
        findViewById<Button>(R.id.notificationButton).setOnClickListener { demanderNotification() }
        findViewById<Button>(R.id.workButton).setOnClickListener { programmerVerification() }
        creerCanal()
        afficherAnimal()

        WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(WORK_NAME)
            .observe(this) { travaux ->
                workText.text = if (travaux.isNullOrEmpty()) {
                    getString(R.string.work_none)
                } else {
                    travaux.joinToString("\n") { info ->
                        val nom = info.outputData.getString(PetCareWorker.ANIMAL_NAME) ?: "—"
                        getString(R.string.work_state, info.id.toString().take(8), info.state.name, nom)
                    }
                }
            }
    }

    private fun afficherAnimal() {
        val espece = getString(when (animal.especeCode) {
            "cat" -> R.string.species_cat
            "dog" -> R.string.species_dog
            else -> R.string.species_unknown
        })
        animalText.text = getString(R.string.animal_summary, animal.nom, espece)
        val count = compteurs.nombre(animal.id)
        countText.text = resources.getQuantityString(R.plurals.care_count, count, count)
    }

    private fun confirmerAvecToast() {
        // EXERCICE 2A
        // 1. On enregistre un soin pour l'animal actuellement sélectionné (+1 dans le compteur).
        compteurs.confirmer(animal.id)

        // 2. On rafraîchit l'écran pour que le nouveau nombre de soins s'affiche tout de suite.
        afficherAnimal()

        // 3. On prépare le texte : la ressource care_done contient "%1$s", getString
        //    remplace ce "%1$s" par le nom de l'animal.
        val message = getString(R.string.care_done, animal.nom)

        // 4. On crée le Toast (durée courte) ET on l'affiche avec .show().
        //    Sans .show(), le Toast est créé en mémoire mais jamais visible à l'écran.
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun confirmerAvecAnnulation() {
        // EXERCICE 2B
        // 1. On CAPTURE l'animal dans une variable locale (val = immuable).
        //    Important : l'utilisateur peut appuyer sur "Changer d'animal" pendant que le
        //    Snackbar est encore visible. Si on utilisait "animal" directement dans l'action
        //    Annuler, on annulerait le soin du MAUVAIS animal. La variable capturée fige
        //    l'animal concerné au moment du clic.
        val animalConcerne = animal

        // 2. On enregistre le soin pour cet animal précis, puis on rafraîchit l'affichage.
        compteurs.confirmer(animalConcerne.id)
        afficherAnimal()

        // 3. On construit le message avec le nom de l'animal capturé.
        val message = getString(R.string.care_done, animalConcerne.nom)

        // 4. On crée le Snackbar attaché à la vue racine "root" (le Snackbar a besoin d'une
        //    vue pour savoir où s'afficher), avec une durée longue pour laisser le temps d'annuler.
        Snackbar.make(root, message, Snackbar.LENGTH_LONG)
            // 5. On ajoute l'action "Annuler" (texte de la ressource undo).
            //    Le bloc { } est exécuté UNIQUEMENT si l'utilisateur appuie sur Annuler.
            .setAction(R.string.undo) {
                // 6. On retire UN soin à l'animal capturé (pas à l'animal sélectionné actuel).
                compteurs.annuler(animalConcerne.id)
                // 7. On rafraîchit pour voir le compteur redescendre.
                afficherAnimal()
            }
            // 8. Comme pour le Toast : sans .show(), rien n'apparaît.
            .show()
    }

    private fun creerCanal() {
        // Kit prévu pour minSdk 26 ou supérieur.
        val canal = NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT)
        canal.description = getString(R.string.channel_description)
        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun demanderNotification() {
        animalDuRappel = animal
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            permissionNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            afficherNotification()
        }
    }

    private fun afficherNotification() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) return

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            Toast.makeText(this, R.string.notifications_disabled, Toast.LENGTH_LONG).show()
            return
        }
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) {
            Toast.makeText(this, R.string.notifications_disabled, Toast.LENGTH_LONG).show()
            return
        }
        val notification = construireRappel(animalDuRappel ?: animal)
        if (notification != null) NotificationManagerCompat.from(this).notify(1001, notification)
    }

    // EXERCICE 2 : contenu du rappel. Canal, permission et PendingIntent sont fournis.
    private fun construireRappel(animalConcerne: Animal): Notification? {
        // EXERCICE 2C
        // NotificationCompat.Builder construit la notification étape par étape.
        // On lui donne le contexte (this) et l'identifiant du canal créé dans creerCanal().
        // Sur Android 8+, une notification sans canal valide n'est jamais affichée.
        return NotificationCompat.Builder(this, CHANNEL_ID)
            // 1. Petite icône : OBLIGATOIRE, sinon la notification est refusée par le système.
            //    C'est l'icône monochrome affichée dans la barre d'état.
            .setSmallIcon(R.drawable.ic_pet_notification)
            // 2. Titre de la notification (ligne du haut).
            .setContentTitle(getString(R.string.reminder_title))
            // 3. Texte de la notification : reminder_text contient "%1$s", remplacé par le nom
            //    de l'animal passé en paramètre (animalConcerne, pas "animal", car la sélection
            //    peut avoir changé entre le clic et l'affichage, par ex. après la demande de permission).
            .setContentText(getString(R.string.reminder_text, animalConcerne.nom))
            // 4. Intention déclenchée quand l'utilisateur touche la notification :
            //    intentionOuvrirApp() renvoie un PendingIntent qui ouvre MainActivity.
            .setContentIntent(intentionOuvrirApp())
            // 5. Suppression automatique : la notification disparaît dès qu'on la touche.
            .setAutoCancel(true)
            // 6. Priorité : nécessaire pour les anciennes versions d'Android (avant les canaux),
            //    cohérente avec IMPORTANCE_DEFAULT du canal.
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // 7. build() assemble tout et renvoie l'objet Notification final.
            //    L'affichage lui-même est fait par afficherNotification() avec l'id fixe 1001 :
            //    comme l'id ne change pas, un nouveau rappel REMPLACE le précédent.
            .build()
    }

    private fun intentionOuvrirApp(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun programmerVerification() {
        // EXERCICE 3B
        // 1. On prépare les données d'entrée du Worker. workDataOf crée un objet "Data"
        //    (un petit dictionnaire clé → valeur) que WorkManager transmettra au Worker.
        //    Les clés sont les constantes de PetCareWorker pour éviter les fautes de frappe.
        val entrees = workDataOf(
            PetCareWorker.ANIMAL_ID to animal.id,
            PetCareWorker.ANIMAL_NAME to animal.nom
        )

        // 2. On construit une demande de travail PONCTUELLE (OneTime = exécutée une seule fois)
        //    qui lancera PetCareWorker. Le tag "petcare-demo" sert à retrouver/annuler
        //    ce type de travail plus tard si besoin.
        val demande = OneTimeWorkRequestBuilder<PetCareWorker>()
            .setInputData(entrees)
            .addTag("petcare-demo")
            .build()

        // 3. On envoie la demande à WorkManager sous un nom UNIQUE (WORK_NAME).
        //    ExistingWorkPolicy.KEEP : si un travail du même nom est déjà en cours ou
        //    en attente, la nouvelle demande est ignorée (pas de doublon). C'est ce qui
        //    explique qu'un double clic rapide ne lance qu'une seule vérification.
        WorkManager.getInstance(this).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.KEEP,
            demande
        )

        // 4. On informe l'utilisateur. Le résultat réel arrivera dans workText grâce au
        //    LiveData observé dans onCreate (ENQUEUED → RUNNING → SUCCEEDED).
        Toast.makeText(this, R.string.work_requested, Toast.LENGTH_SHORT).show()
    }

    private fun exerciceACompleter(numero: Int) {
        Toast.makeText(this, getString(R.string.exercise_pending, numero), Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val CHANNEL_ID = "care_reminders"
        // Une file globale : KEEP ignore une demande si la précédente est inachevée.
        private const val WORK_NAME = "petcare-check"
    }
}
