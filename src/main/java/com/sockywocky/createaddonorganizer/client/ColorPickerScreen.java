package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ColorPickerScreen extends Screen {
    private enum Mode { COLOR, IMAGE }

    private enum EditTarget { BANNER, BOX, TEXT, HIGHLIGHT }

    private enum TextTarget { PRIMARY, SECONDARY }

    private final Screen parent;
    private final ResourceLocation id;
    private final Component sectionName;

    private Mode mode;
    private EditTarget target = EditTarget.BANNER;
    private TextTarget textEditTarget = TextTarget.PRIMARY;

    private final Hsva bannerHsva;
    private final Hsva boxHsva;
    private final Hsva textHsva;
    private Hsva text2Hsva;
    private boolean twoTone;

    private final boolean isMainTab;
    private final boolean highlightOnly;
    private boolean hasHighlight;
    private final Hsva highlightHsva;

    private ResourceLocation selectedTexture;
    private String selectedRef;
    private boolean bannerAnimated;
    private int bannerFrameTicks = 4;

    private int previewY;

    public ColorPickerScreen(Screen parent, ResourceLocation id, Component sectionName, boolean isMainTab) {
        super(Component.translatable("createaddonorganizer.colors.pick"));
        this.parent = parent;
        this.id = id;
        this.sectionName = sectionName;

        this.bannerHsva = Hsva.fromArgb(Config.bannerColorFor(id));
        this.boxHsva = Hsva.fromArgb(Config.boxColorFor(id));
        this.textHsva = Hsva.fromArgb(Config.textColorFor(id));
        Integer secondary = Config.textSecondaryColorFor(id);
        this.twoTone = secondary != null;
        this.text2Hsva = Hsva.fromArgb(secondary != null ? secondary : 0xFFCEA05A);

        this.selectedRef = Config.bannerRefFor(id);
        if (selectedRef != null) {
            this.selectedTexture = BannerTextures.resolve(selectedRef);
            this.mode = Mode.IMAGE;
            syncAnimationFields();
        } else {
            this.mode = Mode.COLOR;
        }

        this.isMainTab = isMainTab;
        this.highlightOnly = SimulatedSupport.isMainTab(id);
        if (highlightOnly) {
            this.target = EditTarget.HIGHLIGHT;
        }
        Integer highlightOverride = Config.highlightColorFor(id);
        this.hasHighlight = highlightOverride != null;
        this.highlightHsva = Hsva.fromArgb(highlightOverride != null ? highlightOverride : 0xFF4A90D9);
    }

    private void syncAnimationFields() {
        Integer declared = selectedTexture != null ? Config.animatedFrameTicks(selectedTexture) : null;
        this.bannerAnimated = selectedTexture != null && BannerAnimation.isAnimatable(selectedTexture);
        this.bannerFrameTicks = declared != null ? declared : 4;
    }

    @Override
    protected void init() {
        int buttonsY = this.height - 28;

        if (highlightOnly) {
            initHighlightPanel();
            addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.ok"), b -> confirm())
                    .bounds(this.width / 2 - 102, buttonsY, 100, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.cancel"), b -> onClose())
                    .bounds(this.width / 2 + 2, buttonsY, 100, 20).build());
            return;
        }

        this.previewY = this.height - 78;

        switch (target) {
            case BANNER -> initBannerPanel();
            case BOX -> initBoxPanel();
            case TEXT -> initTextPanel();
            case HIGHLIGHT -> initHighlightPanel();
        }

        if (isMainTab) {
            addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.highlightButton"), b -> {
                        target = EditTarget.HIGHLIGHT;
                        rebuildWidgets();
                    })
                    .bounds(this.width - 106, 6, 100, 20).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.ok"), b -> confirm())
                .bounds(this.width / 2 - 102, buttonsY, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.cancel"), b -> onClose())
                .bounds(this.width / 2 + 2, buttonsY, 100, 20).build());
    }

    private void initHighlightPanel() {
        int x = this.width / 2 - 100;
        addRenderableWidget(Checkbox.builder(Component.translatable("createaddonorganizer.colors.highlight.enabled"), this.font)
                .pos(x, 34)
                .selected(hasHighlight)
                .onValueChange((cb, checked) -> {
                    hasHighlight = checked;
                    rebuildWidgets();
                })
                .build());
        if (hasHighlight) {
            addColorControls(x, 64, highlightHsva);
        }
    }

    private void initBannerPanel() {
        int x = this.width / 2 - 100;
        addRenderableWidget(Button.builder(modeLabel(), b -> {
            mode = (mode == Mode.COLOR) ? Mode.IMAGE : Mode.COLOR;
            rebuildWidgets();
        }).bounds(x, 34, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.reset"), b -> resetBanner())
                .bounds(x + 154, 34, 46, 20).build());

        if (mode == Mode.COLOR) {
            addColorControls(x, 60, bannerHsva);
        } else {
            boolean isUpload = selectedRef != null && selectedRef.startsWith("file:");
            addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.banner.upload"),
                    b -> upload()).bounds(x, 60, isUpload ? 130 : 200, 20).build());
            if (isUpload) {
                addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.banner.delete"),
                        b -> confirmDelete()).bounds(x + 134, 60, 66, 20).build());
            }

            boolean canAnimate = selectedTexture != null && BannerAnimation.isAnimatable(selectedTexture);
            if (canAnimate) {
                addRenderableWidget(Checkbox.builder(Component.translatable("createaddonorganizer.banner.animated"), this.font)
                        .pos(x, 84)
                        .selected(bannerAnimated)
                        .onValueChange((cb, checked) -> {
                            bannerAnimated = checked;
                            rebuildWidgets();
                        })
                        .build());
                if (bannerAnimated) {
                    addRenderableWidget(new ChannelSlider(x, 108, 200, 20,
                            Component.translatable("createaddonorganizer.banner.speed"),
                            (bannerFrameTicks - 1) / 9.0,
                            v -> bannerFrameTicks = 1 + (int) Math.round(v * 9),
                            v -> (1 + (int) Math.round(v * 9)) + "t"));
                }
            }

            List<String> pool = BannerPools.poolFor(id);
            boolean hasPool = !pool.isEmpty();
            int checkboxY = canAnimate ? 132 : 86;
            int listTop = checkboxY;
            if (hasPool) {
                addRenderableWidget(Checkbox.builder(Component.translatable("createaddonorganizer.banner.showAll"), this.font)
                        .pos(x, checkboxY)
                        .selected(Config.showAllBanners())
                        .onValueChange((cb, checked) -> {
                            Config.setShowAllBanners(checked);
                            rebuildWidgets();
                        })
                        .build());
                listTop = checkboxY + 24;
            }

            int listBottom = previewY - 22;
            GalleryList gallery = new GalleryList(this.minecraft, this.width, listBottom - listTop, listTop, BannerTextures.HEIGHT + 6);
            boolean restrict = hasPool && !DevMode.isUnlocked() && !Config.showAllBanners();
            List<String> refs;
            if (restrict) {
                refs = new ArrayList<>(pool);
                for (String extra : Config.extraPoolFor(id)) {
                    if (!refs.contains(extra)) {
                        refs.add(extra);
                    }
                }
            } else {
                refs = BannerTextures.gallery();
            }
            for (String ref : refs) {
                gallery.add(ref);
            }
            addRenderableWidget(gallery);
        }
    }

    private void initBoxPanel() {
        int x = this.width / 2 - 100;
        int y = addColorControls(x, 34, boxHsva);
        addRenderableWidget(new ChannelSlider(x, y, 200, 20, Component.translatable("createaddonorganizer.colors.alpha"),
                boxHsva.a, v -> boxHsva.a = (float) v));
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.reset"), b -> {
            Hsva def = Hsva.fromArgb(Config.DEFAULT_BOX_COLOR.get());
            boxHsva.h = def.h;
            boxHsva.s = def.s;
            boxHsva.v = def.v;
            boxHsva.a = def.a;
            rebuildWidgets();
        }).bounds(x, y + 24, 200, 20).build());
        addRenderableWidget(Checkbox.builder(Component.translatable("createaddonorganizer.colors.tintedBox"), this.font)
                .pos(x, y + 48)
                .selected(Config.tintedTextBox())
                .onValueChange((cb, checked) -> Config.setTintedTextBox(checked))
                .build());
    }

    private void initTextPanel() {
        int x = this.width / 2 - 100;
        int panelTop = 34;

        if (twoTone) {
            addRenderableWidget(Button.builder(textTargetLabel(), b -> {
                textEditTarget = (textEditTarget == TextTarget.PRIMARY) ? TextTarget.SECONDARY : TextTarget.PRIMARY;
                rebuildWidgets();
            }).bounds(x, panelTop, 200, 20).build());
            panelTop += 26;
        }

        boolean editingSecondary = twoTone && textEditTarget == TextTarget.SECONDARY;
        Hsva active = editingSecondary ? text2Hsva : textHsva;
        int y = addColorControls(x, panelTop, active);

        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.reset"), b -> {
            Hsva def = Hsva.fromArgb(editingSecondary
                    ? Config.DEFAULT_TEXT_SECONDARY_COLOR.get()
                    : Config.DEFAULT_TEXT_COLOR.get());
            active.h = def.h;
            active.s = def.s;
            active.v = def.v;
            rebuildWidgets();
        }).bounds(x, y, 200, 20).build());

        addRenderableWidget(Checkbox.builder(Component.translatable("createaddonorganizer.colors.twoTone"), this.font)
                .pos(x, y + 24)
                .selected(twoTone)
                .onValueChange((cb, checked) -> {
                    twoTone = checked;
                    textEditTarget = TextTarget.PRIMARY;
                    rebuildWidgets();
                })
                .build());
    }

    private Component textTargetLabel() {
        String key = textEditTarget == TextTarget.PRIMARY
                ? "createaddonorganizer.colors.text.primary"
                : "createaddonorganizer.colors.text.secondary";
        return Component.translatable("createaddonorganizer.colors.text.editing").copy().append(": ").append(Component.translatable(key));
    }

    private int addColorControls(int x, int y, Hsva target) {
        int width = 200;
        int squareHeight = 100;
        int barHeight = 16;
        int gap = 6;

        int barY = y + squareHeight + gap;
        int hexY = barY + barHeight + gap;

        EditBox hexBox = new EditBox(this.font, x, hexY, width - 50, 20, Component.empty());
        hexBox.setMaxLength(7);
        hexBox.setValue(hex6(target));

        boolean[] refreshing = {false};
        hexBox.setResponder(text -> {
            if (!refreshing[0]) {
                applyHex(text, target, null);
            }
        });
        Runnable refreshHexText = () -> {
            refreshing[0] = true;
            hexBox.setValue(hex6(target));
            refreshing[0] = false;
        };

        addRenderableWidget(new SvSquare(x, y, width, squareHeight, target, refreshHexText));
        addRenderableWidget(new HueBar(x, barY, width, barHeight, target, refreshHexText));
        addRenderableWidget(hexBox);
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.copy"),
                        b -> this.minecraft.keyboardHandler.setClipboard(hexBox.getValue()))
                .bounds(x + width - 44, hexY, 44, 20).build());

        return hexY + 20;
    }

    private static String hex6(Hsva hsva) {
        return String.format(Locale.ROOT, "#%06X", ColorUtil.hsvToRgb(hsva.h, hsva.s, hsva.v));
    }

    private static void applyHex(String raw, Hsva target, Runnable afterChange) {
        String s = raw.startsWith("#") ? raw.substring(1) : raw;
        if (s.length() != 6) {
            return;
        }
        try {
            int rgb = Integer.parseInt(s, 16);
            float[] hsv = ColorUtil.rgbToHsv(0xFF000000 | rgb);
            target.h = hsv[0];
            target.s = hsv[1];
            target.v = hsv[2];
            if (afterChange != null) {
                afterChange.run();
            }
        } catch (NumberFormatException ignored) {

        }
    }

    private void resetBanner() {
        Hsva def = Hsva.fromArgb(Config.DEFAULT_BANNER_COLOR.get());
        bannerHsva.h = def.h;
        bannerHsva.s = def.s;
        bannerHsva.v = def.v;
        bannerHsva.a = def.a;
        mode = Mode.COLOR;
        selectedRef = null;
        selectedTexture = null;
        syncAnimationFields();
        rebuildWidgets();
    }

    private Component modeLabel() {
        String key = mode == Mode.COLOR
                ? "createaddonorganizer.banner.mode.color"
                : "createaddonorganizer.banner.mode.image";
        return Component.translatable("createaddonorganizer.banner.mode").copy().append(": ").append(Component.translatable(key));
    }

    private Component currentTitle() {
        String key = switch (target) {
            case BANNER -> "createaddonorganizer.colors.target.banner";
            case BOX -> "createaddonorganizer.colors.target.box";
            case TEXT -> "createaddonorganizer.colors.target.text";
            case HIGHLIGHT -> "createaddonorganizer.colors.target.highlight";
        };
        return this.title.copy().append(" — ").append(Component.translatable(key));
    }

    private void upload() {
        Optional<Path> chosen = BannerTextures.chooseFile();
        if (chosen.isEmpty()) {
            return;
        }
        try {
            String ref = BannerTextures.importFile(chosen.get());
            ResourceLocation tex = BannerTextures.resolve(ref);
            if (tex != null) {
                this.selectedRef = ref;
                this.selectedTexture = tex;
                Config.addExtraPoolEntry(id, ref);
                syncAnimationFields();
                rebuildWidgets();
            }
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to import banner image", e);
        }
    }

    private void confirmDelete() {
        String ref = this.selectedRef;
        ResourceLocation texture = this.selectedTexture;
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                BannerTextures.deleteFile(ref);
                if (texture != null) {
                    Config.clearAnimatedBanner(texture);
                    BannerAnimation.invalidate(texture);
                }
                Config.removeExtraPoolEntriesForRef(ref);
                this.selectedRef = null;
                this.selectedTexture = null;
                syncAnimationFields();
            }
            this.minecraft.setScreen(this);
        }, Component.translatable("createaddonorganizer.banner.delete.title"),
                Component.translatable("createaddonorganizer.banner.delete.message", ref)));
    }

    private void confirm() {
        if (highlightOnly) {
            if (hasHighlight) {
                Config.setHighlightColor(id, highlightHsva.toArgb());
            } else {
                Config.clearHighlightColor(id);
            }
            onClose();
            return;
        }

        if (mode == Mode.COLOR) {
            int argb = bannerHsva.toArgb();
            Config.clearSectionBanner(id);
            Config.setSectionColor(id, argb);
            LiveColors.apply(id, argb);
        } else if (selectedRef != null && selectedTexture != null) {
            Config.setSectionBanner(id, selectedRef);
            LiveColors.applyTexture(id, selectedTexture);
            if (BannerAnimation.isAnimatable(selectedTexture)) {
                if (bannerAnimated) {
                    Config.setAnimatedBanner(selectedTexture, bannerFrameTicks);
                } else {
                    Config.clearAnimatedBanner(selectedTexture);
                }
                BannerAnimation.invalidate(selectedTexture);
            }
        }

        int textArgb = textHsva.toArgb();
        Config.setTextColor(id, textArgb);
        LiveColors.applyTextColor(id, textArgb);

        Config.setBoxColor(id, boxHsva.toArgb());

        if (twoTone) {
            Config.setTextSecondaryColor(id, text2Hsva.toArgb());
        } else {
            Config.clearTextSecondaryColor(id);
        }

        if (isMainTab) {
            if (hasHighlight) {
                Config.setHighlightColor(id, highlightHsva.toArgb());
            } else {
                Config.clearHighlightColor(id);
            }
        }

        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (!highlightOnly && button == 0) {
            EditTarget hit = hitTest((int) mouseX, (int) mouseY);
            if (hit != null && !Config.editorHintSeen()) {

                Config.setEditorHintSeen(true);
            }
            if (hit != null && hit != target) {
                target = hit;
                rebuildWidgets();
                return true;
            }
        }
        return false;
    }

    private EditTarget hitTest(int mx, int my) {
        int bx = this.width / 2 - BannerTextures.WIDTH / 2;
        int by = previewY;
        boolean hasBanner = mode == Mode.COLOR || selectedTexture != null;
        if (hasBanner) {
            int textX = bx + 4;
            int textY = by + (BannerTextures.HEIGHT - 1 - 9) / 2 + 1;
            int w = this.font.width(this.sectionName);
            if (within(mx, my, textX, textY, textX + w, textY + 9)) {
                return EditTarget.TEXT;
            }
            if (within(mx, my, textX - 4, textY - 3, textX + w + 3, textY + 9 + 2)) {
                return EditTarget.BOX;
            }
        }
        if (within(mx, my, bx, by, bx + BannerTextures.WIDTH, by + BannerTextures.HEIGHT - 1)) {
            return EditTarget.BANNER;
        }
        return null;
    }

    private static boolean within(int x, int y, int x1, int y1, int x2, int y2) {
        return x >= x1 && x < x2 && y >= y1 && y < y2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, currentTitle(), this.width / 2, 16, 0xFFFFFFFF);

        if (highlightOnly) {
            return;
        }

        int bx = this.width / 2 - BannerTextures.WIDTH / 2;
        int by = previewY;
        g.fill(bx - 6, by - 6, bx + BannerTextures.WIDTH + 6, by + BannerTextures.HEIGHT + 6, 0xD0202020);

        boolean hasBanner = mode == Mode.COLOR || selectedTexture != null;
        if (mode == Mode.COLOR) {
            g.fill(bx, by, bx + BannerTextures.WIDTH, by + BannerTextures.HEIGHT - 1, bannerHsva.toArgb());
        } else if (selectedTexture != null) {
            float v = 0.0F;
            int texHeight = BannerTextures.HEIGHT;
            var anim = BannerAnimation.preview(selectedTexture, bannerAnimated, bannerFrameTicks);
            if (anim.isPresent()) {
                boolean hovered = BannerAnimation.isHovering(bx, by, BannerTextures.WIDTH, BannerTextures.HEIGHT - 1, mouseX, mouseY);
                int frame = BannerAnimation.currentFrame(selectedTexture, anim.get(), hovered);
                v = frame * BannerTextures.HEIGHT;
                texHeight = anim.get().frameCount() * BannerTextures.HEIGHT;
            }

            g.blit(selectedTexture, bx, by, 0.0F, v,
                    BannerTextures.WIDTH, BannerTextures.HEIGHT - 1, BannerTextures.WIDTH, texHeight);
        } else {
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.banner.none"),
                    this.width / 2, by + (BannerTextures.HEIGHT - 8) / 2, 0xFF888888);
        }

        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.colors.clickHint"),
                this.width / 2, by - 18, 0xFFAAAAAA);

        EditTarget hovered = hitTest(mouseX, mouseY);

        boolean pulsing = !Config.editorHintSeen();

        if (hasBanner) {
            int textX = bx + 4;
            int textY = by + (BannerTextures.HEIGHT - 1 - 9) / 2 + 1;
            int w = this.font.width(this.sectionName);
            if (Config.tintedTextBox()) {
                g.fill(textX - 4, textY - 3, textX + w + 3, textY + 9 + 2, boxHsva.toArgb());
            }
            if (twoTone) {
                TwoToneText.draw(g, this.font, this.sectionName, textX, textY, textHsva.toArgb(), text2Hsva.toArgb());
            } else {
                g.drawString(this.font, this.sectionName, textX, textY, textHsva.toArgb(), true);
            }
            if (pulsing) {
                outline(g, textX - 4, textY - 3, textX + w + 3, textY + 9 + 2, pulseColor(0.12f));
                outline(g, textX, textY, textX + w, textY + 9, pulseColor(0.24f));
            }
            if (hovered == EditTarget.TEXT) {
                outline(g, textX, textY, textX + w, textY + 9);
            } else if (hovered == EditTarget.BOX) {
                outline(g, textX - 4, textY - 3, textX + w + 3, textY + 9 + 2);
            }
        }
        if (pulsing) {
            outline(g, bx, by, bx + BannerTextures.WIDTH, by + BannerTextures.HEIGHT - 1, pulseColor(0f));
        }
        if (hovered == EditTarget.BANNER) {
            outline(g, bx, by, bx + BannerTextures.WIDTH, by + BannerTextures.HEIGHT - 1);
        }

        if (mode == Mode.COLOR) {
            g.drawCenteredString(this.font, Config.formatHex(bannerHsva.toArgb()), this.width / 2, by + BannerTextures.HEIGHT + 10, 0xFFAAAAAA);
        }
    }

    private static final long PULSE_MS = 1700;

    private static int pulseColor(float phaseOffset) {
        float phase = (System.currentTimeMillis() % PULSE_MS) / (float) PULSE_MS + phaseOffset;
        float wave = (Mth.sin(phase * Mth.TWO_PI) + 1f) / 2f;
        int a = 0x30 + Math.round((0xB0 - 0x30) * wave);
        return (a << 24) | 0x00FFFFFF;
    }

    private static void outline(GuiGraphics g, int x1, int y1, int x2, int y2) {
        outline(g, x1, y1, x2, y2, 0xFFFFFFFF);
    }

    private static void outline(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }

    private static void drawMarker(GuiGraphics g, int cx, int cy) {
        outline(g, cx - 5, cy - 5, cx + 5, cy + 5, 0xFF000000);
        outline(g, cx - 4, cy - 4, cx + 4, cy + 4, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private static final class Hsva {
        float h;
        float s;
        float v;
        float a = 1f;

        int toArgb() {
            return (Math.round(a * 255) << 24) | ColorUtil.hsvToRgb(h, s, v);
        }

        static Hsva fromArgb(int argb) {
            Hsva hsva = new Hsva();
            float[] hsv = ColorUtil.rgbToHsv(argb);
            hsva.h = hsv[0];
            hsva.s = hsv[1];
            hsva.v = hsv[2];
            hsva.a = ((argb >>> 24) & 0xFF) / 255f;
            return hsva;
        }
    }

    private static class ChannelSlider extends AbstractSliderButton {
        private final Component label;
        private final DoubleConsumer onChange;
        private final DoubleFunction<String> formatter;

        ChannelSlider(int x, int y, int w, int h, Component label, double initial, DoubleConsumer onChange) {
            this(x, y, w, h, label, initial, onChange, null);
        }

        ChannelSlider(int x, int y, int w, int h, Component label, double initial, DoubleConsumer onChange,
                DoubleFunction<String> formatter) {
            super(x, y, w, h, Component.empty(), initial);
            this.label = label;
            this.onChange = onChange;
            this.formatter = formatter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            String suffix = formatter != null ? formatter.apply(this.value) : Math.round(this.value * 100) + "%";
            setMessage(label.copy().append(": " + suffix));
        }

        @Override
        protected void applyValue() {
            onChange.accept(this.value);
        }
    }

    private static final int GRADIENT_CELL = 5;

    private static class SvSquare extends AbstractWidget {
        private final Hsva target;
        private final Runnable afterChange;

        SvSquare(int x, int y, int w, int h, Hsva target, Runnable afterChange) {
            super(x, y, w, h, Component.empty());
            this.target = target;
            this.afterChange = afterChange;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            update(mouseX, mouseY);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            update(mouseX, mouseY);
        }

        private void update(double mouseX, double mouseY) {
            target.s = Mth.clamp((float) ((mouseX - getX()) / getWidth()), 0f, 1f);
            target.v = Mth.clamp((float) (1 - (mouseY - getY()) / getHeight()), 0f, 1f);
            afterChange.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x0 = getX();
            int y0 = getY();
            int w = getWidth();
            int h = getHeight();
            for (int cy = 0; cy < h; cy += GRADIENT_CELL) {
                int ch = Math.min(GRADIENT_CELL, h - cy);
                float v = 1f - (cy + ch / 2f) / h;
                for (int cx = 0; cx < w; cx += GRADIENT_CELL) {
                    int cw = Math.min(GRADIENT_CELL, w - cx);
                    float s = (cx + cw / 2f) / w;
                    g.fill(x0 + cx, y0 + cy, x0 + cx + cw, y0 + cy + ch, 0xFF000000 | ColorUtil.hsvToRgb(target.h, s, v));
                }
            }

            int mx = x0 + Math.round(target.s * w);
            int my = y0 + Math.round((1 - target.v) * h);
            drawMarker(g, mx, my);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {}
    }

    private static class HueBar extends ChannelSlider {
        private final Hsva target;

        HueBar(int x, int y, int w, int h, Hsva target, Runnable afterChange) {
            super(x, y, w, h, Component.translatable("createaddonorganizer.colors.hue"), target.h,
                    v -> {
                        target.h = (float) v;
                        afterChange.run();
                    });
            this.target = target;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x0 = getX();
            int y0 = getY();
            int w = getWidth();
            int h = getHeight();
            for (int cx = 0; cx < w; cx += GRADIENT_CELL) {
                int cw = Math.min(GRADIENT_CELL, w - cx);
                float hue = (cx + cw / 2f) / w;
                g.fill(x0 + cx, y0, x0 + cx + cw, y0 + h, 0xFF000000 | ColorUtil.hsvToRgb(hue, 1f, 1f));
            }

            int mx = x0 + Math.round(target.h * w);
            drawMarker(g, mx, y0 + h / 2);
        }
    }

    private class GalleryList extends ContainerObjectSelectionList<GalleryList.Row> {
        GalleryList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        void add(String ref) {
            addEntry(new Row(ref));
        }

        @Override
        public int getRowWidth() {
            return BannerTextures.WIDTH + 12;
        }

        private class Row extends ContainerObjectSelectionList.Entry<Row> {
            private final String ref;
            private final ResourceLocation texture;

            Row(String ref) {
                this.ref = ref;
                this.texture = BannerTextures.resolve(ref);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of();
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (button == 0 && texture != null) {
                    selectedTexture = texture;
                    selectedRef = ref;
                    syncAnimationFields();
                    rebuildWidgets();
                    return true;
                }
                return false;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, boolean hovered, float partialTick) {
                int bx = left + (rowWidth - BannerTextures.WIDTH) / 2;
                int by = top + (rowHeight - BannerTextures.HEIGHT) / 2;
                boolean selected = ref.equals(selectedRef);
                int border = selected ? 0xFFFFFFFF : (hovered ? 0xFF808080 : 0xFF000000);
                g.fill(bx - 1, by - 1, bx + BannerTextures.WIDTH + 1, by + BannerTextures.HEIGHT + 1, border);
                if (texture != null) {

                    int texHeight = BannerAnimation.preview(texture, false, 1)
                            .map(BannerAnimation.AnimInfo::frameCount).orElse(1) * BannerTextures.HEIGHT;
                    g.blit(texture, bx, by, 0.0F, 0.0F, BannerTextures.WIDTH, BannerTextures.HEIGHT,
                            BannerTextures.WIDTH, texHeight);
                }
            }
        }
    }
}
