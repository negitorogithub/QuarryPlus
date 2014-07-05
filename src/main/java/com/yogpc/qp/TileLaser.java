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

package com.yogpc.qp;

import java.util.ArrayList;
import java.util.List;

import static buildcraft.BuildCraftCore.actionOn;
import static buildcraft.BuildCraftCore.actionOff;
import buildcraft.api.core.Position;
import buildcraft.api.gates.IAction;
import buildcraft.api.gates.IActionReceptor;
import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerHandler;
import buildcraft.api.power.PowerHandler.PowerReceiver;
import buildcraft.api.power.PowerHandler.Type;
import buildcraft.core.IMachine;
import buildcraft.core.LaserData;
import buildcraft.core.triggers.ActionMachineControl;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileLaser extends TileEntity implements IPowerReceptor, IActionReceptor, IMachine, IEnchantableTile {
	private LaserData[] lasers;
	private final List<Object> laserTargets = new ArrayList<Object>();
	protected final PowerHandler powerHandler = new PowerHandler(this, Type.MACHINE);
	private ActionMachineControl.Mode lastMode = ActionMachineControl.Mode.Unknown;

	protected byte unbreaking;
	protected byte fortune;
	protected byte efficiency;
	protected boolean silktouch;

	private long from = 38669;

	public TileLaser() {
		PowerManager.configureL(this.powerHandler, this.efficiency, this.unbreaking);
	}

	@Override
	public void updateEntity() {
		super.updateEntity();
		if (this.worldObj.isRemote) return;

		if (this.lastMode == ActionMachineControl.Mode.Off) {
			removeLaser();
			return;
		}

		if (!isValidTable() && (this.worldObj.getWorldTime() % 100) == (this.from % 100)) {
			findTable();
		}

		if (!isValidTable() || this.powerHandler.getEnergyStored() == 0) {
			removeLaser();
			return;
		}

		if (!isValidLaser()) {// createLaser
			for (int i = 0; i < this.lasers.length; i++) {
				this.lasers[i] = new LaserData(new Position(this.xCoord, this.yCoord, this.zCoord), new Position(this.xCoord, this.yCoord, this.zCoord));
				this.worldObj.spawnEntityInWorld(this.lasers[i]);
				this.from = this.worldObj.getWorldTime();
			}
		}

		if (isValidLaser() && (this.worldObj.getWorldTime() % 10) == (this.from % 10)) {// updateLaser
			ForgeDirection fd = ForgeDirection.values()[this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord)];
			Position head = new Position(this.xCoord + 0.5 + 0.3 * fd.offsetX, this.yCoord + 0.5 + 0.3 * fd.offsetY, this.zCoord + 0.5 + 0.3 * fd.offsetZ);
			for (int i = 0; i < this.laserTargets.size(); i++) {
				Position tail = new Position(ILaserTargetHelper.getXCoord(this.laserTargets.get(i)) + 0.475 + (this.worldObj.rand.nextFloat() - 0.5) / 5F,
						ILaserTargetHelper.getYCoord(this.laserTargets.get(i)) + 9F / 16F, ILaserTargetHelper.getZCoord(this.laserTargets.get(i)) + 0.475
								+ (this.worldObj.rand.nextFloat() - 0.5) / 5F);
				this.lasers[i].setPositions(head, tail);

				if (!this.lasers[i].isVisible()) this.lasers[i].show();
			}
		}

		float power = PowerManager.useEnergyL(this.powerHandler, this.unbreaking, this.fortune, this.silktouch, this.efficiency);
		for (Object lt : this.laserTargets)
			ILaserTargetHelper.receiveLaserEnergy(lt, power / this.laserTargets.size());
		for (LaserData laser : this.lasers)
			laser.pushPower(power / this.laserTargets.size());
	}

	protected boolean isValidLaser() {
		if (this.lasers == null) return false;
		for (LaserData laser : this.lasers)
			if (laser == null) return false;
		return true;
	}

	protected boolean isValidTable() {
		if (this.laserTargets.size() == 0) return false;
		for (Object lt : this.laserTargets)
			if (lt == null || !ILaserTargetHelper.isValid(lt)) return false;
		return true;
	}

	protected void findTable() {
		removeLaser();
		int meta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);

		int minX = this.xCoord - 5 * (this.fortune + 1);
		int minY = this.yCoord - 5 * (this.fortune + 1);
		int minZ = this.zCoord - 5 * (this.fortune + 1);
		int maxX = this.xCoord + 5 * (this.fortune + 1);
		int maxY = this.yCoord + 5 * (this.fortune + 1);
		int maxZ = this.zCoord + 5 * (this.fortune + 1);

		switch (ForgeDirection.values()[meta]) {
		case WEST:
			maxX = this.xCoord;
			break;
		case EAST:
			minX = this.xCoord;
			break;
		case DOWN:
			maxY = this.yCoord;
			break;
		case UP:
			minY = this.yCoord;
			break;
		case NORTH:
			maxZ = this.zCoord;
			break;
		default:
		case SOUTH:
			minZ = this.zCoord;
			break;
		}

		this.laserTargets.clear();

		for (int x = minX; x <= maxX; ++x) {
			for (int y = minY; y <= maxY; ++y) {
				for (int z = minZ; z <= maxZ; ++z) {
					TileEntity tile = this.worldObj.getTileEntity(x, y, z);
					if (ILaserTargetHelper.isInstance(tile)) {
						if (ILaserTargetHelper.isValid(tile)) {
							this.laserTargets.add(tile);
						}
					}
				}
			}
		}
		if (this.laserTargets.isEmpty()) return;
		if (!this.silktouch) {
			Object laserTarget = this.laserTargets.get(this.worldObj.rand.nextInt(this.laserTargets.size()));
			this.laserTargets.clear();
			this.laserTargets.add(laserTarget);
		}
		this.lasers = new LaserData[this.laserTargets.size()];
	}

	protected void removeLaser() {
		if (this.lasers != null) for (int i = 0; i < this.lasers.length; i++)
			if (this.lasers[i] != null) {
				this.lasers[i].isVisible = false;
				this.lasers[i] = null;
			}
	}

	@Override
	public PowerReceiver getPowerReceiver(ForgeDirection side) {
		return this.powerHandler.getPowerReceiver();
	}

	@Override
	public void doWork(PowerHandler workProvider) {}

	@Override
	public void readFromNBT(NBTTagCompound nbttc) {
		super.readFromNBT(nbttc);
		this.fortune = nbttc.getByte("fortune");
		this.efficiency = nbttc.getByte("efficiency");
		this.unbreaking = nbttc.getByte("unbreaking");
		this.silktouch = nbttc.getBoolean("silktouch");
		this.powerHandler.readFromNBT(nbttc);
		PowerManager.configureL(this.powerHandler, this.efficiency, this.unbreaking);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttc) {
		super.writeToNBT(nbttc);
		nbttc.setByte("fortune", this.fortune);
		nbttc.setByte("efficiency", this.efficiency);
		nbttc.setByte("unbreaking", this.unbreaking);
		nbttc.setBoolean("silktouch", this.silktouch);
		this.powerHandler.writeToNBT(nbttc);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		removeLaser();
	}

	@Override
	public boolean isActive() {
		return isValidTable();
	}

	@Override
	public boolean manageFluids() {
		return false;
	}

	@Override
	public boolean manageSolids() {
		return false;
	}

	@Override
	public boolean allowAction(IAction action) {
		return action == actionOn || action == actionOff;
	}

	@Override
	public void actionActivated(IAction action) {
		if (action == actionOn) {
			this.lastMode = ActionMachineControl.Mode.On;
		} else if (action == actionOff) {
			this.lastMode = ActionMachineControl.Mode.Off;
		}
	}

	@Override
	public World getWorld() {
		return this.worldObj;
	}

	@Override
	public byte getEfficiency() {
		return this.efficiency;
	}

	@Override
	public byte getFortune() {
		return this.fortune;
	}

	@Override
	public byte getUnbreaking() {
		return this.unbreaking;
	}

	@Override
	public boolean getSilktouch() {
		return this.silktouch;
	}

	@Override
	public void set(byte pefficiency, byte pfortune, byte punbreaking, boolean psilktouch) {
		this.efficiency = pefficiency;
		this.fortune = pfortune;
		this.unbreaking = punbreaking;
		this.silktouch = psilktouch;
	}

	@Override
	public void G_reinit() {
		PowerManager.configureL(this.powerHandler, this.efficiency, this.unbreaking);
	}
}
