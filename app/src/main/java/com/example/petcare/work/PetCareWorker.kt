package com.example.petcare.work

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

/** Simulation locale, sans réseau, heure exacte ni notification différée. */
class PetCareWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        // EXERCICE 3C
        // 1. On lit les entrées transmises par MainActivity via workDataOf.
        //    getLong(clé, valeurParDéfaut) : si la clé est absente, on reçoit la valeur par
        //    défaut (ici -1, volontairement invalide pour détecter l'absence).
        //    getString(clé) renvoie null si la clé est absente.
        val id = inputData.getLong(ANIMAL_ID, -1L)
        val nom = inputData.getString(ANIMAL_NAME)

        // 2. Validation : un ID doit être strictement positif, et un nom ne doit être
        //    ni null ni vide/espaces. Sinon on signale l'échec à WorkManager.
        //    (isNullOrBlank gère les deux cas null et "   " en une seule vérification.)
        if (id <= 0L || nom.isNullOrBlank()) {
            Log.w(TAG, "Données invalides : id=$id, nom=$nom")
            return Result.failure()
        }

        // 3. Simulation de la vérification : on écrit juste une trace dans Logcat.
        //    Log.i = niveau "info". Le tag "PetCareWork" permet de filtrer dans Logcat.
        Log.i(TAG, "Vérification simulée pour l'animal #$id ($nom)")

        // 4. Succès : on renvoie les mêmes données en sortie (outputData).
        //    C'est ce que MainActivity lit dans info.outputData pour afficher le nom
        //    dans workText à côté de l'état SUCCEEDED.
        return Result.success(
            workDataOf(
                ANIMAL_ID to id,
                ANIMAL_NAME to nom
            )
        )
    }

    companion object {
        // Tag utilisé pour filtrer les traces dans Logcat (demandé par l'énoncé).
        private const val TAG = "PetCareWork"
        const val ANIMAL_ID = "animal_id"
        const val ANIMAL_NAME = "animal_name"
    }
}
