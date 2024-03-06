package de.mwoehrl.sqlanimatorserver.record;

public record Animation(long id, ExecutionStep[] steps, int querywidth) {

}
