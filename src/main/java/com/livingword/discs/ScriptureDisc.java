package com.livingword.discs;

import com.livingword.network.LivingWordNetwork;
import com.livingword.lectern.LecternEvents;
import com.livingword.sync.ListeningSession;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

public final class ScriptureDisc extends Item {
    private final String displayKey;
    private final String translationId;
    private final String bookId;
    private final int startChapter;
    private final int endChapter;
    private final String audioManifestId;

    public ScriptureDisc(
        Properties properties,
        String displayKey,
        String translationId,
        String bookId,
        int startChapter,
        int endChapter,
        String audioManifestId
    ) {
        super(properties);
        this.displayKey = displayKey;
        this.translationId = translationId;
        this.bookId = bookId;
        this.startChapter = startChapter;
        this.endChapter = endChapter;
        this.audioManifestId = audioManifestId;
    }

    public String displayKey() {
        return displayKey;
    }

    public String translationId() {
        return translationId;
    }

    public String bookId() {
        return bookId;
    }

    public int startChapter() {
        return startChapter;
    }

    public int endChapter() {
        return endChapter;
    }

    public String audioManifestId() {
        return audioManifestId;
    }

    public boolean handlesBlockUse(BlockState state) {
        return handlesBlockUseId(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public static boolean handlesBlockUseId(ResourceLocation blockId) {
        return ResourceLocation.withDefaultNamespace("jukebox").equals(blockId);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (!handlesBlockUse(level.getBlockState(context.getClickedPos()))) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            BlockPos pos = context.getClickedPos();
            if (!(level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) {
                return InteractionResult.PASS;
            }
            if (!jukebox.getTheItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            if (player.isShiftKeyDown() && ScriptureDiscEvents.stopAndForgetJukeboxSession(level.dimension().location(), pos)) {
                serverPlayer.displayClientMessage(Component.translatable("message.livingword.disc.session_reset"), true);
                return InteractionResult.CONSUME;
            }
            ScriptureDiscSelection selection = ScriptureDiscSelection.from(stack);
            ScriptureDiscEvents.stopActiveJukeboxSession(level.dimension().location(), pos);
            LecternEvents.pauseSessionsForParticipant(serverPlayer);
            ScriptureDiscEvents.pauseSessionsForParticipant(serverPlayer);
            insertIntoJukebox(level, pos, level.getBlockState(pos), serverPlayer, stack, jukebox);
            long resumePositionMillis = ScriptureDiscEvents.resumePosition(level.dimension().location(), pos, selection);
            ListeningSession session = LivingWordNetwork.startPositionedListeningSession(
                serverPlayer,
                pos,
                selection.translationId(),
                selection.bookId(),
                selection.chapter(),
                selection.audioManifestId(),
                48.0D,
                resumePositionMillis
            );
            ScriptureDiscEvents.rememberJukeboxSession(level.dimension().location(), pos, selection, session.id(), resumePositionMillis);
            serverPlayer.displayClientMessage(Component.translatable("message.livingword.disc.session_started", formatSelection(selection)), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            openSelectionScreen(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(displayKey).withStyle(ChatFormatting.GOLD));
        ScriptureDiscSelection selection = ScriptureDiscSelection.from(stack);
        tooltip.add(Component.literal(formatSelection(selection)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.livingword.scripture_disc.tooltip.configure").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.livingword.scripture_disc.tooltip.jukebox").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.livingword.scripture_disc.tooltip.stop").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String formatSelection(ScriptureDiscSelection selection) {
        ScriptureDiscAudioSource audioSource = ScriptureDiscAudioSource.byManifestId(selection.translationId(), selection.audioManifestId());
        return selection.translationId().toUpperCase(java.util.Locale.ROOT)
            + " / "
            + formatBookId(selection.bookId())
            + " "
            + selection.chapter()
            + " / "
            + audioSource.displayName()
            + " / "
            + selection.playbackMode().displayName();
    }

    private static String formatBookId(String bookId) {
        String[] words = bookId.replace('_', ' ').split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!formatted.isEmpty()) {
                formatted.append(' ');
            }
            formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return formatted.toString();
    }

    private static void insertIntoJukebox(Level level, BlockPos pos, BlockState state, ServerPlayer player, ItemStack stack, JukeboxBlockEntity jukebox) {
        ItemStack inserted = stack.copy();
        inserted.setCount(1);
        jukebox.setTheItem(inserted);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.awardStat(Stats.PLAY_RECORD);
    }

    private static void openSelectionScreen(InteractionHand hand) {
        try {
            Class<?> client = Class.forName("com.livingword.client.LivingWordClient");
            client.getMethod("openScriptureDiscSelection", InteractionHand.class).invoke(null, hand);
        } catch (ReflectiveOperationException exception) {
            // Dedicated servers never call this path; a warning is enough if client wiring is missing.
        }
    }
}
