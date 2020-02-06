package me.randomhashtags.showcase;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.plugin.Plugin;

@Plugin(
        id = "showcase",
        name = "Showcase",
        description = "Prove everyone you have obtained the most rate and valuable items on the server. Showcased items cannot be taken back!",
        authors = {
                "RandomHashTags"
        }
)
public class ShowcaseSponge {

    @Inject
    private Logger logger;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
    }
}
