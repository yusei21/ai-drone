package com.sloth.registerapp.features.report.data.manager

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sloth.registerapp.core.auth.LocalSessionManager
import com.sloth.registerapp.core.database.AppDatabase
import com.sloth.registerapp.core.database.FlightReportEntity
import com.sloth.registerapp.features.report.domain.model.FlightReport
import com.sloth.registerapp.features.report.domain.model.FlightReportSession
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.max

/**
 * Gerencia o ciclo de um relatório de voo em memória:
 * - cria sessão ao iniciar missão
 * - mantém cronômetro (elapsedMs)
 * - finaliza sessão gerando relatório completo
 *
 * Estrutura preparada para evolução: campo extraData aceita novos dados sem quebrar contrato.
 */
class FlightReportManager(
    context: Context? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private var timerJob: Job? = null
    private val gson = Gson()
    private val localSessionManager = context?.applicationContext
        ?.let { LocalSessionManager.getInstance(it) }
    private val flightReportDao = context?.applicationContext
        ?.let { AppDatabase.getInstance(it).flightReportDao() }

    private val _currentSession = MutableStateFlow<FlightReportSession?>(null)
    val currentSession: StateFlow<FlightReportSession?> = _currentSession.asStateFlow()

    private val _reports = MutableStateFlow<List<FlightReport>>(emptyList())
    val reports: StateFlow<List<FlightReport>> = _reports.asStateFlow()

    init {
        if (flightReportDao != null) {
            scope.launch {
                _reports.value = flightReportDao.getAll(resolveOwnerUserId()).map { it.toDomain() }
            }
        }
    }

    /**
     * Inicia uma nova sessão de relatório.
     * Se já existir uma sessão ativa, ela será substituída.
     */
    fun startReport(
        missionName: String,
        aircraftName: String,
        startedAtMs: Long = System.currentTimeMillis(),
        extraData: Map<String, String> = emptyMap()
    ) {
        stopTimer()
        val safeNow = System.currentTimeMillis()
        _currentSession.value = FlightReportSession(
            id = UUID.randomUUID().toString(),
            missionName = missionName,
            aircraftName = aircraftName,
            createdAtMs = safeNow,
            startedAtMs = startedAtMs,
            elapsedMs = max(0L, safeNow - startedAtMs),
            extraData = extraData
        )
        startTimer()
    }

    /**
     * Atualiza dados extras da sessão ativa.
     * Use para anexar novos campos sem alterar modelo principal.
     */
    fun updateExtraData(extraData: Map<String, String>) {
        val session = _currentSession.value ?: return
        _currentSession.value = session.copy(
            extraData = session.extraData + extraData
        )
    }

    /**
     * Finaliza a sessão ativa e salva relatório.
     */
    fun finishReport(
        finalObservation: String? = null,
        endedAtMs: Long = System.currentTimeMillis()
    ): FlightReport? {
        val session = _currentSession.value ?: return null
        stopTimer()

        val duration = max(0L, endedAtMs - session.startedAtMs)
        val report = FlightReport(
            id = session.id,
            missionName = session.missionName,
            aircraftName = session.aircraftName,
            createdAtMs = session.createdAtMs,
            startedAtMs = session.startedAtMs,
            endedAtMs = endedAtMs,
            durationMs = duration,
            finalObservation = finalObservation,
            extraData = session.extraData
        )

        _reports.value = listOf(report) + _reports.value
        _currentSession.value = null
        if (flightReportDao != null) {
            scope.launch {
                flightReportDao.insert(report.toEntity(ownerUserId = resolveOwnerUserId()))
            }
        }
        return report
    }

    /**
     * Cancela a sessão ativa sem gerar relatório.
     */
    fun cancelCurrentReport() {
        stopTimer()
        _currentSession.value = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                val session = _currentSession.value ?: break
                val now = System.currentTimeMillis()
                _currentSession.value = session.copy(
                    elapsedMs = max(0L, now - session.startedAtMs)
                )
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun FlightReport.toEntity(ownerUserId: String): FlightReportEntity {
        return FlightReportEntity(
            id = id,
            ownerUserId = ownerUserId,
            missionName = missionName,
            aircraftName = aircraftName,
            createdAtMs = createdAtMs,
            startedAtMs = startedAtMs,
            endedAtMs = endedAtMs,
            durationMs = durationMs,
            finalObservation = finalObservation,
            extraDataJson = gson.toJson(extraData)
        )
    }

    private fun FlightReportEntity.toDomain(): FlightReport {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        val parsedExtras = runCatching {
            gson.fromJson<Map<String, String>>(extraDataJson, mapType) ?: emptyMap()
        }.getOrDefault(emptyMap())

        return FlightReport(
            id = id,
            missionName = missionName,
            aircraftName = aircraftName,
            createdAtMs = createdAtMs,
            startedAtMs = startedAtMs,
            endedAtMs = endedAtMs,
            durationMs = durationMs,
            finalObservation = finalObservation,
            extraData = parsedExtras
        )
    }

    private fun resolveOwnerUserId(): String {
        val manager = localSessionManager ?: return GUEST_OWNER_ID
        return runBlocking {
            val userId = manager.currentUserId.first()
            if (userId.isNullOrBlank()) GUEST_OWNER_ID else userId
        }
    }

    companion object {
        private const val GUEST_OWNER_ID = "__guest__"
    }
}
