package com.project.grcplatform.constant;

/** Type de contrôle: nature/purpose of the control. */
public enum ControlNature {
    PREVENTIF,   // Préventif  — avoids the risk from materialising
    DETECTIF,    // Détectif   — detects when the risk materialises
    CORRECTIF    // Correctif  — limits damage after the fact
}
