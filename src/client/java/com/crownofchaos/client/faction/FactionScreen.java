package com.crownofchaos.client.faction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.crownofchaos.faction.Faction;
import com.crownofchaos.faction.FactionManager;
import com.crownofchaos.faction.FactionStance;
import com.crownofchaos.network.FactionActionPayload;
import com.crownofchaos.network.FactionActionPayload.Action;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.DyeColor;

/**
 * The faction menu. Players without a faction can found one or join one; members see their
 * faction, its members, its stance toward other factions, and every faction. Owners get the
 * controls for managing it.
 *
 * <p>The menu never changes faction data itself: buttons send a {@link FactionActionPayload},
 * the server checks it and replies in chat, and the updated factions it sends back call
 * {@link #refresh()}. The UI mirrors the server's rules only to grey out useless buttons.
 *
 * <p>Everything is drawn with flat fills and outlines (no textures), in the Crown of Darkness's teal and cyan.
 */
public class FactionScreen extends Screen {
	// Colors (ARGB).
	static final int PANEL_OUTER = 0xF0031F28;
	static final int PANEL_INNER = 0xF005404F;
	static final int ACCENT = 0xFF35E9F5;
	static final int ACCENT_DARK = 0xFF079093;
	static final int TEXT = 0xFFDFFBFD;
	static final int TEXT_DIM = 0xFF8FCBD1;
	static final int GREEN = 0xFF6FD66F;
	static final int RED = 0xFFE05A4F;
	static final int GRAY = 0xFFAAAAAA;

	// Layout.
	private static final int PANEL_WIDTH = 320;
	private static final int PANEL_HEIGHT = 230;
	private static final int MARGIN = 12;
	private static final int TAB_Y = 18;
	private static final int TAB_HEIGHT = 16;
	private static final int CONTENT_Y = 42;
	private static final int ROW_BUTTON_HEIGHT = 14;
	/** Longest Minecraft player name, for the invite box. */
	private static final int MAX_PLAYER_NAME = 16;

	private static final String KEY = "gui.crownofchaos.faction.";

	private enum Tab {
		CREATE("create", false),
		JOIN("join", false),
		OVERVIEW("overview", true),
		MEMBERS("members", true),
		RELATIONS("relations", true),
		WAR("war", true),
		FACTIONS("factions", true);

		final String key;
		final boolean member;

		Tab(String key, boolean member) {
			this.key = key;
			this.member = member;
		}
	}

	/** A button at the right end of a list row. */
	private record RowButton(Component label, int width, int color, boolean active, Runnable onPress) {
	}

	/** A line in a paged list: a gold section header, or an entry with optional detail and buttons. */
	private record Row(Component text, Component detail, int detailColor, boolean header, List<RowButton> buttons) {
		static Row header(Component text) {
			return new Row(text, null, 0, true, List.of());
		}

		static Row entry(Component text, Component detail, int detailColor, List<RowButton> buttons) {
			return new Row(text, detail, detailColor, false, buttons);
		}
	}

	private record PlacedRow(Row row, int y) {
	}

	// State kept across rebuilds and refreshes.
	private Tab tab;
	private final Map<Tab, Integer> pages = new EnumMap<>(Tab.class);
	private String createName = "";
	private String renameText = "";
	private String inviteText = "";
	private String warText = "";
	private DyeColor selectedColor;
	private String pendingConfirm;

	// Rebuilt by init().
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;
	private EditBox nameBox;
	private EditBox renameBox;
	private EditBox inviteBox;
	private EditBox warBox;
	private FactionButton createButton;
	private FactionButton renameButton;
	private FactionButton inviteButton;
	private FactionButton warButton;
	private final List<PlacedRow> placedRows = new ArrayList<>();
	private int rowHeight;
	private boolean twoLineRows;
	private int listTop;
	private int pageCount;
	private Component emptyText;
	private List<FormattedCharSequence> infoLines = List.of();

	private boolean confirmClicked;
	private boolean needsRebuild;

	public FactionScreen() {
		super(Component.translatable(KEY + "title"));
	}

	/** Called when new faction data arrives while the menu is open. */
	public void refresh() {
		this.rebuildKeepingFocus();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---------------------------------------------------------------- data helpers

	private UUID me() {
		return this.minecraft.player == null ? new UUID(0, 0) : this.minecraft.player.getUUID();
	}

	private Optional<Faction> mine() {
		return FactionClientState.mine();
	}

	private static Component tr(String key, Object... args) {
		return Component.translatable(KEY + key, args);
	}

	private static int codePoints(String s) {
		return s.codePointCount(0, s.length());
	}

	/** The faction (other than {@code except}) that already uses this name, ignoring case. */
	private static boolean nameTaken(String name, Faction except) {
		String lower = name.toLowerCase(Locale.ROOT);
		return FactionClientState.all().stream()
				.anyMatch(f -> f != except && f.name().trim().toLowerCase(Locale.ROOT).equals(lower));
	}

	/** The faction (other than {@code except}) using this color, or null. */
	private static Faction colorOwner(DyeColor color, Faction except) {
		return FactionClientState.all().stream()
				.filter(f -> f != except && f.color() == color)
				.findFirst().orElse(null);
	}

	private static Component stanceName(FactionStance stance) {
		return tr("stance." + stance.getSerializedName());
	}

	private static int stanceColor(FactionStance stance) {
		return switch (stance) {
			case ALLOWED -> GREEN;
			case NEUTRAL -> GRAY;
			case TARGETED -> RED;
		};
	}

	private static Component onOff(boolean on) {
		return tr(on ? "on" : "off").copy().withColor(on ? RED : GRAY);
	}

	private static void send(FactionActionPayload payload) {
		ClientPlayNetworking.send(payload);
	}

	// ---------------------------------------------------------------- building

	@Override
	protected void init() {
		this.panelWidth = Math.min(PANEL_WIDTH, this.width - 8);
		this.panelHeight = Math.min(PANEL_HEIGHT, this.height - 8);
		this.left = (this.width - this.panelWidth) / 2;
		this.top = (this.height - this.panelHeight) / 2;
		this.nameBox = null;
		this.renameBox = null;
		this.inviteBox = null;
		this.warBox = null;
		this.createButton = null;
		this.renameButton = null;
		this.inviteButton = null;
		this.warButton = null;
		this.placedRows.clear();
		this.pageCount = 1;
		this.emptyText = null;
		this.infoLines = List.of();

		Optional<Faction> mine = this.mine();
		boolean member = mine.isPresent();
		if (this.tab == null || this.tab.member != member) {
			this.tab = member ? Tab.OVERVIEW : Tab.CREATE;
			this.pendingConfirm = null;
		}

		this.addTabs(member);
		this.addRenderableWidget(new FactionButton(this.left + this.panelWidth - 15, this.top + 4, 11, 11,
				tr("close"), TEXT, this::onClose));

		switch (this.tab) {
			case CREATE -> this.initCreate();
			case JOIN -> this.initJoin();
			case OVERVIEW -> this.initOverview(mine.get());
			case MEMBERS -> this.initMembers(mine.get());
			case RELATIONS -> this.initRelations(mine.get());
			case WAR -> this.initWar(mine.get());
			case FACTIONS -> this.initFactions();
		}
	}

	private void addTabs(boolean member) {
		List<Tab> tabs = new ArrayList<>();
		for (Tab t : Tab.values()) {
			if (t.member == member) {
				tabs.add(t);
			}
		}
		int x0 = this.left + 8;
		int total = this.panelWidth - 16;
		int gap = 3;
		int w = (total - gap * (tabs.size() - 1)) / tabs.size();
		for (int i = 0; i < tabs.size(); i++) {
			Tab t = tabs.get(i);
			this.addRenderableWidget(new FactionButton(x0 + i * (w + gap), this.top + TAB_Y, w, TAB_HEIGHT,
					tr("tab." + t.key), TEXT, () -> this.switchTab(t)).setSelected(t == this.tab));
		}
	}

	private void switchTab(Tab t) {
		this.tab = t;
		this.pendingConfirm = null;
		this.needsRebuild = true;
	}

	private int contentLeft() {
		return this.left + MARGIN;
	}

	private int contentRight() {
		return this.left + this.panelWidth - MARGIN;
	}

	private int contentTop() {
		return this.top + CONTENT_Y;
	}

	private int centerX() {
		return this.left + this.panelWidth / 2;
	}

	/** A button that has to be clicked twice; the first click changes its text to a confirm prompt. */
	private FactionButton confirmButton(String key, int x, int y, int w, int h, Component label, Component confirmLabel,
			int color, Runnable action) {
		boolean pending = key.equals(this.pendingConfirm);
		return new FactionButton(x, y, w, h, pending ? confirmLabel : label, pending ? RED : color, () -> {
			this.confirmClicked = true;
			if (key.equals(this.pendingConfirm)) {
				this.pendingConfirm = null;
				action.run();
			} else {
				this.pendingConfirm = key;
			}
			this.needsRebuild = true;
		});
	}

	private RowButton confirmRowButton(String key, Component label, int width, int color, Runnable action) {
		boolean pending = key.equals(this.pendingConfirm);
		return new RowButton(pending ? tr("confirm_short") : label, width, pending ? RED : color, true, () -> {
			this.confirmClicked = true;
			if (key.equals(this.pendingConfirm)) {
				this.pendingConfirm = null;
				action.run();
			} else {
				this.pendingConfirm = key;
			}
			this.needsRebuild = true;
		});
	}

	/** Adds 16 color swatches, in rows of {@code perRow}, starting at (x, y). */
	private void addSwatches(int x, int y, int size, int step, int perRow, DyeColor selected, Faction except,
			java.util.function.Consumer<DyeColor> onPick) {
		DyeColor[] colors = DyeColor.values();
		for (int i = 0; i < colors.length; i++) {
			DyeColor color = colors[i];
			Faction owner = colorOwner(color, except);
			int sx = x + (i % perRow) * step;
			int sy = y + (i / perRow) * step;
			this.addRenderableWidget(new ColorSwatch(sx, sy, size, color, color == selected,
					owner == null ? null : owner.displayName(), () -> onPick.accept(color)));
		}
	}

	// ---- Create

	private void initCreate() {
		int ct = this.contentTop();
		int cx = this.centerX();
		int boxWidth = 180;
		this.nameBox = new EditBox(this.font, cx - boxWidth / 2, ct + 10, boxWidth, 18, tr("create.name"));
		this.nameBox.setMaxLength(FactionManager.MAX_NAME_LENGTH * 2);
		this.nameBox.setHint(tr("create.name_hint"));
		this.nameBox.setValue(this.createName);
		this.nameBox.setResponder(value -> {
			String limited = limitCodePoints(value);
			if (!limited.equals(value)) {
				this.nameBox.setValue(limited);
				return;
			}
			this.createName = value;
			this.updateButtons();
		});
		this.addRenderableWidget(this.nameBox);

		int size = 16;
		int step = 19;
		int gridWidth = 8 * step - (step - size);
		this.addSwatches(cx - gridWidth / 2, ct + 62, size, step, 8, this.selectedColor, null, color -> {
			this.selectedColor = color;
			this.needsRebuild = true;
		});

		this.createButton = this.addRenderableWidget(new FactionButton(cx - 60, this.top + this.panelHeight - 30, 120, 20,
				tr("create.button"), ACCENT, () -> {
					if (this.createProblem() == null) {
						send(new FactionActionPayload(Action.CREATE, this.createName.trim(), Optional.empty(), this.selectedColor.getId()));
					}
				}));
		this.updateButtons();
	}

	private static String limitCodePoints(String value) {
		if (codePoints(value) <= FactionManager.MAX_NAME_LENGTH) {
			return value;
		}
		return value.substring(0, value.offsetByCodePoints(0, FactionManager.MAX_NAME_LENGTH));
	}

	/** Why a faction name can't be used, or null if it can. */
	private Component nameProblem(String rawName, Faction except) {
		String name = rawName.trim();
		if (name.isEmpty()) {
			return tr("status.empty");
		}
		if (codePoints(name) > FactionManager.MAX_NAME_LENGTH) {
			return tr("status.too_long", FactionManager.MAX_NAME_LENGTH);
		}
		if (nameTaken(name, except)) {
			return tr("status.name_taken");
		}
		return null;
	}

	/** Why the Create button is off, or null if it's ready. */
	private Component createProblem() {
		Component problem = this.nameProblem(this.createName, null);
		if (problem != null) {
			return problem;
		}
		if (this.selectedColor == null) {
			return tr("status.pick_color");
		}
		if (colorOwner(this.selectedColor, null) != null) {
			return tr("status.color_taken");
		}
		return null;
	}

	private void updateButtons() {
		if (this.createButton != null) {
			this.createButton.setActive(this.createProblem() == null);
		}
		if (this.renameButton != null) {
			Faction faction = this.mine().orElse(null);
			this.renameButton.setActive(faction != null && this.nameProblem(this.renameText, faction) == null
					&& !this.renameText.trim().equals(faction.name()));
		}
		if (this.inviteButton != null) {
			String name = this.inviteText.trim();
			this.inviteButton.setActive(!name.isEmpty() && name.length() <= MAX_PLAYER_NAME);
		}
		if (this.warButton != null) {
			String name = this.warText.trim();
			this.warButton.setActive(!name.isEmpty() && name.length() <= MAX_PLAYER_NAME);
		}
	}

	// ---- Join

	private void initJoin() {
		UUID me = this.me();
		List<Faction> all = FactionClientState.all();
		List<Row> rows = new ArrayList<>();

		List<Faction> invitedBy = all.stream().filter(f -> f.invites().containsKey(me)).toList();
		if (!invitedBy.isEmpty()) {
			rows.add(Row.header(tr("join.invites")));
			for (Faction f : invitedBy) {
				rows.add(Row.entry(f.displayName(), tr("join.invited_you"), TEXT_DIM, List.of(
						new RowButton(tr("join.accept"), 48, GREEN, true, () -> send(FactionActionPayload.withTarget(Action.ACCEPT_INVITE, f.id()))),
						new RowButton(tr("join.decline"), 48, RED, true, () -> send(FactionActionPayload.withTarget(Action.DECLINE_INVITE, f.id()))))));
			}
		}
		if (!all.isEmpty()) {
			rows.add(Row.header(tr("join.all")));
			for (Faction f : all) {
				Component detail = tr("member_count", f.members().size());
				List<RowButton> buttons;
				if (f.invites().containsKey(me)) {
					buttons = List.of();
					detail = tr("join.invited_you");
				} else if (f.requests().containsKey(me)) {
					buttons = List.of(new RowButton(tr("join.cancel_request"), 84, RED, true,
							() -> send(FactionActionPayload.withTarget(Action.CANCEL_REQUEST, f.id()))));
				} else {
					buttons = List.of(new RowButton(tr("join.ask"), 84, GREEN, true,
							() -> send(FactionActionPayload.withTarget(Action.REQUEST_JOIN, f.id()))));
				}
				rows.add(Row.entry(f.displayName(), detail, TEXT_DIM, buttons));
			}
		}
		this.layoutRows(rows, this.contentTop(), 17, false, tr("join.empty"));
	}

	// ---- Overview

	private void initOverview(Faction faction) {
		int ct = this.contentTop();
		int x0 = this.contentLeft();
		int x1 = this.contentRight();
		int width = x1 - x0;
		if (!faction.isOwner(this.me())) {
			this.addRenderableWidget(this.confirmButton("leave", this.centerX() - 70, ct + 62, 140, 20,
					tr("overview.leave"), tr("confirm"), RED, () -> send(FactionActionPayload.of(Action.LEAVE))));
			return;
		}

		int labelWidth = 44;
		int buttonWidth = 70;
		this.renameBox = new EditBox(this.font, x0 + labelWidth, ct + 52, width - labelWidth - buttonWidth - 4, 16, tr("overview.rename_label"));
		this.renameBox.setMaxLength(FactionManager.MAX_NAME_LENGTH * 2);
		this.renameBox.setHint(Component.literal(faction.name()).withColor(0xFF6A6A6A));
		this.renameBox.setValue(this.renameText);
		this.renameBox.setResponder(value -> {
			String limited = limitCodePoints(value);
			if (!limited.equals(value)) {
				this.renameBox.setValue(limited);
				return;
			}
			this.renameText = value;
			this.updateButtons();
		});
		this.addRenderableWidget(this.renameBox);
		this.renameButton = this.addRenderableWidget(new FactionButton(x1 - buttonWidth, ct + 52, buttonWidth, 16,
				tr("overview.rename"), TEXT, () -> send(FactionActionPayload.withText(Action.RENAME, this.renameText.trim()))));

		int size = 13;
		int step = Math.min(15, (width - labelWidth + 2) / 16);
		this.addSwatches(x0 + labelWidth, ct + 74, Math.min(size, step - 1), step, 16, faction.color(), faction,
				color -> send(new FactionActionPayload(Action.SET_COLOR, "", Optional.empty(), color.getId())));

		int half = (width - 8) / 2;
		boolean alert = faction.redAlert();
		this.addRenderableWidget(new FactionButton(x0, ct + 96, half, 20,
				tr("overview.red_alert_button", onOff(alert)), TEXT,
				() -> send(new FactionActionPayload(Action.SET_RED_ALERT, "", Optional.empty(), alert ? 0 : 1))));
		this.addRenderableWidget(this.confirmButton("disband", x1 - half, ct + 96, half, 20,
				tr("overview.disband"), tr("confirm"), RED, () -> send(FactionActionPayload.of(Action.DISBAND))));
		this.infoLines = this.font.split(tr("overview.red_alert_info"), width);
		this.updateButtons();
	}

	// ---- Members

	private void initMembers(Faction faction) {
		UUID me = this.me();
		boolean owner = faction.isOwner(me);
		int ct = this.contentTop();
		int x0 = this.contentLeft();
		int listStart = ct;
		if (owner) {
			int buttonWidth = 60;
			this.inviteBox = new EditBox(this.font, x0, ct, 150, 16, tr("members.invite_hint"));
			this.inviteBox.setMaxLength(MAX_PLAYER_NAME);
			this.inviteBox.setHint(tr("members.invite_hint"));
			this.inviteBox.setValue(this.inviteText);
			this.inviteBox.setResponder(value -> {
				this.inviteText = value;
				this.updateButtons();
			});
			this.addRenderableWidget(this.inviteBox);
			this.inviteButton = this.addRenderableWidget(new FactionButton(x0 + 154, ct, buttonWidth, 16,
					tr("members.invite"), GREEN, this::sendInvite));
			listStart = ct + 22;
		}

		List<Row> rows = new ArrayList<>();
		rows.add(Row.header(tr("members.header", faction.members().size())));
		for (Map.Entry<UUID, String> member : faction.members().entrySet()) {
			UUID id = member.getKey();
			boolean isOwner = faction.isOwner(id);
			Component detail = isOwner ? tr("members.owner") : id.equals(me) ? tr("members.you") : null;
			List<RowButton> buttons = List.of();
			if (owner && !isOwner) {
				buttons = List.of(
						this.confirmRowButton("transfer:" + id, tr("members.make_owner"), 70, ACCENT,
								() -> send(FactionActionPayload.withTarget(Action.TRANSFER, id))),
						new RowButton(tr("members.kick"), 40, RED, true,
								() -> send(FactionActionPayload.withTarget(Action.KICK, id))));
			}
			rows.add(Row.entry(Component.literal(member.getValue()), detail, isOwner ? ACCENT : TEXT_DIM, buttons));
		}
		if (owner) {
			if (!faction.invites().isEmpty()) {
				rows.add(Row.header(tr("members.pending")));
				for (Map.Entry<UUID, String> invite : faction.invites().entrySet()) {
					UUID id = invite.getKey();
					rows.add(Row.entry(Component.literal(invite.getValue()), null, 0, List.of(
							new RowButton(tr("members.cancel"), 50, RED, true,
									() -> send(FactionActionPayload.withTarget(Action.CANCEL_INVITE, id))))));
				}
			}
			if (!faction.requests().isEmpty()) {
				rows.add(Row.header(tr("members.requests")));
				for (Map.Entry<UUID, String> request : faction.requests().entrySet()) {
					UUID id = request.getKey();
					rows.add(Row.entry(Component.literal(request.getValue()), null, 0, List.of(
							new RowButton(tr("join.accept"), 48, GREEN, true,
									() -> send(FactionActionPayload.withTarget(Action.ACCEPT_REQUEST, id))),
							new RowButton(tr("join.decline"), 48, RED, true,
									() -> send(FactionActionPayload.withTarget(Action.DECLINE_REQUEST, id))))));
				}
			}
		}
		this.layoutRows(rows, listStart, 16, false, null);
		this.updateButtons();
	}

	private void sendInvite() {
		String name = this.inviteText.trim();
		if (!name.isEmpty()) {
			send(FactionActionPayload.withText(Action.INVITE, name));
			this.inviteText = "";
			this.needsRebuild = true;
		}
	}

	// ---- Relations

	private void initRelations(Faction faction) {
		boolean owner = faction.isOwner(this.me());
		this.infoLines = this.font.split(tr("relations.info"), this.contentRight() - this.contentLeft());
		int listStart = this.contentTop() + this.infoLines.size() * 10 + 4;

		List<Row> rows = new ArrayList<>();
		for (Faction other : FactionClientState.all()) {
			if (other.id().equals(faction.id())) {
				continue;
			}
			FactionStance stance = faction.stanceToward(other.id());
			FactionStance theirs = other.stanceToward(faction.id());
			Component theirsText = stanceName(theirs).copy().withColor(stanceColor(theirs));
			Component name = faction.warFactions().contains(other.id())
					? other.displayName().append(Component.literal(" ")).append(tr("war.at_war").copy().withColor(RED))
					: other.displayName();
			if (owner) {
				FactionStance next = stance.next();
				RowButton button = new RowButton(stanceName(stance), 70, stanceColor(stance), true,
						() -> send(new FactionActionPayload(Action.SET_STANCE, "", Optional.of(other.id()), next.ordinal())));
				rows.add(Row.entry(name, tr("relations.their_stance", theirsText), TEXT_DIM, List.of(button)));
			} else {
				Component oursText = stanceName(stance).copy().withColor(stanceColor(stance));
				rows.add(Row.entry(name, tr("relations.both_stances", oursText, theirsText), TEXT_DIM, List.of()));
			}
		}
		this.layoutRows(rows, listStart, 22, true, tr("relations.empty"));
	}

	// ---- War

	private void initWar(Faction faction) {
		boolean owner = faction.isOwner(this.me());
		int x0 = this.contentLeft();
		int width = this.contentRight() - x0;
		this.infoLines = this.font.split(tr("war.info"), width);
		int listStart = this.contentTop() + this.infoLines.size() * 10 + 4;
		if (owner) {
			int buttonWidth = 80;
			this.warBox = new EditBox(this.font, x0, listStart, width - buttonWidth - 4, 16, tr("war.player_hint"));
			this.warBox.setMaxLength(MAX_PLAYER_NAME);
			this.warBox.setHint(tr("war.player_hint"));
			this.warBox.setValue(this.warText);
			this.warBox.setResponder(value -> {
				this.warText = value;
				this.updateButtons();
			});
			this.addRenderableWidget(this.warBox);
			this.warButton = this.addRenderableWidget(new FactionButton(x0 + width - buttonWidth, listStart, buttonWidth, 16,
					tr("war.declare"), RED, this::sendWarOnPlayer));
			listStart += 20;
		}

		List<Row> rows = new ArrayList<>();
		rows.add(Row.header(tr("war.factions")));
		boolean anyFaction = false;
		for (Faction other : FactionClientState.all()) {
			if (other.id().equals(faction.id())) {
				continue;
			}
			anyFaction = true;
			boolean ours = faction.warFactions().contains(other.id());
			boolean theirs = other.warFactions().contains(faction.id());
			Component name = ours
					? other.displayName().append(Component.literal(" ")).append(tr("war.at_war").copy().withColor(RED))
					: other.displayName();
			Component detail = theirs ? tr("war.at_war_with_you") : null;
			List<RowButton> buttons = List.of();
			if (owner) {
				buttons = List.of(ours
						? new RowButton(tr("war.make_peace"), 80, GREEN, true,
								() -> send(FactionActionPayload.withTarget(Action.END_WAR, other.id())))
						: this.confirmRowButton("war:" + other.id(), tr("war.declare"), 80, RED,
								() -> send(FactionActionPayload.withTarget(Action.DECLARE_WAR, other.id()))));
			}
			rows.add(Row.entry(name, detail, TEXT_DIM, buttons));
		}
		if (!anyFaction) {
			rows.add(Row.entry(tr("war.no_factions").copy().withColor(TEXT_DIM), null, 0, List.of()));
		}

		rows.add(Row.header(tr("war.players")));
		if (faction.warPlayers().isEmpty()) {
			rows.add(Row.entry(tr("war.no_players").copy().withColor(TEXT_DIM), null, 0, List.of()));
		}
		for (Map.Entry<UUID, String> enemy : faction.warPlayers().entrySet()) {
			UUID id = enemy.getKey();
			List<RowButton> buttons = owner
					? List.of(new RowButton(tr("war.make_peace"), 80, GREEN, true,
							() -> send(FactionActionPayload.withTarget(Action.END_WAR_ON_PLAYER, id))))
					: List.of();
			rows.add(Row.entry(Component.literal(enemy.getValue()).withColor(RED), null, 0, buttons));
		}
		this.layoutRows(rows, listStart, 16, false, null);
		this.updateButtons();
	}

	private void sendWarOnPlayer() {
		String name = this.warText.trim();
		if (!name.isEmpty() && name.length() <= MAX_PLAYER_NAME) {
			send(FactionActionPayload.withText(Action.DECLARE_WAR_ON_PLAYER, name));
			this.warText = "";
			this.needsRebuild = true;
		}
	}

	// ---- Factions

	private void initFactions() {
		UUID myFaction = this.mine().map(Faction::id).orElse(null);
		List<Row> rows = new ArrayList<>();
		for (Faction f : FactionClientState.all()) {
			String ownerName = f.members().getOrDefault(f.owner(), "?");
			Component name = f.id().equals(myFaction)
					? f.displayName().append(Component.literal(" ")).append(tr("factions.yours").copy().withColor(TEXT_DIM))
					: f.displayName();
			rows.add(Row.entry(name, tr("factions.details", ownerName, f.members().size(), f.flagCount()), TEXT_DIM, List.of()));
		}
		this.layoutRows(rows, this.contentTop(), 22, true, tr("factions.empty"));
	}

	// ---- Paged lists

	/** Places the rows of the current page from {@code y} down, with their buttons and page controls. */
	private void layoutRows(List<Row> rows, int y, int height, boolean twoLines, Component empty) {
		this.rowHeight = height;
		this.twoLineRows = twoLines;
		this.listTop = y;
		this.emptyText = rows.isEmpty() ? empty : null;

		int pagerY = this.top + this.panelHeight - 22;
		int perPage = Math.max(1, (pagerY - 2 - y) / height);
		this.pageCount = Math.max(1, (rows.size() + perPage - 1) / perPage);
		int page = Math.min(this.pages.getOrDefault(this.tab, 0), this.pageCount - 1);
		this.pages.put(this.tab, page);

		int right = this.contentRight();
		int from = page * perPage;
		int to = Math.min(rows.size(), from + perPage);
		for (int i = from; i < to; i++) {
			Row row = rows.get(i);
			int rowY = y + (i - from) * height;
			this.placedRows.add(new PlacedRow(row, rowY));
			int bx = right;
			int by = rowY + (height - ROW_BUTTON_HEIGHT) / 2;
			for (int b = row.buttons().size() - 1; b >= 0; b--) {
				RowButton button = row.buttons().get(b);
				bx -= button.width();
				this.addRenderableWidget(new FactionButton(bx, by, button.width(), ROW_BUTTON_HEIGHT, button.label(),
						button.color(), button.onPress()).setActive(button.active()));
				bx -= 3;
			}
		}

		if (this.pageCount > 1) {
			int cx = this.centerX();
			this.addRenderableWidget(new FactionButton(cx - 70, pagerY, 16, 14, tr("page.previous"), ACCENT, () -> {
				this.pages.put(this.tab, page - 1);
				this.needsRebuild = true;
			}).setActive(page > 0));
			this.addRenderableWidget(new FactionButton(cx + 54, pagerY, 16, 14, tr("page.next"), ACCENT, () -> {
				this.pages.put(this.tab, page + 1);
				this.needsRebuild = true;
			}).setActive(page < this.pageCount - 1));
		}
	}

	// ---------------------------------------------------------------- input and rebuilding

	/** Rebuilds the widgets, keeping the focused text box focused. */
	private void rebuildKeepingFocus() {
		this.needsRebuild = false;
		GuiEventListener focused = this.getFocused();
		int focusedBox = focused == null ? -1 : focused == this.nameBox ? 0 : focused == this.renameBox ? 1 : focused == this.inviteBox ? 2
				: focused == this.warBox ? 3 : -1;
		this.rebuildWidgets();
		EditBox box = switch (focusedBox) {
			case 0 -> this.nameBox;
			case 1 -> this.renameBox;
			case 2 -> this.inviteBox;
			case 3 -> this.warBox;
			default -> null;
		};
		if (box != null) {
			this.setFocused(box);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		this.confirmClicked = false;
		boolean handled = super.mouseClicked(event, doubleClick);
		if (!this.confirmClicked && this.pendingConfirm != null) {
			// Clicking anywhere else cancels a half-done confirmation.
			this.pendingConfirm = null;
			this.needsRebuild = true;
		}
		if (this.needsRebuild) {
			this.rebuildKeepingFocus();
		}
		return handled;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isConfirmation()) {
			GuiEventListener focused = this.getFocused();
			if (focused != null && focused == this.inviteBox && this.inviteButton != null && this.inviteButton.active) {
				this.sendInvite();
				this.rebuildKeepingFocus();
				return true;
			}
			if (focused != null && focused == this.warBox && this.warButton != null && this.warButton.active) {
				this.sendWarOnPlayer();
				this.rebuildKeepingFocus();
				return true;
			}
			if (focused != null && focused == this.renameBox && this.renameButton != null && this.renameButton.active) {
				this.renameButton.onPress(event);
				return true;
			}
			if (focused != null && focused == this.nameBox && this.createButton != null && this.createButton.active) {
				this.createButton.onPress(event);
				return true;
			}
		}
		boolean handled = super.keyPressed(event);
		if (this.needsRebuild) {
			this.rebuildKeepingFocus();
		}
		return handled;
	}

	@Override
	public void tick() {
		if (this.needsRebuild) {
			this.rebuildKeepingFocus();
		}
	}

	@Override
	protected void repositionElements() {
		this.rebuildKeepingFocus();
	}

	// ---------------------------------------------------------------- drawing

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.extractTransparentBackground(graphics);
		int x0 = this.left;
		int y0 = this.top;
		int x1 = x0 + this.panelWidth;
		int y1 = y0 + this.panelHeight;
		graphics.fill(x0, y0, x1, y1, PANEL_OUTER);
		graphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, PANEL_INNER);
		graphics.outline(x0, y0, this.panelWidth, this.panelHeight, ACCENT);
		graphics.outline(x0 + 2, y0 + 2, this.panelWidth - 4, this.panelHeight - 4, ACCENT_DARK);
		// Gold studs in the corners.
		for (int[] c : new int[][] {{x0 + 4, y0 + 4}, {x1 - 7, y0 + 4}, {x0 + 4, y1 - 7}, {x1 - 7, y1 - 7}}) {
			graphics.fill(c[0], c[1], c[0] + 3, c[1] + 3, ACCENT);
		}
		// Line under the tabs.
		int lineY = y0 + TAB_Y + TAB_HEIGHT + 3;
		graphics.fill(x0 + 8, lineY, x1 - 8, lineY + 1, ACCENT_DARK);
		this.minecraft.gui.hud.extractDeferredSubtitles();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(this.font, this.title, this.centerX(), this.top + 6, ACCENT);

		Optional<Faction> mine = this.mine();
		switch (this.tab) {
			case CREATE -> this.drawCreate(graphics);
			case OVERVIEW -> mine.ifPresent(f -> this.drawOverview(graphics, f));
			case RELATIONS, WAR -> this.drawInfoLines(graphics, this.contentTop());
			default -> {
			}
		}
		this.drawRows(graphics);
	}

	private void drawCreate(GuiGraphicsExtractor graphics) {
		int ct = this.contentTop();
		int cx = this.centerX();
		graphics.centeredText(this.font, tr("create.name"), cx, ct, ACCENT);
		if (this.nameBox != null) {
			int count = codePoints(this.createName);
			graphics.text(this.font, count + "/" + FactionManager.MAX_NAME_LENGTH,
					this.nameBox.getX() + this.nameBox.getWidth() + 4, ct + 15, count > 0 ? TEXT_DIM : GRAY);
		}
		String name = this.createName.trim();
		int tagColor = this.selectedColor == null ? TEXT : ColorSwatch.argb(this.selectedColor);
		Component tag = Component.literal("[" + name + "]").withColor(tagColor);
		graphics.centeredText(this.font, tr("create.preview", tag), cx, ct + 36, TEXT);
		Component colorLine = this.selectedColor == null ? tr("create.no_color")
				: tr("create.color", ColorSwatch.colorName(this.selectedColor).copy().withColor(tagColor));
		graphics.centeredText(this.font, colorLine, cx, ct + 50, ACCENT);
		Component problem = this.createProblem();
		graphics.centeredText(this.font, problem == null ? tr("status.ready") : problem, cx, ct + 106,
				problem == null ? GREEN : TEXT_DIM);
	}

	private void drawOverview(GuiGraphicsExtractor graphics, Faction faction) {
		int ct = this.contentTop();
		int cx = this.centerX();
		int x0 = this.contentLeft();
		Component name = faction.displayName();
		int nameWidth = this.font.width(name);
		if (nameWidth * 2 <= this.contentRight() - x0) {
			graphics.pose().pushMatrix();
			graphics.pose().translate(cx, ct);
			graphics.pose().scale(2.0F, 2.0F);
			graphics.text(this.font, name, -nameWidth / 2, 0, -1);
			graphics.pose().popMatrix();
		} else {
			graphics.centeredText(this.font, name, cx, ct + 4, -1);
		}
		String ownerName = faction.members().getOrDefault(faction.owner(), "?");
		graphics.centeredText(this.font, tr("overview.owner", Component.literal(ownerName).withColor(ACCENT)), cx, ct + 22, TEXT);
		graphics.centeredText(this.font, tr("overview.stats", faction.members().size(), faction.flagCount(), onOff(faction.redAlert())),
				cx, ct + 34, TEXT);
		if (faction.isOwner(this.me())) {
			graphics.text(this.font, tr("overview.rename_label"), x0, ct + 56, ACCENT);
			graphics.text(this.font, tr("overview.color_label"), x0, ct + 77, ACCENT);
			this.drawInfoLines(graphics, ct + 122);
		}
	}

	private void drawInfoLines(GuiGraphicsExtractor graphics, int y) {
		for (FormattedCharSequence line : this.infoLines) {
			graphics.text(this.font, line, this.contentLeft(), y, TEXT_DIM);
			y += 10;
		}
	}

	private void drawRows(GuiGraphicsExtractor graphics) {
		int x0 = this.contentLeft();
		int x1 = this.contentRight();
		if (this.emptyText != null) {
			List<FormattedCharSequence> lines = this.font.split(this.emptyText, x1 - x0);
			int y = this.listTop + 12;
			for (FormattedCharSequence line : lines) {
				graphics.centeredText(this.font, line, this.centerX(), y, TEXT_DIM);
				y += 10;
			}
		}
		for (PlacedRow placed : this.placedRows) {
			Row row = placed.row();
			int y = placed.y();
			if (row.header()) {
				int textY = y + (this.rowHeight - 8) / 2;
				graphics.text(this.font, row.text(), x0, textY, ACCENT);
				int lineX = x0 + this.font.width(row.text()) + 4;
				if (lineX < x1) {
					graphics.fill(lineX, textY + 4, x1, textY + 5, ACCENT_DARK);
				}
				continue;
			}
			int buttonsWidth = 0;
			for (RowButton button : row.buttons()) {
				buttonsWidth += button.width() + 3;
			}
			int available = x1 - x0 - 4 - buttonsWidth;
			// Faint separator under each entry.
			graphics.fill(x0, y + this.rowHeight - 1, x1, y + this.rowHeight, 0x3035E9F5);
			if (this.twoLineRows) {
				graphics.text(this.font, this.fit(row.text(), available), x0 + 4, y + 2, TEXT);
				if (row.detail() != null) {
					graphics.text(this.font, this.fit(row.detail(), available), x0 + 4, y + 12, row.detailColor());
				}
			} else {
				int textY = y + (this.rowHeight - 8) / 2;
				int detailWidth = row.detail() == null ? 0 : Math.min(this.font.width(row.detail()), available / 2);
				int nameSpace = available - (detailWidth > 0 ? detailWidth + 8 : 0);
				FormattedCharSequence text = this.fit(row.text(), nameSpace);
				graphics.text(this.font, text, x0 + 4, textY, TEXT);
				if (row.detail() != null) {
					int dx = x0 + 4 + this.font.width(text) + 8;
					graphics.text(this.font, this.fit(row.detail(), detailWidth), dx, textY, row.detailColor());
				}
			}
		}
		if (this.pageCount > 1) {
			int page = this.pages.getOrDefault(this.tab, 0);
			graphics.centeredText(this.font, tr("page", page + 1, this.pageCount), this.centerX(),
					this.top + this.panelHeight - 19, TEXT_DIM);
		}
	}

	/** The text, cut short with "..." if it's wider than {@code maxWidth}. */
	private FormattedCharSequence fit(Component text, int maxWidth) {
		if (this.font.width(text) <= maxWidth) {
			return text.getVisualOrderText();
		}
		int ellipsis = this.font.width("...");
		FormattedText cut = this.font.substrByWidth(text, Math.max(0, maxWidth - ellipsis));
		return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of("...")));
	}
}
