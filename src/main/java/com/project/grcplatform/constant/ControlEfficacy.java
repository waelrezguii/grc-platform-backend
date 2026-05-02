package com.project.grcplatform.constant;

/**
 * Efficacité du contrôle — computed automatically from formalism, nature, timing,
 * tracability, conformite4Yeux, and performance.
 *
 * Computation rules (applied in priority order):
 *
 * INEXISTANT if:
 *   - timing = AUTOMATIQUE  AND performance = NON_PERFORMANT
 *   - OR nature = CORRECTIF AND performance = MOYENNEMENT_PERFORMANT AND timing IN {SEMI_AUTOMATIQUE, MANUEL}
 *
 * INSUFFISANT if (not INEXISTANT) AND any of:
 *   - formalism = NON_FORMAL
 *   - nature    = CORRECTIF
 *   - conformite4Yeux = false
 *
 * EFFICACE otherwise (all conditions green)
 */
public enum ControlEfficacy {
    EFFICACE,     // Efficace    — control is effective
    INSUFFISANT,  // Insuffisant — control is insufficient
    INEXISTANT    // Inexistant  — control effectively does not exist
}
