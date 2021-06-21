package de.photon.aacadditionpro;

import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.commands.MainCommand;
import de.photon.aacadditionpro.util.messaging.ChatMessage;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

class CommandTest
{
    private static AACAdditionPro mock = Dummy.mockAACAdditionPro();

    private static List<String> getTabCompletions(String... arguments)
    {
        return MainCommand.getInstance().onTabComplete(new DummyCommandSender(), null, "aacap", arguments);
    }

    @Test
    void commandHelpTest()
    {
        DummyCommandSender dummySender = new DummyCommandSender();
        MainCommand.getInstance().onCommand(dummySender, null, "aacadditionpro", new String[]{"help"});
        final List<String> expectedLines = MainCommand.getInstance().getCommandAttributes().getCommandHelp().stream()
                                                      .map(line -> ChatMessage.AACADDITIONPRO_PREFIX + line)
                                                      .collect(Collectors.toList());
        Assertions.assertLinesMatch(expectedLines, dummySender.sentMessages, "Command help not functional.");
    }

    @Test
    void tabCompleteTest()
    {
        Assertions.assertLinesMatch(ImmutableList.of("on", "off", "help"), getTabCompletions("verbose"));
        Assertions.assertLinesMatch(ImmutableList.of("on", "off"), getTabCompletions("verbose", "o"));
        Assertions.assertLinesMatch(ImmutableList.of("off"), getTabCompletions("verbose", "of"));
        Assertions.assertLinesMatch(ImmutableList.of("help"), getTabCompletions("verbose", "h"));
    }

    private static class DummyCommandSender implements ConsoleCommandSender
    {
        final List<String> sentMessages = new ArrayList<>();

        @Override
        public void sendMessage(@NotNull String message)
        {
            sentMessages.add(message);
        }

        @Override
        public void sendMessage(@NotNull String[] messages)
        {
            sentMessages.addAll(Arrays.asList(messages));
        }

        @Override
        public void sendMessage(@Nullable UUID sender, @NotNull String message)
        {
            sentMessages.add(message);
        }

        @Override
        public void sendMessage(@Nullable UUID sender, @NotNull String[] messages)
        {
            sentMessages.addAll(Arrays.asList(messages));
        }

        @NotNull
        @Override
        public Server getServer()
        {
            return null;
        }

        @NotNull
        @Override
        public String getName()
        {
            return "Console";
        }

        @NotNull
        @Override
        public Spigot spigot()
        {
            return null;
        }

        @Override
        public boolean isConversing()
        {
            return false;
        }

        @Override
        public void acceptConversationInput(@NotNull String input)
        {

        }

        @Override
        public boolean beginConversation(@NotNull Conversation conversation)
        {
            return false;
        }

        @Override
        public void abandonConversation(@NotNull Conversation conversation)
        {

        }

        @Override
        public void abandonConversation(@NotNull Conversation conversation, @NotNull ConversationAbandonedEvent details)
        {

        }

        @Override
        public void sendRawMessage(@NotNull String message)
        {

        }

        @Override
        public void sendRawMessage(@Nullable UUID sender, @NotNull String message)
        {

        }

        @Override
        public boolean isPermissionSet(@NotNull String name)
        {
            return true;
        }

        @Override
        public boolean isPermissionSet(@NotNull Permission perm)
        {
            return true;
        }

        @Override
        public boolean hasPermission(@NotNull String name)
        {
            return true;
        }

        @Override
        public boolean hasPermission(@NotNull Permission perm)
        {
            return true;
        }

        @NotNull
        @Override
        public PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value)
        {
            return null;
        }

        @NotNull
        @Override
        public PermissionAttachment addAttachment(@NotNull Plugin plugin)
        {
            return null;
        }

        @Nullable
        @Override
        public PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks)
        {
            return null;
        }

        @Nullable
        @Override
        public PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks)
        {
            return null;
        }

        @Override
        public void removeAttachment(@NotNull PermissionAttachment attachment) {}

        @Override
        public void recalculatePermissions() {}

        @NotNull
        @Override
        public Set<PermissionAttachmentInfo> getEffectivePermissions()
        {
            return Collections.emptySet();
        }

        @Override
        public boolean isOp()
        {
            return true;
        }

        @Override
        public void setOp(boolean value) {}
    }
}
