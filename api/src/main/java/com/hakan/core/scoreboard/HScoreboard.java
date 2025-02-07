package com.hakan.core.scoreboard;

import com.hakan.core.HCore;
import com.hakan.core.utils.ColorUtil;
import com.hakan.core.utils.Validate;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * HScoreboard class for creating a
 * scoreboard object for player
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class HScoreboard {

    protected final Player player;
    protected String title;
    protected String[] lines;

    /**
     * Creates new Instance of this class.
     *
     * @param player Player.
     */
    protected HScoreboard(@Nonnull Player player, @Nonnull String title) {
        this.player = Validate.notNull(player, "uid cannot be null!");
        this.title = Validate.notNull(title, "title cannot be null!");
        this.lines = new String[15];
    }

    /**
     * Checks if scoreboard still exist for player
     *
     * @return if scoreboard still exist for player, return true
     */
    public final boolean isExist() {
        return HScoreboardHandler.has(this.player.getUniqueId());
    }

    /**
     * Gets UUID of player.
     *
     * @return UUID of player.
     */
    @Nonnull
    public final Player getPlayer() {
        return this.player;
    }

    /**
     * Gets title of scoreboard.
     *
     * @return Title of scoreboard.
     */
    @Nonnull
    public final String getTitle() {
        return this.title;
    }

    /**
     * Sets title of scoreboard.
     *
     * @param title Title.
     */
    public final void setTitle(@Nonnull String title) {
        this.title = Validate.notNull(title, "title cannot be null!");
    }

    /**
     * Gets text of line.
     *
     * @param line Line.
     * @return Text of line.
     */
    @Nullable
    public final String getLine(int line) {
        return this.lines[line];
    }

    /**
     * Sets line of scoreboard to text.
     *
     * @param line Line number.
     * @param text Text.
     */
    public final void setLine(int line, @Nonnull String text) {
        this.lines[line] = Validate.notNull(text, "text cannot be null!");
    }

    /**
     * Sets lines of scoreboard to lines.
     *
     * @param lines List of lines.
     */
    public final void setLines(@Nonnull List<String> lines) {
        Validate.notNull(lines, "lines cannot be null!");
        IntStream.range(0, lines.size()).forEach(i -> this.lines[i] = lines.get(i));
    }

    /**
     * Sets lines of scoreboard to lines.
     *
     * @param lines List of lines.
     */
    public final void setLines(@Nonnull String... lines) {
        this.lines = Validate.notNull(lines, "lines cannot be null!");
    }

    /**
     * Removes line from scoreboard.
     *
     * @param line Line number.
     */
    public final void removeLine(int line) {
        this.lines[line] = null;
    }

    /**
     * When the time is up, scoreboard will
     * remove automatically.
     *
     * @param time     Time.
     * @param timeUnit Time unit (TimeUnit.SECONDS, TimeUnit.HOURS, etc.)
     * @return Instance of this class.
     */
    @Nonnull
    public final HScoreboard expire(int time, @Nonnull TimeUnit timeUnit) {
        Validate.notNull(timeUnit, "time unit cannot be null");
        HCore.syncScheduler().after(time, timeUnit).run(this::delete);
        return this;
    }

    /**
     * When the time is up, scoreboard will
     * remove automatically.
     *
     * @param duration Duration.
     * @return Instance of this class.
     */
    @Nonnull
    public final HScoreboard expire(@Nonnull Duration duration) {
        Validate.notNull(duration, "duration cannot be null!");
        HCore.syncScheduler().after(duration).run(this::delete);
        return this;
    }

    /**
     * When the time is up, scoreboard will
     * remove automatically.
     *
     * @param ticks Ticks.
     * @return Instance of this class.
     */
    @Nonnull
    public final HScoreboard expire(int ticks) {
        HCore.syncScheduler().after(ticks)
                .run(this::delete);
        return this;
    }

    /**
     * Once every updateInterval ticks,
     * it will trigger.
     *
     * @param updateInterval Update interval as tick.
     * @return Instance of this class.
     */
    @Nonnull
    public final HScoreboard update(int updateInterval) {
        return this.update(updateInterval, (board) -> {
        });
    }

    /**
     * Once every updateInterval ticks,
     * it will trigger.
     *
     * @param updateInterval Update interval as tick.
     * @param consumer       Callback.
     * @return Instance of this class.
     */
    @Nonnull
    public final HScoreboard update(int updateInterval, @Nonnull Consumer<HScoreboard> consumer) {
        Validate.notNull(consumer, "consumer cannot be null!");
        Validate.isTrue(updateInterval <= 0, "update interval must be greater than 0!");

        HCore.syncScheduler().every(updateInterval)
                .terminateIf(task -> !this.isExist())
                .run(() -> {
                    consumer.accept(this);
                    this.show();
                });
        return this;
    }

    /**
     * Sets the field of the object.
     *
     * @param obj       Object.
     * @param fieldName Field name.
     * @param value     Value.
     */
    @Nonnull
    protected final <T> T setField(@Nonnull T obj,
                                   @Nonnull String fieldName,
                                   @Nonnull Object value) {
        try {
            Validate.notNull(obj, "object cannot be null!");
            Validate.notNull(fieldName, "field name cannot be null!");
            Validate.notNull(value, "value cannot be null!");

            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception ignored) {
        }
        return obj;
    }

    /**
     * Splits text into 3 different parts
     * for prefix, middle and suffix.
     *
     * @param text Text.
     * @param len1 Length of prefix.
     * @param len2 Length of middle.
     * @param len3 Length of suffix.
     * @return 3 parts.
     */
    @Nonnull
    protected final String[] splitLine(int line, @Nonnull String text, int len1, int len2, int len3) {
        String color = (line >= 10) ? "§" + new String[]{"a", "b", "c", "d", "e", "f"}[line - 10] : "§" + line;

        text += new String(new char[len1 + len2 + len3 - text.length() - 6]).replace("\0", "‼");
        String prefixColored = text.substring(0, len1);
        String prefixLastColor = ColorUtil.getLastColors(prefixColored);
        prefixLastColor = prefixLastColor.length() == 0 ? "§r" : prefixLastColor;

        text = text.substring(len1);
        String middle = prefixLastColor + text.substring(0, len2 - prefixLastColor.length() - 2);
        String middleColored = middle + color;
        String middleLastColor = ColorUtil.getLastColors(middle);
        middleLastColor = middleLastColor.length() == 0 ? "§r" : middleLastColor;

        text = text.substring(len2 - prefixLastColor.length() - 2);
        String suffixColored = middleLastColor + text.substring(0, len3 - middleLastColor.length());

        return new String[]{
                prefixColored.replace("‼", ""),
                middleColored.replace("‼", ""),
                suffixColored.replace("‼", "")
        };
    }

    /**
     * Shows the scoreboard to player.
     */
    @Nonnull
    public abstract HScoreboard show();

    /**
     * Deletes scoreboard.
     *
     * @return Instance of this class.
     */
    @Nonnull
    public abstract HScoreboard delete();
}