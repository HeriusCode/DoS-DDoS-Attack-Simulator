package client.view;

/** Immutable row used by the system event log tab. */
record LogEntry(String time, String level, String component, String message) { }
