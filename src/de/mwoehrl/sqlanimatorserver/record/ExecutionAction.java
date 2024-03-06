package de.mwoehrl.sqlanimatorserver.record;

public record ExecutionAction(Transition[] transitions, String prevCanvas) {

}
