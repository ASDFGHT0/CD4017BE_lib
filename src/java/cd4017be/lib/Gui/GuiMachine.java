package cd4017be.lib.Gui;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.TileContainer.TankSlot;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.util.Utils;
import cd4017be.lib.util.Vec3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.SlotItemHandler;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author CD4017BE
 */
public abstract class GuiMachine extends GuiContainer {

	public static final ResourceLocation LIB_TEX = new ResourceLocation("cd4017be_lib", "textures/icons.png");
	public ResourceLocation MAIN_TEX;
	public int focus = -1, tabsX = 0, tabsY = 7, bgTexX = 0, bgTexY = 0, titleX, titleY;
	/**	1: background texture, 2: main title, 4: inventory title */
	protected byte drawBG = 7;
	public ArrayList<GuiComp> guiComps = new ArrayList<GuiComp>();
	private Slot lastClickSlot;

	public GuiMachine(Container container) {
		super(container);
	}

	@Override
	public void initGui() {
		guiComps.clear();
		super.initGui();
		titleX = xSize / 2; titleY = 4;
		if (inventorySlots instanceof TileContainer) {
			TileContainer cont = (TileContainer)inventorySlots;
			for (TankSlot slot : cont.tankSlots)
				guiComps.add(new FluidTank(guiComps.size(), slot));
			if (cont.data instanceof AutomatedTile) {
				AutomatedTile tile = (AutomatedTile)cont.data;
				int xPos = tabsX;
				if (tile.tanks != null && tile.tanks.tanks.length > 0) guiComps.add(new FluidSideCfg(guiComps.size(), xPos -= tile.tanks.tanks.length * 9 + 9, tabsY, tile));
				if (tile.inventory != null && tile.inventory.groups.length > 0) guiComps.add(new ItemSideCfg(guiComps.size(), xPos -= tile.inventory.groups.length * 9 + 9, tabsY, tile));
				if (tile.energy != null) guiComps.add(new EnergySideCfg(guiComps.size(), xPos - 18, tabsY, tile));
			}
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) {
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.disableDepth();
		GlStateManager.disableAlpha();
		GlStateManager.enableBlend();
		GlStateManager.pushMatrix();
		GlStateManager.translate(-guiLeft, -guiTop, 0);
		for (GuiComp comp : guiComps)
			if (comp.isInside(mx, my))
				comp.drawOverlay(mx, my);
		GlStateManager.popMatrix();
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		if ((drawBG & 1) != 0) {
			mc.renderEngine.bindTexture(MAIN_TEX);
			this.drawTexturedModalRect(guiLeft, guiTop, bgTexX, bgTexY, xSize, ySize);
		}
		if ((drawBG & 4) != 0 && inventorySlots instanceof TileContainer) {
			TileContainer cont = (TileContainer)inventorySlots;
			if (cont.invPlayerS != cont.invPlayerE) {
				Slot pos = cont.inventorySlots.get(cont.invPlayerS);
				this.drawStringCentered(I18n.translateToLocal("container.inventory"), this.guiLeft + pos.xDisplayPosition + 80, this.guiTop + pos.yDisplayPosition - 14, 0x404040);
			}
		}
		if ((drawBG & 2) != 0 && inventorySlots instanceof DataContainer)
			this.drawStringCentered(((DataContainer)inventorySlots).data.getName(), guiLeft + titleX, guiTop + titleY, 0x404040);
		GlStateManager.color(1F, 1F, 1F, 1F);
		for (GuiComp comp : guiComps) comp.draw();
	}

	@Override
	protected void mouseClicked(int x, int y, int b) throws IOException {
		boolean doSuper = true;
		for (GuiComp comp : guiComps) 
			if (comp.isInside(x, y)) {
				if (comp.id != focus) this.setFocus(comp.id);
				doSuper = !comp.mouseIn(x, y, b, 0);
				if (!doSuper) break;
			}
		if (focus >= 0 && !guiComps.get(focus).isInside(x, y)) this.setFocus(-1);
		if (doSuper) super.mouseClicked(x, y, b);
	}

	@Override
	protected void mouseClickMove(int x, int y, int b, long t) {
		if (focus >= 0) guiComps.get(focus).mouseIn(x, y, b, 1);
		else {
			Slot slot = this.getSlotUnderMouse();
			ItemStack itemstack = this.mc.thePlayer.inventory.getItemStack();
			if (slot instanceof SlotHolo && slot != lastClickSlot) {
				if (itemstack == null || slot.getStack() == null || itemstack.isItemEqual(slot.getStack()))
					this.handleMouseClick(slot, slot.slotNumber, b, ClickType.PICKUP);
			} else super.mouseClickMove(x, y, b, t);
			lastClickSlot = slot;
		}
	}

	@Override
	protected void mouseReleased(int x, int y, int b) {
		if (focus < 0 || !guiComps.get(focus).mouseIn(x, y, b, 2))
			super.mouseReleased(x, y, b);
		lastClickSlot = null;
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (focus >= 0) guiComps.get(focus).keyTyped(typedChar, keyCode);
		else super.keyTyped(typedChar, keyCode);
	}

	public void drawFormatInfo(int x, int y, String key, Object... args) {
		this.drawHoveringText(Arrays.asList(TooltipInfo.format("gui.cd4017be." + key, args).split("\n")), x, y, fontRendererObj);
	}

	public void drawLocString(int x, int y, int h, int c, String s, Object... args) {
		String[] text = TooltipInfo.format("gui.cd4017be." + s, args).split("\n");
		for (String l : text) {
			this.fontRendererObj.drawString(l, x, y, c);
			y += h;
		}
	}

	public void drawStringCentered(String s, int x, int y, int c) {
		this.fontRendererObj.drawString(s, x - this.fontRendererObj.getStringWidth(s) / 2, y, c);
	}

	protected void drawSideCube(int x, int y, int s, byte dir) {
		GlStateManager.enableDepth();
		this.drawGradientRect(x, y, x + 64, y + 64, 0xff000000, 0xff000000);
		this.mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GL11.glPushMatrix();
		GL11.glTranslatef(x + 32, y + 32, 32);
		GL11.glScalef(16F, -16F, 16F);
		EntityPlayer player = ((DataContainer)this.inventorySlots).player;
		GL11.glRotatef(player.rotationPitch, 1, 0, 0);
		GL11.glRotatef(player.rotationYaw + 90, 0, 1, 0);
		GL11.glTranslatef(-0.5F, -0.5F, 0.5F);
		IGuiData tile = ((DataContainer)this.inventorySlots).data;
		this.mc.getBlockRendererDispatcher().renderBlockBrightness(tile.pos().getY() >= 0 ? player.worldObj.getBlockState(tile.pos()) : Blocks.GLASS.getDefaultState(), 1);
		//GL11.glRotatef(-90, 0, 1, 0);
		this.mc.renderEngine.bindTexture(LIB_TEX);
		Vec3 p = Vec3.Def(0.5, 0.5, 0.5), a, b;
		switch(s) {
		case 0: a = Vec3.Def(0, -1, 0); break;
		case 1: a = Vec3.Def(0, 1, 0); break;
		case 2: a = Vec3.Def(0, 0, -1); break;
		case 3: a = Vec3.Def(0, 0, 1); break;
		case 4: a = Vec3.Def(-1, 0, 0); break;
		default: a = Vec3.Def(1, 0, 0);
		}
		Vec3d look = player.getLookVec();
		b = Vec3.Def(look.xCoord, look.yCoord, look.zCoord).mult(a).norm();
		p = p.add(a.scale(0.5)).add(b.scale(-0.5));
		a = a.scale(1.5);
		final float tx = (float)(144 + 16 * dir) / 256F, dtx = 16F / 256F, ty = 24F / 256F, dty = 8F / 256F;
		
		VertexBuffer t = Tessellator.getInstance().getBuffer();
		t.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		t.pos(p.x + b.x, p.y + b.y, p.z + b.z).tex(tx, ty + dty).endVertex();
		t.pos(p.x + a.x + b.x, p.y + a.y + b.y, p.z + a.z + b.z).tex(tx + dtx, ty + dty).endVertex();
		t.pos(p.x + a.x, p.y + a.y, p.z + a.z).tex(tx + dtx, ty).endVertex();
		t.pos(p.x, p.y, p.z).tex(tx, ty).endVertex();
		Tessellator.getInstance().draw();
		GL11.glPopMatrix();
	}

	public void drawItemStack(ItemStack stack, int x, int y, String altText){
		zLevel = 200.0F;
		itemRender.zLevel = 200.0F;
		net.minecraft.client.gui.FontRenderer font = null;
		if (stack != null) font = stack.getItem().getFontRenderer(stack);
		if (font == null) font = fontRendererObj;
		this.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
		this.itemRender.renderItemOverlayIntoGUI(font, stack, x, y, altText);
		this.zLevel = 0.0F;
		this.itemRender.zLevel = 0.0F;
	}

	public static void color(int c) {
		GlStateManager.enableBlend();
		GlStateManager.color((float)(c >> 16 & 0xff) / 255F, (float)(c >> 8 & 0xff) / 255F, (float)(c & 0xff) / 255F, (float)(c >> 24 & 0xff) / 255F);
	}

	public void setFocus(int id) {
		if (focus >= 0 && focus < guiComps.size()) guiComps.get(focus).unfocus();
		focus = id >= 0 && id < guiComps.size() && guiComps.get(id).focus() ? id : -1;
	}

	protected Object getDisplVar(int id) {return null;}
	protected void setDisplVar(int id, Object obj, boolean send) {}

	public class GuiComp {
		public final int id, px, py, w, h;
		public String tooltip;
		public GuiComp(int id, int px, int py, int w, int h) {
			this.id = id;
			this.px = px + guiLeft;
			this.py = py + guiTop;
			this.w = w; this.h = h;
		}
		/** @param s '#' gets replaced with state or prefix 'x*A+B;' (A,B are numbers) does linear transformation on numeric state and uses it as format argument */
		public GuiComp setTooltip(String s) {
			this.tooltip = s;
			return this;
		}
		public boolean isInside(int x, int y) {
			return x >= px && x < px + w && y >= py && y < py + h;
		}
		public void drawOverlay(int mx, int my) {
			if (tooltip == null) return;
			String text;
			if (tooltip.startsWith("x*")) {
				int p = tooltip.indexOf('+', 2), q = tooltip.indexOf(';', p);
				float f = (Float)getDisplVar(id) * Float.parseFloat(tooltip.substring(2, p)) + Float.parseFloat(tooltip.substring(p + 1, q));
				text = TooltipInfo.format("gui.cd4017be." + tooltip.substring(q + 1), f);
			} else {
				Object o = getDisplVar(id);
				text = TooltipInfo.getLocFormat("gui.cd4017be." + tooltip.replace("#", o == null ? "" : o.toString()));
			}
			drawHoveringText(Arrays.asList(text.split("\n")), mx, py + h + 12, fontRendererObj);
		}
		public void draw() {}
		public void keyTyped(char c, int k) {}
		/** @param b mouse button: 0=left 1=right 2=middle
		 *  @param d event type: 0=click 1=clickMove 2=release
		 *  @return consume event*/
		public boolean mouseIn(int x, int y, int b, int d) {return false;}
		public void unfocus() {}
		/** @return do focus */
		public boolean focus() {return false;}
	}

	public class Tooltip extends GuiComp {

		public Tooltip(int id, int px, int py, int w, int h, String tooltip) {
			super(id, px, py, w, h);
			this.setTooltip(tooltip);
		}

		@Override
		public void drawOverlay(int mx, int my) {
			Object obj = getDisplVar(id);
			Object[] objA = obj instanceof Object[] ? (Object[])obj : new Object[]{obj};
			String s = tooltip.startsWith("\\") ? String.format(tooltip.substring(1), objA) : TooltipInfo.format("gui.cd4017be." + tooltip, objA);
			drawHoveringText(Arrays.asList(s.split("\n")), mx, my, fontRendererObj);
		}

	}

	public class Text extends GuiComp {
		public String text;
		public int fh = 8, tc = 0xff404040;
		public boolean center = false;

		public Text(int id, int x, int y, int w, int h, String key) {
			super(id, x, y, w, h);
			this.text = key;
		}

		public Text font(int tc, int fh) {
			this.tc = tc;
			this.fh = fh;
			return this;
		}

		public Text center() {
			this.center = true;
			return this;
		}

		@Override
		public void draw() {
			Object obj = getDisplVar(id);
			Object[] objA = obj instanceof Object[] ? (Object[])obj : new Object[]{obj};
			String[] lines = (text.startsWith("\\") ? 
					String.format(text.substring(1), objA) : 
					TooltipInfo.format("gui.cd4017be." + (text.contains("#") ? text.replaceAll("#", ((Integer)obj).toString()) : text), objA)
				).split("\n");
			int y = py, x;
			for (String l : lines) {
				x = center ? px + (w - fontRendererObj.getStringWidth(l)) / 2 : px;
				fontRendererObj.drawString(l, x, y, tc);
				y += fh;
			}
			GlStateManager.color(1F, 1F, 1F, 1F);
		}
	}

	public class TextField extends GuiComp {
		public final int maxL;
		public int tc = 0xff404040, cc = 0xff800000;
		public String text = "";
		public int cur;

		public TextField(int id, int x, int y, int w, int h, int max) {
			super(id, x, y, w, h);
			this.maxL = max;
		}

		public TextField color(int text, int cursor) {
			this.tc = text; this.cc = cursor;
			return this;
		}

		@Override
		public void draw() {
			if (focus == id) {
				if (cur > text.length()) cur = text.length();
				drawVerticalLine(px - 1 + fontRendererObj.getStringWidth(text.substring(0, cur)), py + (h - 9) / 2, py + (h + 7) / 2, cc);
			} else text = (String)getDisplVar(id);
			fontRendererObj.drawString(text, px, py + (h - 8) / 2, tc);
			GlStateManager.color(1, 1, 1, 1);
		}

		@Override
		public void keyTyped(char c, int k) {
			try {
				boolean ctr = isCtrlKeyDown() && !isShiftKeyDown() && !isAltKeyDown();
				switch(k) {
				case Keyboard.KEY_LEFT: if (cur > 0) cur--; break;
				case Keyboard.KEY_RIGHT: if (cur < text.length()) cur++; break;
				case Keyboard.KEY_DELETE: if (cur < text.length()) {
						text = text.substring(0, cur).concat(text.substring(cur + 1));
					} break;
				case Keyboard.KEY_BACK: if (cur > 0) {
						cur--;
						text = text.substring(0, cur).concat(text.substring(cur + 1));
					} break;
				case Keyboard.KEY_RETURN: setFocus(-1); break;
				case Keyboard.KEY_UP: setFocus(id - 1); break;
				case Keyboard.KEY_DOWN: setFocus(id + 1); break;
				case Keyboard.KEY_C: if (ctr) {
						setClipboardString(text);
						break;
					}
				case Keyboard.KEY_V: if (ctr) {
						String s = getClipboardString();
						text = text.substring(0, cur).concat(s).concat(text.substring(cur, text.length()));
						cur += s.length();
						if (text.length() > maxL) {
							text = text.substring(0, maxL);
							cur = maxL;
						}
						break;
					}
				case Keyboard.KEY_D: if (ctr) {
						text = "";
						break;
					}
				default: if (ChatAllowedCharacters.isAllowedCharacter(c) && cur < maxL){
						text = text.substring(0, cur).concat("" + c).concat(text.substring(cur, Math.min(text.length(), maxL - 1)));
						cur++;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				if (cur < 0) cur = 0;
				if (cur > text.length()) cur = text.length();
			}
		}

		@Override
		public void unfocus() {
			setDisplVar(id, text, true);
		}

		@Override
		public boolean focus() {
			text = (String)getDisplVar(id);
			cur = text.length();
			return true;
		}

	}

	public class Slider extends GuiComp {
		public final int l, tx, ty, tw, th;
		public final boolean hor;

		public Slider(int id, int x, int y, int l, int texX, int texY, int texW, int texH, boolean hor) {
			super(id, x, y, hor?l:texW, hor?texH:l);
			this.hor = hor;
			this.l = l;
			this.tx = texX;
			this.ty = texY;
			this.tw = texW;
			this.th = texH;
		}

		@Override
		public void draw() {
			mc.renderEngine.bindTexture(MAIN_TEX);
			int f = (int)((Float)getDisplVar(id) * (float)l - 0.5F * (float)(hor?tw:th));
			drawTexturedModalRect(hor? px + f : px, hor? py : py + f, tx, ty, tw, th);
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			float f = ((float)(hor? x - px : y - py) + 0.5F) / (float)l;
			if (f < 0) f = 0;
			else if (f > 1) f = 1;
			setDisplVar(id, f, false);
			if (d == 2) setFocus(-1);
			return true;
		}

		@Override
		public void unfocus() {
			setDisplVar(id, getDisplVar(id), true);
		}

		@Override
		public boolean focus() {return true;}

	}

	public class NumberSel extends GuiComp {
		public boolean hor = false;
		public int ts = 4, tc = 0xff404040, nb = 1, min, max, exp;
		public final String form;

		public NumberSel(int id, int px, int py, int w, int h, String form, int min, int max, int exp) {
			super(id, px, py, w, h);
			this.min = min;
			this.max = max;
			this.exp = exp;
			this.form = form;
		}

		public NumberSel setup(int ts, int tc, int nb, boolean hor) {
			this.nb = nb;
			this.tc = tc;
			this.ts = ts / 2;
			this.hor = hor;
			return this;
		}

		@Override
		public void draw() {
			String s = String.format(form, getDisplVar(id));
			int x = px + (w - fontRendererObj.getStringWidth(s)) / 2, y = py + (h - 8) / 2;
			fontRendererObj.drawString(s, x, y, tc);
			GlStateManager.color(1F, 1F, 1F, 1F);
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			if (d != 0) return true;
			int pw = (hor ? w : h) / 2, p = (hor ? x - px : py + h - 1 - y) - pw;
			int ofs;
			if (p < -ts) {
				p = (-p - ts) * nb / (pw - ts) * 2 + b;
				ofs = -1;
				for (int i = 0; i < p; i++) ofs *= exp;
			} else if (p >= ts) {
				p = (p - ts) * nb / (pw - ts) * 2 + b;
				ofs = 1;
				for (int i = 0; i < p; i++) ofs *= exp;
			} else ofs = 0;
			if (ofs != 0)
				setDisplVar(id, Math.max(min, Math.min(max, (Integer)getDisplVar(id) + ofs)), true);
			return true;
		}

	}

	public class Button extends GuiComp {

		public final int states;
		public int tx, ty;

		public Button(int id, int px, int py, int w, int h, int states) {
			super(id, px, py, w, h);
			this.states = states;
		}

		public Button texture(int tx, int ty) {
			this.tx = tx;
			this.ty = ty;
			return this;
		}

		@Override
		public void drawOverlay(int mx, int my) {
			super.drawOverlay(mx, my);
			Object o;
			if (states >= 0 && (o = getDisplVar(id)) instanceof EnumFacing)
				drawSideCube(tabsX + guiLeft - 64, tabsY + guiTop + 63, ((EnumFacing)o).ordinal(), (byte)states);//TODO check render pos
		}

		@Override
		public void draw() {
			if (states < 0) return;
			Object o = getDisplVar(id);
			int s = o instanceof EnumFacing ? ((EnumFacing)o).ordinal() : (Integer)o;
			mc.renderEngine.bindTexture(MAIN_TEX);
			drawTexturedModalRect(px, py, tx, ty + s * h, w, h);
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			setDisplVar(id, b, true);
			return b == 0;
		}

	}

	public class ProgressBar extends GuiComp {
		public final byte type;
		public final int tx, ty;

		/** @param type 0:horFrac, 1:vertFrac, 2:horShift, 3:vertShift, 4:precision */
		public ProgressBar(int id, int px, int py, int w, int h, int tx, int ty, byte type) {
			super(id, px, py, w, h);
			this.type = type;
			this.tx = tx;
			this.ty = ty;
		}

		@Override
		public void draw() {
			float f = (Float)getDisplVar(id);
			if (Float.isNaN(f)) return;
			if (type < 2 || type == 4) {
				if (f > 1) f = 1;
				else if (f < -1) f = -1;
			}
			mc.renderEngine.bindTexture(MAIN_TEX);
			boolean v = (type & 1) != 0;
			if (type == 0 || type == 1) {
				int n = (int)((float)(v?h:w) * (f<0?-f:f));
				int dx = (!v && f<0)? w - n : 0, dy = (v && f>0)? h - n : 0;
				drawTexturedModalRect(px + dx, py + dy, tx + dx, ty + dy, v?w:n, v?n:h);
			} else if (type == 2 || type == 3) {
				int n = (int)((float)(v?h:w) * f);
				drawTexturedModalRect(px, py, tx + (v?0:n), ty + (v?n:0), w, h);
			} else if (type == 4) {
				int n = (int)((float)(w * h) * (f<0?-f:f)), m = n / h; n %= h;
				int dx = f<0 ? w - m : 0, dx1 = f<0 ? w - m - 1 : m, dy1 = f<0 ? h - n : 0;
				drawTexturedModalRect(px + dx, py, tx + dx, ty, m, h);
				drawTexturedModalRect(px + dx1, py + dy1, tx + dx1, ty + dy1, 1, n);
			}
		}

	}

	public class EnergySideCfg extends GuiComp {
		final AutomatedTile tile;

		public EnergySideCfg(int id, int px, int py, AutomatedTile tile) {
			super(id, px, py, 18, 63);
			this.tile = tile;
		}

		@Override
		public void drawOverlay(int mx, int my) {
			int s = (my - py) / 9 - 1;
			if (s >= 0)
				drawSideCube(guiLeft + tabsX - 64, py + 63, s, (tile.energy.sideCfg >> s & 1) != 0 ? (byte)3 : 0);
		}

		@Override
		public void draw() {
			mc.renderEngine.bindTexture(LIB_TEX);
			drawTexturedModalRect(px, py, 0, 0, 18, 63);
			drawTexturedModalRect(px, py, 0, 99, 9, 9);
			drawTexturedModalRect(px + 9, py, 36, 90, 9, 9);
			for (int i = 0; i < 6; i++)
				drawTexturedModalRect(px + 9, py + 9 + i * 9, (tile.energy.sideCfg >> i & 1) != 0 ? 36 : 9, 81, 9, 9);
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			if (x >= px + 9 && (y -= py + 9) >= 0) {
				PacketBuffer dos = tile.getPacketTargetData();
				dos.writeByte(3);
				dos.writeByte(tile.energy.sideCfg ^= 1 << (y / 9));
				BlockGuiHandler.sendPacketToServer(dos);
			}
			return true;
		}

	}

	public class FluidSideCfg extends GuiComp {
		final AutomatedTile tile;

		public FluidSideCfg(int id, int px, int py, AutomatedTile tile) {
			super(id, px, py, 9 * tile.tanks.tanks.length + 9, 81);
			this.tile = tile;
		}

		@Override
		public void drawOverlay(int mx, int my) {
			int s = (my - py) / 9 - 1;
			int i = (mx - px) / 9 - 1;
			byte dir = s < 6 ? tile.tanks.getConfig(s, i) : s != 6 ? (byte)4 : tile.tanks.isLocked(i) ? (byte)5 : (byte)6; 
			if (i >= 0) {
				mc.renderEngine.bindTexture(LIB_TEX);
				for (TankSlot slot : ((TileContainer)inventorySlots).tankSlots)
					if (slot.tankNumber == i)
						drawTexturedModalRect(guiLeft + slot.xDisplayPosition + (slot.size >> 4 & 0xf) * 9 - 9, guiTop + slot.yDisplayPosition + (slot.size & 0xf) * 18 - (s<6?10:18), 144 + dir * 16, 16, 16, s<6?8:16);
			}
			if(s >= 0 && s < 6) drawSideCube(guiLeft + tabsX - 64, py + 63, s, dir);
		}

		@Override
		public void draw() {
			int s = tile.tanks.tanks.length;
			mc.renderEngine.bindTexture(LIB_TEX);
			drawTexturedModalRect(px, py, 0, 0, 9 + s * 9, 81);
			drawTexturedModalRect(px, py, 0, 81, 9, 9);
			for (int j = 0; j < s; j++) {
				drawTexturedModalRect(px + 9 + j * 9, py, 18 + tile.tanks.tanks[j].dir * 9, 90, 9, 9);
				for (int i = 0; i < 6; i++)
					drawTexturedModalRect(px + 9 + j * 9, py + 9 + i * 9, 9 + (int)(tile.tanks.sideCfg >> (8 * i + 2 * j) & 3) * 9, 81, 9, 9);
				if ((tile.tanks.sideCfg >> (48 + j) & 1) != 0)
					drawTexturedModalRect(px + 9 + j * 9, py + 63, 9, 81, 9, 9);
			}
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			x = (x - px) / 9 - 1;
			y = (y - py) / 9 - 1;
			if (x >= 0 && y >= 0) {
				PacketBuffer dos = tile.getPacketTargetData();
				if (y == 7) dos.writeByte(2).writeByte(x);
				else if (y == 6) dos.writeByte(1).writeLong(tile.tanks.sideCfg ^= 1L << (48 + x));
				else {
					int p = y * 8 + x * 2;
					long sp = 3L << p;
					dos.writeByte(1).writeLong(tile.tanks.sideCfg = (tile.tanks.sideCfg & ~sp) | (tile.tanks.sideCfg + (b == 0 ? 1L << p : sp) & sp));
				}
				BlockGuiHandler.sendPacketToServer(dos);
			}
			return true;
		}

	}

	public class ItemSideCfg extends GuiComp {
		final AutomatedTile tile;

		public ItemSideCfg(int id, int px, int py, AutomatedTile tile) {
			super(id, px, py, 9 * tile.inventory.groups.length + 9, 63);
			this.tile = tile;
		}

		@Override
		public void drawOverlay(int mx, int my) {
			int s = (my - py) / 9 - 1;
			int i = (mx - px) / 9 - 1;
			byte dir = tile.inventory.getConfig(s, i);
			if (i >= 0) {
				mc.renderEngine.bindTexture(LIB_TEX);
				int i0 = tile.inventory.groups[i].s, i1 = tile.inventory.groups[i].e;
				for (Slot slot : inventorySlots.inventorySlots)
					if (slot instanceof SlotItemHandler && slot.getSlotIndex() >= i0 && slot.getSlotIndex() < i1)
						drawTexturedModalRect(guiLeft + slot.xDisplayPosition, guiTop + slot.yDisplayPosition, 144 + dir * 16, 0, 16, 16);
			}
			if (s >= 0) drawSideCube(guiLeft + tabsX - 64, py + 63, s, dir);
		}

		@Override
		public void draw() {
			int s = tile.inventory.groups.length;
			mc.renderEngine.bindTexture(LIB_TEX);
			drawTexturedModalRect(px, py, 0, 0, 9 + s * 9, 63);
			drawTexturedModalRect(px, py, 0, 90, 9, 9);
			for (int j = 0; j < s; j++) {
				drawTexturedModalRect(px + 9 + j * 9, py, 18 + tile.inventory.groups[j].dir * 9, 90, 9, 9);
				for (int i = 0; i < 6; i++)
					drawTexturedModalRect(px + 9 + j * 9, py + 9 + i * 9, 9 + (int)(tile.inventory.sideCfg >> (10 * i + 2 * j) & 3) * 9, 81, 9, 9);
			}
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			x = (x - px) / 9 - 1;
			y = (y - py) / 9 - 1;
			if (x >= 0 && y >= 0) {
				int p = y * 10 + x * 2;
				long sp = 3L << p;
				PacketBuffer dos = tile.getPacketTargetData();
				dos.writeByte(0);
				dos.writeLong(tile.inventory.sideCfg = (tile.inventory.sideCfg & ~sp) | (tile.inventory.sideCfg + (b == 0 ? 1L << p : sp) & sp));
				BlockGuiHandler.sendPacketToServer(dos);
			}
			return true;
		}

	}

	public class FluidTank extends GuiComp {
		final TankSlot slot;

		public FluidTank(int id, TankSlot slot) {
			super(id, slot.xDisplayPosition, slot.yDisplayPosition, (slot.size >> 4 & 0xf) * 18 - 2, (slot.size & 0xf) * 18 - 2);
			this.slot = slot;
		}

		@Override
		public void drawOverlay(int mx, int my) {
			FluidStack stack = slot.getStack();
			ArrayList<String> info = new ArrayList<String>();
			info.add(stack != null ? stack.getLocalizedName() : "Empty");
			info.add(String.format("%s/%s ", Utils.formatNumber(stack != null ? (float)stack.amount / 1000F : 0F, 3), Utils.formatNumber((float)slot.inventory.getCapacity(slot.tankNumber) / 1000F, 3)) + TooltipInfo.getFluidUnit());
			drawHoveringText(info, mx, my, fontRendererObj);
		}

		@Override
		public void draw() {
			GlStateManager.disableAlpha();
			GlStateManager.enableBlend();
			ResourceLocation res;
			FluidStack stack = slot.getStack();
			if (stack != null && (res = stack.getFluid().getStill()) != null) {
				mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
				int c = slot.inventory.getCapacity(slot.tankNumber);
				int n = c == 0 || stack.amount >= c ? h : (int)((long)h * (long)stack.amount / (long)c);
				drawTexturedModalRect(px, py + h - n, mc.getTextureMapBlocks().getAtlasSprite(res.toString()), w, n);
			}
			mc.renderEngine.bindTexture(LIB_TEX);
			drawTexturedModalRect(px + w - 16, py, 110, 52 - h, 16, h);
			GlStateManager.disableBlend();
			GlStateManager.enableAlpha();
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			if (d == 0 && inventorySlots instanceof DataContainer) {
				FluidStack fluid = FluidUtil.getFluidContained(((DataContainer)inventorySlots).player.inventory.getItemStack());
				setDisplVar(id, fluid != null ? fluid.getFluid() : null, false);
			}
			return false;
		}

	}

}
