package com.loadoutsaver;

import com.loadoutsaver.interfaces.ILoadout;
import com.loadoutsaver.interfaces.ISubscriber;
import net.runelite.client.config.ConfigManager;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import com.loadoutsaver.LoadoutSaverPlugin;

/**
 * The loadout manager is responsible for coordinating access and updates to and from the user's set of loadouts.
 */
public class LoadoutManager {
    private final List<ILoadout> loadouts;

    private final HashSet<ISubscriber<Stream<ILoadout>>> subscribers = new HashSet<>();

    private final LoadoutSaverConfig config;
    private final ConfigManager configManager;

    public LoadoutManager(LoadoutSaverConfig config, ConfigManager configManager) {
        this(DataIO.Parse(config.loadouts()), config, configManager);
    }

    /**
     * Saves the active loadout collection to the runelite configuration file.
     * @param configManager The configuration manager, used to write to the configuration file.
     */
    public void save(ConfigManager configManager) {
        System.out.println("Serializing...");
        String serialized = DataIO.FullSerialize(loadouts);
        System.out.println("Saving...");
        configManager.setConfiguration(
                LoadoutSaverPlugin.CONFIG_GROUP_NAME,
                LoadoutSaverPlugin.CONFIG_SAVED_LOADOUT_KEY,
                serialized
        );
        System.out.println("Save complete.");
    }

    public void save() {
        this.save(this.configManager);
    }

    private LoadoutManager(List<ILoadout> loadouts, LoadoutSaverConfig config, ConfigManager configManager) {
        this.loadouts = loadouts;
        this.configManager = configManager;
        this.config = config;
    }

    public Stream<ILoadout> GetLoadouts() {
        return loadouts.stream();
    }

    public void AddLoadout(ILoadout loadout) {
        this.loadouts.add(loadout);
        if (config.autoSave()) {
            this.save();
        }
        for (ISubscriber<Stream<ILoadout>> subscriber : this.subscribers) {
            subscriber.Update(GetLoadouts());
        }
    }

    public void RemoveLoadout(ILoadout loadout) {
        this.loadouts.remove(loadout);
        if (config.autoSave()) {
            this.save();
        }
        for (ISubscriber<Stream<ILoadout>> subscriber : this.subscribers) {
            subscriber.Update(GetLoadouts());
        }
    }

    public void ClearLoadouts() {
        this.loadouts.clear();
        if (config.autoSave()) {
            this.save();
        }
        for (ISubscriber<Stream<ILoadout>> subscriber : this.subscribers) {
            subscriber.Update(GetLoadouts());
        }
    }

    public int size() {
        return loadouts.size();
    }

    public void Subscribe(ISubscriber<Stream<ILoadout>> subscriber) {
        subscribers.add(subscriber);
        subscriber.Update(GetLoadouts());
    }

    public void UnSubscribe(ISubscriber<Stream<ILoadout>> subscriber) {
        subscribers.remove(subscriber);
    }
}
