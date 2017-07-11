package io.github.icohedron.pictionary;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.selector.Selector;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Plugin(id = "pictionary", name = "Pictionary", version = "1.0.0",
        description = "Set an answer and the plugin will announce the first player who typed the answer in chat")
public class Pictionary {

    @Inject
    private Logger logger;

    private final Text prefix = Text.of(TextColors.GRAY, "[", TextColors.GOLD, "Pictionary", TextColors.GRAY, "] ");

    private String answer;
    private String prevAnswer;

    @Listener
    public void onInitializationEvent(GameInitializationEvent event) {
        answer = null;
        prevAnswer = null;

        CommandSpec setString = CommandSpec.builder()
                .description(Text.of("Set an answer via string"))
                .permission("pictionary.command.set.string")
                .arguments(GenericArguments.remainingJoinedStrings(Text.of("string")))
                .executor((src, args) -> {
                    Optional<String> arg = args.getOne("string");
                    if (arg.isPresent()) {
                        answer = arg.get();
                        src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Answer was set to '", TextColors.WHITE, answer, TextColors.YELLOW, "'"));
                        return CommandResult.success();
                    }
                    src.sendMessage(Text.of(TextColors.RED, "An error has occurred"));
                    return CommandResult.empty();
                })
                .build();

        /* Known bugs with 'set entity' command (Minecraft 1.10.2, Sponge API 5.1.0):
         * Selector arguments 'c=', 'tag=', 'score_*=', 'score_*_min=1' do not work. There may be more. This is an issue with Sponge, not the plugin.
         * A workaround is to execute off of the entity you wish to set as the answer and make it select itself as the answer. (e.g. 'execute @r ~ ~ ~ pictionary set entity @e[r=0]')
         */
        CommandSpec setEntity = CommandSpec.builder()
                .description(Text.of("Set an answer via entity"))
                .permission("pictionary.command.set.entity")
                .arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("entity"))))
                .executor((src, args) -> {
                    Optional<String> arg = args.getOne("entity");
                    if (arg.isPresent()) {
                        Set<Entity> entities = Selector.parse(arg.get()).resolve(src);

                        if (entities.size() == 0) {
                            src.sendMessage(Text.of(prefix, TextColors.RED, "No entity found by the selector specified"));
                            return CommandResult.empty();
                        }

                        if (entities.size() > 1) {
                            src.sendMessage(Text.of(prefix, TextColors.RED, "The selector specified found more than one entity"));
                            return CommandResult.empty();
                        }

                        Entity entity = entities.iterator().next();
                        Optional<Text> displayName = entity.get(Keys.DISPLAY_NAME);
                        if (displayName.isPresent()) {
                            answer = displayName.get().toPlain();
                            src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Answer was set to '", TextColors.WHITE, answer, TextColors.YELLOW, "'"));
                            return CommandResult.success();
                        }

                        src.sendMessage(Text.of(prefix, TextColors.RED, "Entity has no display name"));
                        return CommandResult.empty();
                    }
                    src.sendMessage(Text.of(TextColors.RED, "An error has occurred"));
                    return CommandResult.empty();
                })
                .build();

        CommandSpec set = CommandSpec.builder()
                .description(Text.of("Set an answer"))
                .child(setString, "string")
                .child(setEntity, "entity")
                .build();

        CommandSpec clear = CommandSpec.builder()
                .description(Text.of("Register an answer"))
                .permission("pictionary.command.clear")
                .executor((src, args) -> {
                    answer = null;
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, "The current answer was cleared"));
                    return CommandResult.success();
                })
                .build();

        CommandSpec status = CommandSpec.builder()
                .description(Text.of("Status"))
                .permission("pictionary.command.status")
                .executor((src, args) -> {
                    if (answer == null) {
                        src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Waiting for answer to be set"));
                        return CommandResult.success();
                    }
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Scanning player messages for the correct answer"));
                    return CommandResult.empty();
                })
                .build();

        CommandSpec pictionary = CommandSpec.builder()
                .description(Text.of("Pictionary"))
                .child(set, "set")
                .child(clear, "clear")
                .child(status, "status")
                .build();

        Sponge.getCommandManager().register(this, pictionary, "pictionary");

        logger.info("Plugin was successfully initialized");
    }

    @Listener
    public void onChatEvent(MessageEvent event, @First Player player) {
        if (answer == null) {
            return;
        }
        String message = event.getOriginalMessage().toPlain();

        String[] validAnswers = new String[3];
        validAnswers[0] = answer;
        validAnswers[1] = answer.replace("-","");
        validAnswers[2] = answer.replace("-"," ");

        for (String ans : validAnswers) {
            if (Pattern.compile("^<.*>\\s*" + ans + "\\s*$", Pattern.CASE_INSENSITIVE).matcher(message).find()) {
                prevAnswer = answer;
                Task.builder().execute(() -> MessageChannel.TO_ALL.send(Text.of(prefix, TextColors.GREEN, player.getName(), TextColors.YELLOW, " has gotten the correct answer! The answer was '", TextColors.WHITE, prevAnswer, TextColors.YELLOW, "'"))).delayTicks(1).submit(this); // Delay by a tick to let the player's message show up in chat before the player is announced as a winner.
                // event.setMessageCancelled(true);
                answer = null;
                return;
            }
        }
    }
}
