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
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;
import java.util.regex.Pattern;

@Plugin(id = "pictionary", name = "Pictionary", version = "3.0.0-S7.0",
        description = "Provides mechanics that make Pictionary possible", authors = {"Icohedron"})
public class Pictionary {

    @Inject
    private Logger logger;

    private final Text prefix = Text.of(TextColors.GRAY, "[", TextColors.GOLD, "Pictionary", TextColors.GRAY, "] ");

    private String answer;
    private String prevAnswer;
    private Player artist;

    @Listener
    public void onInitializationEvent(GameInitializationEvent event) {
        answer = null;
        prevAnswer = null;
        artist = null;

        CommandSpec setAnswerByString = CommandSpec.builder()
                .description(Text.of("Set an answer via string"))
                .permission("pictionary.command.answer.setbystring")
                .arguments(GenericArguments.remainingJoinedStrings(Text.of("string")))
                .executor((src, args) -> {
                    String answer = args.<String>getOne("string").get();
                    setAnswer(answer);
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Answer was set to '", TextColors.WHITE, answer, TextColors.YELLOW, "'"));
                    return CommandResult.success();
                })
                .build();

        CommandSpec setAnswerByEntity = CommandSpec.builder()
                .description(Text.of("Set an answer via an entity name"))
                .permission("pictionary.command.answer.setbyentity")
                .arguments(GenericArguments.onlyOne(GenericArguments.entity(Text.of("entity"))))
                .executor((src, args) -> {
                    Entity entity = args.<Entity>getOne("entity").get();
                    Optional<Text> displayName = entity.get(Keys.DISPLAY_NAME);
                    if (displayName.isPresent()) {
                        setAnswer(displayName.get().toPlain());
                        src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Answer was set to '", TextColors.WHITE, answer, TextColors.YELLOW, "'"));
                        return CommandResult.success();
                    }

                    src.sendMessage(Text.of(prefix, TextColors.RED, "Entity has no display name"));
                    return CommandResult.empty();
                })
                .build();

        CommandSpec answerStatus = CommandSpec.builder()
                .description(Text.of("Answer status"))
                .permission("pictionary.command.answer.status")
                .executor((src, args) -> {
                    if (answer == null) {
                        src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Ready to set the next answer"));
                        return CommandResult.success();
                    }
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Waiting for player to guess the correct answer"));
                    return CommandResult.empty();
                })
                .build();

        CommandSpec clearAnswer = CommandSpec.builder()
                .description(Text.of("Clear artist"))
                .permission("pictionary.command.artist.clear")
                .executor((src, args) -> {
                    setAnswer(null);
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, "The current answer was cleared"));
                    return CommandResult.success();
                })
                .build();

        CommandSpec answer = CommandSpec.builder()
                .description(Text.of("Modify answer"))
                .child(setAnswerByEntity, "setByEntity")
                .child(setAnswerByString, "setByString")
                .child(answerStatus, "status")
                .child(clearAnswer, "clear")
                .build();



        CommandSpec setArtist = CommandSpec.builder()
                .description(Text.of("Set the artist"))
                .permission("pictionary.command.artist.set")
                .arguments(GenericArguments.onlyOne(GenericArguments.player(Text.of("player"))))
                .executor((src, args) -> {
                        Player player = args.<Player>getOne("player").get();
                        setArtist(player);
                        src.sendMessage(Text.of(prefix, TextColors.YELLOW, "The artist was set to '", TextColors.WHITE, artist.getName(), TextColors.YELLOW, "'"));
                        return CommandResult.success();
                })
                .build();

        CommandSpec artistStatus = CommandSpec.builder()
                .description(Text.of("Artist status"))
                .permission("pictionary.command.artist.status")
                .executor((src, args) -> {
                    if (artist == null) {
                        src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Ready to set the next artist"));
                        return CommandResult.success();
                    }
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Current artist is ", TextColors.GREEN, artist.getName()));
                    return CommandResult.empty();
                })
                .build();

        CommandSpec clearArtist = CommandSpec.builder()
                .description(Text.of("Clear artist"))
                .permission("pictionary.command.artist.clear")
                .executor((src, args) -> {
                    setArtist(null);
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, "The current artist was cleared"));
                    return CommandResult.success();
                })
                .build();

        CommandSpec artist = CommandSpec.builder()
                .description(Text.of("Modify artist"))
                .child(setArtist, "set")
                .child(artistStatus, "status")
                .child(clearArtist, "clear")
                .build();



        CommandSpec clear = CommandSpec.builder()
                .description(Text.of("Clear artist and answer"))
                .permission("pictionary.command.clear")
                .executor((src, args) -> {
                    setAnswer(null);
                    setArtist(null);
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, "The current answer and artist were cleared"));
                    return CommandResult.success();
                })
                .build();

        CommandSpec pictionary = CommandSpec.builder()
                .description(Text.of("Pictionary"))
                .child(answer, "answer")
                .child(artist, "artist")
                .child(clear, "clear")
                .build();

        Sponge.getCommandManager().register(this, pictionary, "pictionary", "pc");

        logger.info("Finished initialization");
    }

    @Listener
    public void onChatEvent(MessageEvent event, @First Player player) {
        getAnswer().ifPresent(answer -> {

            if (getArtist().isPresent()) {
                if (getArtist().get().getUniqueId().equals(player.getUniqueId())) {
                    return;
                }
            }

            String message = event.getOriginalMessage().toPlain();

            String[] validAnswers = new String[3];
            validAnswers[0] = answer;
            validAnswers[1] = answer.replace("-","");
            validAnswers[2] = answer.replace("-"," ");

            for (String ans : validAnswers) {
                if (Pattern.compile("^<.*>\\s*" + Pattern.quote(ans) + "\\s*$", Pattern.CASE_INSENSITIVE).matcher(message).find()) {
                    setAnswer(null);
                    setArtist(null);
                    Task.builder().execute(() -> MessageChannel.TO_ALL.send(Text.of(prefix, TextColors.GREEN, player.getName(), TextColors.YELLOW, " has gotten the correct answer! The answer was '", TextColors.WHITE, getPrevAnswer().get(), TextColors.YELLOW, "'"))).delayTicks(1).submit(this); // Delay by a tick to let the player's message show up in chat before the player is announced as a winner.
                    return;
                }
            }
        });
    }

    @Listener
    public void onDisconnectEvent(ClientConnectionEvent.Disconnect event, @First Player player) {
        getArtist().ifPresent(artist -> {
            if (player.getUniqueId().equals(artist.getUniqueId())) {
                setArtist(null);
            }
        });
    }

    public Optional<String> getAnswer() {
        return Optional.ofNullable(answer);
    }

    public void setAnswer(String answer) {
        this.prevAnswer = this.answer;
        this.answer = answer;
    }

    public Optional<String> getPrevAnswer() {
        return Optional.ofNullable(prevAnswer);
    }

    public Optional<Player> getArtist() {
        return Optional.ofNullable(artist);
    }

    private void setArtist(Player player) {
        getArtist().ifPresent(artist -> {
            artist.offer(Keys.CAN_FLY, false);
            artist.offer(Keys.IS_FLYING, false);
        });

        if (player != null) {
            player.offer(Keys.CAN_FLY, true);
        }
        this.artist = player;
    }
}
