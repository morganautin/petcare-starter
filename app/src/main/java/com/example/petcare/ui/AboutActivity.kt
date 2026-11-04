package com.example.petcare.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.example.petcare.R

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        afficherPresentation()
        appliquerInsets(findViewById(R.id.root))
        findViewById<Button>(R.id.closeButton).setOnClickListener { finish() }
    }

    private fun afficherPresentation() {
        // EXERCICE M2.1A
        // setContentView "gonfle" (inflate) le fichier XML et l'installe comme écran de l'activité.
        // On charge activity_about : il contient déjà root, pageTitle, le texte de présentation
        // dans un ScrollView et closeButton. Les findViewById qui suivent dans onCreate
        // trouveront donc bien ces IDs.
        setContentView(R.layout.activity_about)
    }
}
