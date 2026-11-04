package com.example.petcare.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.graphics.ColorUtils
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.example.petcare.R
import com.google.android.material.color.MaterialColors

/** Indicateur de session en lecture seule : aucune interaction tactile à simuler. */
class CareProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()
    private var nombre = 0
    private var objectif = 5

    fun setProgress(nombre: Int, objectif: Int = 5) {
        this.nombre = nombre.coerceAtLeast(0)
        this.objectif = objectif.coerceAtLeast(1)
        contentDescription = context.getString(R.string.care_progress_description, this.nombre, this.objectif)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val largeur = (240 * resources.displayMetrics.density).toInt()
        val hauteur = (140 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(resolveSize(largeur, widthMeasureSpec), resolveSize(hauteur, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // EXERCICE M2.2A — dessin de l'indicateur circulaire.

        // 1. Dimensions adaptées à l'écran : on convertit des dp en pixels avec la densité,
        //    sinon le trait serait minuscule sur un écran haute résolution.
        val densite = resources.displayMetrics.density
        val epaisseur = 10f * densite                                   // épaisseur de l'anneau
        val tailleTexte = TypedValue.applyDimension(                    // texte en sp (suit la
            TypedValue.COMPLEX_UNIT_SP, 22f, resources.displayMetrics  // préférence de police)
        )

        // 2. Zone utile = la View moins les paddings (réservés par le layout).
        val largeurUtile = width - paddingLeft - paddingRight
        val hauteurUtile = height - paddingTop - paddingBottom
        val cx = paddingLeft + largeurUtile / 2f                        // centre du cercle
        val cy = paddingTop + hauteurUtile / 2f

        // 3. Rayon : la PLUS PETITE dimension (ici la hauteur, 140dp < 240dp) pour que le
        //    cercle tienne entièrement, moins la moitié de l'épaisseur du trait, car un trait
        //    est dessiné de part et d'autre de son tracé.
        val rayon = minOf(largeurUtile, hauteurUtile) / 2f - epaisseur / 2f
        if (rayon <= 0f) return                                         // View trop petite : rien à dessiner

        // 4. Rectangle englobant du cercle (champ fourni) : centre ± rayon.
        bounds.set(cx - rayon, cy - rayon, cx + rayon, cy + rayon)

        // 5. Fraction de progression en FLOTTANTS (3 / 5 en entiers donnerait 0 !),
        //    bornée entre 0 et 1 : 6 soins pour un objectif de 5 → arc plein, pas plus d'un tour.
        val fraction = (nombre.toFloat() / objectif).coerceIn(0f, 1f)

        // 6. Couleurs du thème (jour/nuit géré automatiquement par Material) :
        //    - colorPrimary pour l'arc de progression,
        //    - la même couleur très atténuée (alpha 60/255) pour la piste de fond,
        //    - colorOnSurface pour le texte, lisible sur le fond de l'écran.
        val couleurPrimaire = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
        val couleurPiste = ColorUtils.setAlphaComponent(couleurPrimaire, 60)
        val couleurTexte = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)

        // 7. Piste : un anneau complet (360°) dessiné en CONTOUR (STROKE), useCenter=false
        //    pour avoir un anneau et non une part de camembert.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = epaisseur
        paint.strokeCap = Paint.Cap.ROUND                               // extrémités arrondies
        paint.color = couleurPiste
        canvas.drawArc(bounds, 0f, 360f, false, paint)

        // 8. Arc de progression PAR-DESSUS la piste : départ à -90° (en haut du cercle, car
        //    0° est à droite et les angles tournent dans le sens horaire), balayage = fraction × 360°.
        if (fraction > 0f) {
            paint.color = couleurPrimaire
            canvas.drawArc(bounds, -90f, fraction * 360f, false, paint)
        }

        // 9. Texte "nombre / objectif" centré. On repasse en FILL (le Paint garde son état STROKE
        //    sinon le texte serait dessiné en contour). Le vrai compteur est affiché même au-delà
        //    de l'objectif (6 / 5). drawText prend la LIGNE DE BASE en y, pas le centre : on la
        //    recalcule avec les métriques de police pour centrer visuellement.
        paint.style = Paint.Style.FILL
        paint.color = couleurTexte
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = tailleTexte
        val metriques = paint.fontMetrics
        val ligneDeBase = cy - (metriques.ascent + metriques.descent) / 2f
        canvas.drawText(context.getString(R.string.progress_value, nombre, objectif), cx, ligneDeBase, paint)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        // EXERCICE M2.2D — sémantique accessible.
        // 1. On garde d'abord le comportement de base (dont la contentDescription posée par setProgress).
        super.onInitializeAccessibilityNodeInfo(info)

        // 2. wrap() donne une vue "compat" du nœud, utilisable sur toutes les versions d'Android.
        val noeud = AccessibilityNodeInfoCompat.wrap(info)

        // 3. Rôle : TalkBack annoncera "barre de progression" comme pour une ProgressBar standard.
        noeud.className = android.widget.ProgressBar::class.java.name

        // 4. Plage numérique entière : min 0, max objectif, valeur courante bornée à l'objectif
        //    (le texte, lui, garde le compteur réel). Pas d'action de clic ni de réglage :
        //    l'indicateur est en lecture seule.
        noeud.rangeInfo = AccessibilityNodeInfoCompat.RangeInfoCompat.obtain(
            AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_INT,
            0f,
            objectif.toFloat(),
            nombre.coerceAtMost(objectif).toFloat()
        )
    }
}
