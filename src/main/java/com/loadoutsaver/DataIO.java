package com.loadoutsaver;

import com.loadoutsaver.implementations.LoadoutImpl;
import com.loadoutsaver.interfaces.ILoadout;
import com.loadoutsaver.interfaces.ISerializable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Data serialization helper methods for the loadout saver.
 */
public class DataIO {
    private static final Charset ENCODING = StandardCharsets.UTF_8;

    private DataIO() {}

    public static List<ILoadout> Parse(String decoded) {
        String[] lines = decoded.split("\n");
        List<ILoadout> result = Arrays.stream(lines).filter(
                l -> !l.isBlank()
        ).map(
                raw -> {
                    try {
                        return LoadoutImpl.Deserializer.DeserializeString(raw);
                    } catch (IllegalArgumentException iae) {
                        System.err.println("Could not parse loadout: " + raw);
                        return null;
                    }
                }
        ).filter(Objects::nonNull).collect(Collectors.toList());
        if (lines.length != result.size()) {
            // TODO: Can we turn off autosave here, just in case?
            System.err.println(
                "WARNING: failed to parse " + (lines.length - result.size()) + " out of " + lines.length + " loadouts."
            );
        }
        return result;
    }

    public static String FullSerialize(Collection<ILoadout> loadouts) {
        List<String> encoded = loadouts.stream().map(ISerializable::SerializeString).collect(Collectors.toList());
        return String.join("\n", encoded);
    }
}
