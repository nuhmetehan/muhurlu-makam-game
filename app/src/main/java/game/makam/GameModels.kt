package game.makam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val START_YEAR = 1974
const val RECENT_LIMIT = 8

@Serializable
enum class Pillar(val label: String, val shortLabel: String, val description: String) {
    @SerialName("public") Public("Halk Desteği", "Halk", "Halkın yönetime desteği. Gizli kararların tepkisi, karar duyulduğunda ortaya çıkar. Çok düşük destek ayaklanmaya, aşırı destek makamın işlevsiz kalmasına yol açar."),
    @SerialName("military") Military("Ordu ve Güvenlik Yetkisi", "Ordu", "Kuzey Kışlası’nın gücü ve denetim yetkisi. Düşük değer savunmayı zayıflatır; aşırı güç yönetimi tehdit eder."),
    @SerialName("economy") Economy("Kullanılabilir Bütçe", "Ekonomi", "Merkez Planlama Dairesi’nin kullanabildiği Kred ve üretim gelirleri. Vergi ve tasarruf artırır, harcamalar azaltır. Bütçenin tükenmesi iflasa; Kred’in rezervlerde aşırı birikmesi üretim ödemelerinin durmasına yol açar."),
    @SerialName("diplomacy") Diplomacy("Dış Destek ve Bağlılık", "Dış ilişkiler", "Dış bloklarla temas, destek ve taahhütlerin toplamı. Düşük değer yalnızlaşma ve ambargo; yüksek değer dış desteğe bağımlılık riski taşır. Bir blokla yakınlık, diğerinin daha büyük desteğini kaybettirebilir.")
}

typealias Metrics = Map<Pillar, Int>

data class ChoiceImpact(
    val metrics: Metrics, val decision: Metrics, val laws: Metrics, val total: Metrics,
    val lawsSkipped: Boolean
)

fun Metrics.effectSummary(): String = Pillar.entries.mapNotNull { pillar ->
    val value = this[pillar] ?: 0
    if (value == 0) null else "${pillar.shortLabel} ${if (value > 0) "+" else ""}$value"
}.joinToString(" · ").ifEmpty { "Gösterge değişimi yok" }

@Serializable
data class Conditions(
    val minMetrics: Metrics = emptyMap(),
    val maxMetrics: Metrics = emptyMap(),
    val requiredFlags: Set<String> = emptySet(),
    val anyFlags: Set<String> = emptySet(),
    val forbiddenFlags: Set<String> = emptySet(),
    val minTurn: Int = 0, val minQuarters: Int = 0,
    val requiredLaws: Set<String> = emptySet(), val forbiddenLaws: Set<String> = emptySet(),
    val requiredTraits: Set<String> = emptySet()
) {
    fun matches(s: GameState) = s.turn >= minTurn && s.quarters >= minQuarters &&
        minMetrics.all { (p, v) -> s.metrics.getValue(p) >= v } &&
        maxMetrics.all { (p, v) -> s.metrics.getValue(p) <= v } &&
        s.stateFlags.containsAll(requiredFlags) && (anyFlags.isEmpty() || anyFlags.any { it in s.stateFlags }) &&
        forbiddenFlags.none { it in s.stateFlags } &&
        requiredLaws.all { id -> s.laws.any { it.id == id } } &&
        forbiddenLaws.none { id -> s.laws.any { it.id == id } } &&
        (requiredTraits.isEmpty() || s.president.trait in requiredTraits)
}

@Serializable
data class Law(val id: String, val name: String, val description: String, val perTurnEffect: Metrics = emptyMap())
@Serializable
data class Choice(
    val label: String, val effects: Metrics,
    val addFlags: Set<String> = emptySet(), val removeFlags: Set<String> = emptySet(),
    val nextCardId: String? = null, val enactedLaw: Law? = null,
    val repealedLawIds: Set<String> = emptySet(), val result: String = ""
) {
    fun affectedPillars(s: GameState): List<Pillar> {
        val projected = resultingMetrics(s)
        return Pillar.entries.filter { projected.getValue(it) != s.metrics.getValue(it) }
    }

    fun resultingMetrics(s: GameState): Metrics = impact(s).metrics

    fun impact(s: GameState): ChoiceImpact {
        val direct = Pillar.entries.associateWith { pillar ->
            (s.metrics.getValue(pillar) + (effects[pillar] ?: 0)).coerceIn(0, 100)
        }
        // Check direct effects first. Laws already in force drift together at quarter end.
        val lawsSkipped = direct.values.any { it == 0 || it == 100 }
        val metrics = if (lawsSkipped) direct else direct.mapValues { (pillar, value) ->
            (value + s.laws.sumOf { it.perTurnEffect[pillar] ?: 0 }).coerceIn(0, 100)
        }
        return ChoiceImpact(
            metrics = metrics,
            decision = Pillar.entries.associateWith { direct.getValue(it) - s.metrics.getValue(it) },
            laws = Pillar.entries.associateWith { metrics.getValue(it) - direct.getValue(it) },
            total = Pillar.entries.associateWith { metrics.getValue(it) - s.metrics.getValue(it) },
            lawsSkipped = lawsSkipped
        )
    }
}
@Serializable
data class GameCard(
    val id: String, val speaker: String, val speakerRole: String, val avatarId: String,
    val text: String, val conditions: Conditions = Conditions(), val weight: Int = 1,
    val oncePerTerm: Boolean = false, val leftChoice: Choice, val rightChoice: Choice,
    val oncePerPresidency: Boolean = false, val groupId: String? = null
)
@Serializable
data class Ending(val id: String, val title: String, val text: String, val avatarId: String, val assassination: Boolean = false, val metric: Pillar? = null, val extreme: Int? = null)
@Serializable
data class Deck(
    val version: Int, val openingCardId: String, val electionCardId: String,
    val cards: List<GameCard>, val downfalls: List<Ending>, val electionLoss: Ending,
    val crises: Map<String, String>, val presidents: List<String>, val traits: List<String>,
    val objectives: List<Objective> = emptyList(), val dossiers: List<Dossier> = emptyList(),
    val lawReviews: Map<String, String> = emptyMap(), val handoverCardId: String? = null,
    val handoverCardIds: List<String> = emptyList(),
    val resolvedCrises: Map<String, Set<String>> = emptyMap()
)

@Serializable
data class Objective(val id: String, val title: String, val description: String,
    val kind: String = "conditions", val conditions: Conditions = Conditions(),
    val completion: Conditions = Conditions(), val requiredDecisions: Int = 1)
@Serializable
data class ObjectiveProgress(val id: String, val startTurn: Int, val decisions: Int = 0, val status: String = "active")
@Serializable
data class Dossier(val id: String, val title: String, val description: String,
    val started: Conditions, val completion: Conditions, val document: String)
@Serializable
data class Milestone(val id: String, val title: String, val text: String, val turn: Int, val term: Int)
@Serializable
data class DecisionRecord(val turn: Int, val term: Int, val trait: String, val cardId: String,
    val label: String, val result: String, val decision: Metrics, val laws: Metrics, val total: Metrics,
    val lawsSkipped: Boolean, val lawChanges: List<String> = emptyList(), val endingTitle: String? = null)
@Serializable
data class President(val name: String, val trait: String)
@Serializable
data class Legacy(val president: President, val nickname: String, val startTurn: Int, val quarters: Int, val ending: Ending,
    val objectiveTitle: String? = null, val objectiveCompleted: Boolean = false)
@Serializable
data class GameState(
    val president: President, val cardId: String,
    val turn: Int = 0, val quarters: Int = 0,
    val metrics: Metrics = Pillar.entries.associateWith { 50 },
    val stateFlags: Set<String> = emptySet(), val laws: List<Law> = emptyList(),
    val seen: Set<String> = emptySet(), val recent: List<String> = emptyList(),
    val deferredCardId: String? = null,
    val history: List<Legacy> = emptyList(), val inSuccession: Boolean = false,
    val stateVersion: Int = 1, val journal: List<DecisionRecord> = emptyList(),
    val milestones: List<Milestone> = emptyList(), val completedDossiers: Set<String> = emptySet(),
    val completedObjectives: Set<String> = emptySet(), val objective: ObjectiveProgress? = null,
    val usedSpecials: Set<String> = emptySet(), val pendingReviewLawId: String? = null
) {
    val doubleTakesOffice get() = inSuccession && "HAS_BODY_DOUBLE" in stateFlags && history.lastOrNull()?.ending?.assassination == true
    val year get() = START_YEAR + turn / 4
    val quarter get() = turn % 4 + 1
}

fun tenure(quarters: Int): String {
    val years = quarters / 4
    val months = quarters % 4 * 3
    return listOfNotNull(
        if (years > 0) "$years yıl" else null,
        if (months > 0 || years == 0) "$months ay" else null
    ).joinToString(" ")
}
