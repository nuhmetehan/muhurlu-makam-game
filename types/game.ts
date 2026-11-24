export const pillars = ["public", "military", "economy", "diplomacy"] as const;
export type Pillar = typeof pillars[number];
export type Metrics = Readonly<Record<Pillar, number>>;
export type Effects = Readonly<Partial<Metrics>>;

export interface Conditions {
  readonly minMetrics?: Effects;
  readonly maxMetrics?: Effects;
  readonly requiredFlags?: readonly string[];
  readonly anyFlags?: readonly string[];
  readonly forbiddenFlags?: readonly string[];
  readonly minTurn?: number;
  readonly minQuarters?: number;
  readonly requiredLaws?: readonly string[];
  readonly forbiddenLaws?: readonly string[];
  readonly requiredTraits?: readonly string[];
}
export interface Law {
  readonly id: string;
  readonly name: string;
  readonly description: string;
  readonly perTurnEffect?: Effects;
}
export interface Choice {
  readonly label: string;
  readonly effects: Effects;
  readonly addFlags?: readonly string[];
  readonly removeFlags?: readonly string[];
  readonly nextCardId?: string;
  readonly enactedLaw?: Law;
  readonly repealedLawIds?: readonly string[];
  readonly result?: string;
}
export interface GameCard {
  readonly id: string;
  readonly speaker: string;
  readonly speakerRole: string;
  readonly avatarId: "viktor" | "elena" | "varga" | "marko" | "clara" | "sterling";
  readonly text: string;
  readonly conditions?: Conditions;
  readonly weight: number;
  readonly oncePerTerm: boolean;
  readonly oncePerPresidency?: boolean;
  readonly groupId?: string;
  readonly leftChoice: Choice;
  readonly rightChoice: Choice;
}
export interface Ending {
  readonly avatarId: string;
  readonly assassination?: boolean;
  readonly id: string;
  readonly title: string;
  readonly text: string;
}
export interface Downfall extends Ending {
  readonly metric: Pillar;
  readonly extreme: 0 | 100;
}
export interface Deck {
  readonly version: number;
  readonly openingCardId: string;
  readonly electionCardId: string;
  readonly cards: readonly GameCard[];
  readonly downfalls: readonly Downfall[];
  readonly electionLoss: Ending;
  readonly crises: Readonly<Record<string, string>>;
  readonly presidents: readonly string[];
  readonly traits: readonly string[];
  readonly objectives?: readonly Objective[];
  readonly dossiers?: readonly Dossier[];
  readonly lawReviews?: Readonly<Record<string, string>>;
  readonly handoverCardId?: string;
  readonly handoverCardIds?: readonly string[];
  readonly resolvedCrises?: Readonly<Record<string, readonly string[]>>;
}
export interface Objective {
  readonly id: string; readonly title: string; readonly description: string;
  readonly kind?: "conditions" | "election" | "revision";
  readonly conditions?: Conditions; readonly completion?: Conditions;
  readonly requiredDecisions?: number;
}
export interface ObjectiveProgress {
  readonly id: string; readonly startTurn: number; readonly decisions: number;
  readonly status: "active" | "completed";
}
export interface Dossier {
  readonly id: string; readonly title: string; readonly description: string;
  readonly started: Conditions; readonly completion: Conditions; readonly document: string;
}
export interface Milestone {
  readonly id: string; readonly title: string; readonly text: string; readonly turn: number;
  readonly term: number;
}
export interface DecisionRecord {
  readonly turn: number; readonly term: number; readonly trait: string; readonly cardId: string;
  readonly label: string; readonly result: string; readonly decision: Metrics; readonly laws: Metrics;
  readonly total: Metrics; readonly lawsSkipped: boolean; readonly lawChanges: readonly string[];
  readonly endingTitle?: string;
}
export interface President { readonly name: string; readonly trait: string }
export interface Legacy {
  readonly president: President;
  readonly nickname: string;
  readonly startTurn: number;
  readonly quarters: number;
  readonly ending: Ending;
  readonly objectiveTitle?: string | null;
  readonly objectiveCompleted?: boolean;
}
export interface GameState {
  readonly president: President;
  readonly cardId: string;
  readonly turn: number;
  readonly quarters: number;
  readonly metrics: Metrics;
  readonly stateFlags: readonly string[];
  readonly laws: readonly Law[];
  readonly seen: readonly string[];
  readonly recent?: readonly string[];
  readonly deferredCardId: string | null;
  readonly history: readonly Legacy[];
  readonly inSuccession: boolean;
  readonly stateVersion?: number;
  readonly journal?: readonly DecisionRecord[];
  readonly milestones?: readonly Milestone[];
  readonly completedDossiers?: readonly string[];
  readonly completedObjectives?: readonly string[];
  readonly objective?: ObjectiveProgress | null;
  readonly usedSpecials?: readonly string[];
  readonly pendingReviewLawId?: string | null;
}
