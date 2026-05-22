package com.dogetennant.dplayerprofiles.command;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class CommandRegistry {

    /** Canonical commands only - used for help and tab completion. */
    private final LinkedHashMap<String, SubCommand> canonical = new LinkedHashMap<>();
    /** Full lookup including aliases. */
    private final Map<String, SubCommand> lookup = new LinkedHashMap<>();

    public void register(SubCommand command) {
        canonical.put(command.getName().toLowerCase(), command);
        lookup.put(command.getName().toLowerCase(), command);
        for (String alias : command.getAliases()) {
            lookup.put(alias.toLowerCase(), command);
        }
    }

    public Optional<SubCommand> get(String name) {
        return Optional.ofNullable(lookup.get(name.toLowerCase()));
    }

    /** Returns only canonical (non-alias) commands, preserving registration order. */
    public Collection<SubCommand> getAll() {
        return Collections.unmodifiableCollection(canonical.values());
    }
}
