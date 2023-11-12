package com.loadoutsaver;

import com.loadoutsaver.implementations.LoadoutImpl;
import com.loadoutsaver.interfaces.ILoadout;
import com.loadoutsaver.interfaces.ISerializable;

import javax.inject.Inject;
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

    @Inject
    private LoadoutSaverConfig config;

    private DataIO() {}

    /**
     * Parses the given loadout collection string into an ordered list of loadouts.
     * @param decoded The raw, unencoded string representing a list of newline-separated loadouts.
     * @return The list of parsed loadouts. Any loadouts that fail to parse will be discarded.
     */
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

    /**
     * Fully serializes the given collection of loadouts into its string representation.
     * @param loadouts The loadouts to be serialized.
     * @return A newline-separated list of serialized loadouts representing the provided collection.
     */
    public static String FullSerialize(Collection<ILoadout> loadouts) {
        List<String> encoded = loadouts.stream().map(ISerializable::SerializeString).collect(Collectors.toList());
        return String.join("\n", encoded);
    }
}
