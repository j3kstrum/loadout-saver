package com.loadoutsaver;

import com.google.inject.Provides;
import com.loadoutsaver.ui.LoadoutSaverPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.Objects;

@Slf4j
@PluginDescriptor(
	name = "Loadout Saver"
)
public class LoadoutSaverPlugin extends Plugin
{
	public static final String CONFIG_GROUP_NAME = "LoadoutSaver";
	public static final String CONFIG_SAVED_LOADOUT_KEY = "savedloadouts";

	@Inject
	private Client client;

	@Inject
	private LoadoutSaverConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private MenuManager menuManager;

	@Inject
	private ClientToolbar clientToolbar;

	private LoadoutManager loadoutManager;

	@Inject
	private LoadoutSaverPanel loadoutSaverPanel;

	private NavigationButton runeliteButton;

	@Override
	protected void startUp() throws Exception
	{
		// Create the button on the right side of the Runelite interface that loads the loadout saver panel.
		BufferedImage icon;
		synchronized (ImageIO.class) {
			icon = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("icon.png")));
		}
		runeliteButton = NavigationButton.builder().tooltip("Loadout Manager").panel(loadoutSaverPanel).icon(icon).build();
		clientToolbar.addNavigation(this.runeliteButton);

		// Load from save file.
		System.out.println("Load from save file.");
		loadoutManager = new LoadoutManager(config, configManager);
		System.out.println("Load complete; loaded " + loadoutManager.size() + " loadouts.");

		loadoutSaverPanel.setManager(loadoutManager);
	}

	@Override
	protected void shutDown() throws Exception
	{
		// Save to save file.
		System.out.println("Saving " + loadoutManager.size() + " loadouts.");
		loadoutManager.save(configManager);
		System.out.println("Successfully saved to configuration.");

		// Unassociate the plugin button from the runelite interface.
		clientToolbar.removeNavigation(this.runeliteButton);
		menuManager.removePlayerMenuItem("Loadout Manager");
	}

	@Provides
	LoadoutSaverConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LoadoutSaverConfig.class);
	}
}
