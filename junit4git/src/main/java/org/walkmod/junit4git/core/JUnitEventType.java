package org.walkmod.junit4git.core;

public enum JUnitEventType {

  START("start"), STOP("stop");

  private String name;

  JUnitEventType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
