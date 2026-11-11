# TODO - Implementação de Camada Climática para Missões

## Objetivo
Adicionar uma camada de segurança climática no aplicativo com:
- tela dedicada de clima (detalhada);
- resumo climático na tela de vídeo;
- alerta de risco climático durante operação;
- persistência dos dados de clima no relatório da missão.

A arquitetura deve permitir trocar API de clima sem quebrar dashboard/telas.

---

## Princípios de arquitetura
1. **Interface única de provider climático** (port).
2. **Adapters por API** (Open-Meteo, WeatherAPI, etc.) sem vazar DTO para UI.
3. **Serviço de decisão climática** separado da camada de rede.
4. **UI consome estado padronizado**, não resposta bruta da API.
5. **Dados de clima da missão** salvos no relatório para auditoria pós-voo.
6. **Resiliência por fallback**: provider principal + provider secundário sem alterar telas.
7. **Controle rígido de cotas**: evitar chamadas desnecessárias e bloquear consumo acima do orçamento do protótipo.

---

## Estrutura de pastas proposta (alinhada ao projeto)

### Core (infra compartilhada)
- `core/network/SdiaApiService.kt` (não alterar para clima externo; criar client separado)
- `core/settings/WeatherProviderSettingsRepository.kt` (novo: provider principal/secundário + toggles)
- `core/settings/WeatherQuotaSettingsRepository.kt` (novo: limites de consumo e modo protótipo)

### Feature nova: weather
- `features/weather/domain/model/WeatherSnapshot.kt`
- `features/weather/domain/model/WeatherSafetyLevel.kt`
- `features/weather/domain/model/WeatherSafetyDecision.kt`
- `features/weather/domain/provider/WeatherProvider.kt`
- `features/weather/domain/provider/WeatherProviderRouter.kt` (fallback principal -> secundário)
- `features/weather/domain/provider/WeatherQuotaController.kt` (limites + bloqueio por cota)
- `features/weather/domain/usecase/GetWeatherSummaryUseCase.kt`
- `features/weather/domain/usecase/GetWeatherDetailUseCase.kt`
- `features/weather/domain/usecase/BuildWeatherSafetyDecisionUseCase.kt`
- `features/weather/data/provider/OpenMeteoWeatherProvider.kt`
- `features/weather/data/provider/WeatherApiProvider.kt` (opcional etapa 2)
- `features/weather/data/repository/WeatherRepositoryImpl.kt`
- `features/weather/data/repository/WeatherCacheRepository.kt` (cache local de snapshot)
- `features/weather/data/mapper/*`
- `features/weather/data/remote/dto/*`

### Presentation
- `presentation/app/weather/screens/WeatherOverviewScreen.kt` (novo)
- `presentation/video/screens/DroneCameraScreen.kt` (resumo + alerta)
- `presentation/app/report/screens/ReportScreens.kt` (mostrar clima no relatório)
- `presentation/app/main/activities/MainActivity.kt` (rota da tela de clima)

### Report / Mission integration
- Reaproveitar `features/report/data/manager/FlightReportManager.kt`
- Persistir dados de clima em `extraData` no `finishReport()`

---

## Contrato de domínio padronizado

```kotlin
interface WeatherProvider {
    suspend fun getCurrent(lat: Double, lon: Double): WeatherSnapshot
    suspend fun getHourly(lat: Double, lon: Double): List<WeatherSnapshot>
}
```

```kotlin
interface WeatherProviderRouter {
    suspend fun getCurrentWithFallback(lat: Double, lon: Double): WeatherSnapshot
    suspend fun getHourlyWithFallback(lat: Double, lon: Double): List<WeatherSnapshot>
}
```

```kotlin
data class WeatherQuotaPolicy(
    val maxCallsPerMinute: Int,
    val maxCallsPerHour: Int,
    val maxCallsPerDay: Int,
    val cooldownMinutesOn429: Int,
    val prototypeMode: Boolean
)
```

```kotlin
data class WeatherSnapshot(
    val timestampMs: Long,
    val temperatureC: Double?,
    val rainMm: Double?,
    val windSpeedMs: Double?,
    val windGustMs: Double?,
    val lightningRisk: Double?,
    val conditionCode: String?, // sunny/rain/storm/cloudy
    val providerId: String // WEATHER_API / OPEN_METEO / etc.
)
```

```kotlin
enum class WeatherSafetyLevel { SAFE, CAUTION, NO_FLY }
```

```kotlin
data class WeatherSafetyDecision(
    val level: WeatherSafetyLevel,
    val shortMessage: String,
    val reasons: List<String>
)
```

---

## Regras iniciais de segurança (ajustáveis)
- `NO_FLY`:
- rajada >= 14 m/s
- chuva >= 2.0 mm/h
- risco de raio alto
- `CAUTION`:
- vento médio >= 10 m/s
- chuva entre 0.5 e 1.9 mm/h
- `SAFE`:
- abaixo dos limites acima

Mensagem padrão para vídeo:
- SAFE: `Clima favorável para voo`
- CAUTION: `Atenção: condições climáticas moderadas`
- NO_FLY: `Evite voo por risco climático`

---

## UX planejada

## 1) Tela dedicada de clima
- Route: `weather_overview`
- Conteúdo:
- status atual (ícone, temperatura, vento, chuva, raio)
- nível de segurança (cor + texto)
- explicação dos motivos (reasons)
- visão horária (próximas horas)

## 2) Tela de vídeo (resumo compacto)
- HUD/clima mínimo:
- ícone clima
- temperatura
- vento
- badge de nível (`SAFE/CAUTION/NO_FLY`)
- alerta textual curto quando `NO_FLY`

## 3) Relatório da missão
- seção “Condições Climáticas da Missão”
- snapshot de clima no início e fim da missão
- decisão de segurança registrada
- parâmetros usados (vento, chuva, temperatura, raio)

---

## Persistência no relatório de missão

No `FlightReportManager`, incluir em `extraData`:
- `weather_start_temperature_c`
- `weather_start_wind_ms`
- `weather_start_rain_mm`
- `weather_start_lightning_risk`
- `weather_start_safety_level`
- `weather_end_temperature_c`
- `weather_end_wind_ms`
- `weather_end_rain_mm`
- `weather_end_lightning_risk`
- `weather_end_safety_level`
- `weather_provider`
- `weather_summary_message`

Observação:
- Sem alterar schema de `flight_reports` (já existe `extraDataJson`).

---

## Plano passo a passo de implementação

## Etapa 1 - Fundamentos da feature weather
- Criar modelos de domínio e interface `WeatherProvider`.
- Criar provider inicial `OpenMeteoWeatherProvider`.
- Criar provider `WeatherApiProvider` como principal.
- Criar `WeatherProviderRouter` com fallback (WeatherAPI -> Open-Meteo).
- Criar repositório e mapper para resposta externa -> modelo interno.

## Etapa 2 - Decisão climática
- Implementar `BuildWeatherSafetyDecisionUseCase` com thresholds configuráveis.
- Garantir que qualquer provider gere a mesma saída de decisão.

## Etapa 3 - Tela dedicada de clima
- Criar `WeatherOverviewScreen`.
- Adicionar rota no `MainActivity`.
- Exibir dados atuais e previsão curta.

## Etapa 4 - Resumo na tela de vídeo
- Integrar caso de uso de resumo no `DroneCameraScreen`.
- Exibir widget compacto com ícone + vento + temperatura + status.
- Exibir alerta vermelho para `NO_FLY`.

## Etapa 5 - Integração com fluxo de missão
- Ao iniciar missão: capturar snapshot clima inicial.
- Ao encerrar missão: capturar snapshot final.
- Persistir ambos no `FlightReportManager` via `extraData`.

## Etapa 6 - Exibição no relatório
- Atualizar `ReportDetailScreen` para ler `extraData` e renderizar seção climática.
- Exibir nível de risco com destaque visual.

## Etapa 7 - Configuração de provider (troca fácil)
- Criar `WeatherProviderSettingsRepository`.
- Permitir selecionar provider principal e secundário.
- Injeção via factory/router:
- `WeatherProviderFactory.get(activeProvider)`.
- `WeatherProviderRouterFactory.get(primary, fallback)`.
- Configurar modo protótipo:
- bloquear qualquer provider pago por configuração (`allowedProviders`).
- fail-safe quando provider não permitido for selecionado.

## Etapa 8 - Resiliência e fallback
- Timeout e tratamento de erro por provider.
- Cache curto local (último snapshot válido) para evitar tela vazia.
- Mensagem de indisponibilidade sem quebrar interface.
- Regras de fallback:
- tentar provider principal;
- em timeout/rate-limit/erro de rede, tentar provider secundário;
- registrar no resultado qual provider respondeu.
- Regras de consumo:
- aplicar throttling por janela (minuto/hora/dia);
- bloquear provider temporariamente após `429` (`cooldown`);
- evitar retries agressivos em cascata.

---

## Etapa 10 - Governança de cotas e redução de chamadas desnecessárias
- Implementar `WeatherQuotaController` com contadores por provider:
- chamadas por minuto;
- chamadas por hora;
- chamadas por dia.
- Persistir métricas locais de consumo para sobreviver a restart do app.
- Implementar chave de cache por localização aproximada (`lat/lon` arredondado + janela de tempo).
- Definir TTL mínimo por contexto:
- tela de vídeo: refresh de 60s (ou maior, configurável);
- tela de clima detalhada: 30s a 60s;
- relatório: snapshot único por evento (início/fim), sem polling.
- Debounce de atualização por UI:
- não chamar API ao abrir/fechar tela repetidamente em poucos segundos.
- Estratégia `stale-while-revalidate`:
- mostrar último snapshot válido imediatamente;
- atualizar em background só quando TTL expirar.
- Estratégia para erro de cota:
- detectar `429/quota exceeded`;
- entrar em cooldown;
- alternar para fallback (se disponível e dentro da cota);
- se ambos bloqueados, usar cache + mensagem “Dados climáticos temporariamente limitados”.
- Guardrails de protótipo:
- `prototypeMode=true` por padrão;
- impedir uso de provider marcado como pago;
- não permitir auto-upgrade por configuração de app.

## Etapa 9 - Testes e validação
- Teste de mapeamento DTO -> domínio.
- Teste de regras de decisão (SAFE/CAUTION/NO_FLY).
- Teste de persistência no relatório (`extraData`).
- Teste de UI mínima em vídeo sem poluição visual.

---

## Critérios de aceite
- [x] Feature weather desacoplada por interface (`WeatherProvider`).
- [x] Tela dedicada de clima operacional.
- [x] Resumo climático aparece na tela de vídeo.
- [x] Alerta `Evite voo por risco climático` aparece em condição crítica.
- [x] Dados climáticos entram no relatório de missão (início/fim).
- [x] Troca de provider feita por configuração, sem alterar telas.
- [x] Falha da API não derruba fluxo de missão/vídeo.
- [x] Fallback principal->secundário funcionando em erro/timeout/rate-limit.
- [x] Limites por minuto/hora/dia aplicados por provider.
- [x] Cooldown automático após 429 funcionando.
- [x] Cache+TTL reduzindo chamadas repetidas em vídeo e telas.
- [x] Modo protótipo impedindo uso de provider pago.

---

## Progresso executado no projeto
- [x] `features/weather` criada com domínio, providers, router, quota controller, cache e use cases.
- [x] Provider principal (`WeatherApiProvider`) + fallback (`OpenMeteoWeatherProvider`) funcionando via `DefaultWeatherProviderRouter`.
- [x] `WeatherOverviewScreen` criada e roteada em `MainActivity`.
- [x] `DashboardScreen` com acesso para tela de clima (`Ver clima`).
- [x] `DroneCameraScreen` com resumo climático compacto e mensagem de segurança.
- [x] Integração com relatório: snapshot climático no início/fim da missão em `FlightReportManager.extraData`.
- [x] `ReportDetailScreen` exibindo seção climática da missão.
- [x] Chave `BuildConfig.WEATHER_API_KEY` adicionada para provider WeatherAPI.
- [x] Compilação validada após recuperação de energia (`:app:compileDebugKotlin`).

## Pendências de refinamento (não bloqueantes para o fluxo atual)
- [x] Persistir contadores de quota em storage local para sobreviver ao restart do app.
- [x] Adicionar configuração visual em Settings para trocar provider principal/fallback manualmente.
- [x] Expandir previsão horária real (detalhamento via `forecast.json` e `Open-Meteo hourly`).

---

## Decisões futuras (deixar preparado)
- Alertas push climáticos pré-missão.
- Geofencing com clima por waypoint.
- Regras específicas por modelo de drone.
- Histórico climático por missão para analytics.
