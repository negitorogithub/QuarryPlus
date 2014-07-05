/*
 * Copyright (C) 2012,2013 yogpstop
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the
 * GNU Lesser General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.yogpc.qp.client;

import com.yogpc.qp.PacketHandler;
import com.yogpc.qp.TileBasic;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiQ_SelectBlock extends GuiScreenA {
	private GuiQ_SlotBlockList blocks;
	private TileBasic tile;
	private byte targetid;

	public GuiQ_SelectBlock(GuiScreen pscr, TileBasic tb, byte id) {
		super(pscr);
		this.tile = tb;
		this.targetid = id;
	}

	@Override
	public void initGui() {
		this.blocks = new GuiQ_SlotBlockList(this.mc, this.width, this.height, 24, this.height - 32, 18, this, this.targetid == 0 ? this.tile.fortuneList
				: this.tile.silktouchList);
		this.buttonList.add(new GuiButton(-1, this.width / 2 - 150, this.height - 26, 140, 20, StatCollector.translateToLocal("gui.done")));
		this.buttonList.add(new GuiButton(-2, this.width / 2 + 10, this.height - 26, 140, 20, StatCollector.translateToLocal("gui.cancel")));
	}

	@Override
	public void actionPerformed(GuiButton par1) {
		switch (par1.id) {
		case -1:
			PacketHandler.sendPacketToServer(this.tile, (byte) (PacketHandler.CtS_ADD_FORTUNE + this.targetid), this.blocks.currentblock);
			break;
		case -2:
			showParent();
			break;
		}
	}

	@Override
	public void drawScreen(int i, int j, float k) {
		drawDefaultBackground();
		this.blocks.drawScreen(i, j, k);
		String title = StatCollector.translateToLocal("tof.selectblock");
		this.fontRendererObj.drawStringWithShadow(title, (this.width - this.fontRendererObj.getStringWidth(title)) / 2, 8, 0xFFFFFF);
		super.drawScreen(i, j, k);
	}
}
