package com.jjas.labpomodoro.service

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.jjas.labpomodoro.MainActivity
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.PlanTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accesos directos del ícono (mantenerlo presionado): iniciar el plan de siempre, las primeras
 * plantillas y abrir Tu progreso. Se actualizan cuando cambian las plantillas.
 */
@Singleton
class ShortcutPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            settings.settings
                .map { it.planTemplates ?: suggested(context) }
                .distinctUntilChanged()
                .collect { publish(it) }
        }
    }

    private fun publish(templates: List<PlanTemplate>) {
        val shortcuts = buildList {
            add(shortcut("start", context.getString(R.string.shortcut_start), R.drawable.ic_shortcut_play, ACTION_START))
            templates.take(MAX_TEMPLATES).forEachIndexed { i, t ->
                add(
                    shortcut("template_$i", t.name, R.drawable.ic_shortcut_flask, ACTION_START) {
                        putExtra(EXTRA_TEMPLATE, i)
                    }
                )
            }
            add(shortcut("progress", context.getString(R.string.shortcut_progress), R.drawable.ic_shortcut_chart, ACTION_PROGRESS))
        }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }

    private fun shortcut(id: String, label: String, icon: Int, action: String, extras: Intent.() -> Unit = {}) =
        ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(label)
            .setIcon(IconCompat.createWithResource(context, icon))
            .setIntent(Intent(context, MainActivity::class.java).setAction(action).apply(extras))
            .build()

    companion object {
        const val ACTION_START = "com.jjas.labpomodoro.action.SHORTCUT_START"
        const val ACTION_PROGRESS = "com.jjas.labpomodoro.action.SHORTCUT_PROGRESS"
        const val EXTRA_TEMPLATE = "template"

        /** Android muestra unos cuatro: inicio, dos plantillas y progreso. */
        private const val MAX_TEMPLATES = 2

        /** Plantillas sugeridas, en el idioma de la app. */
        fun suggested(context: Context): List<PlanTemplate> = PlanTemplate.suggested(
            context.getString(R.string.template_quick),
            context.getString(R.string.template_study),
            context.getString(R.string.template_deep),
        )
    }
}
