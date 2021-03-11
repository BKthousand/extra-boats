package com.minecraftabnormals.extraboats.common.entity.item.boat;

import com.minecraftabnormals.extraboats.core.BoatHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.monster.piglin.PiglinTasks;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;

public abstract class ContainerBoatEntity extends ExtraBoatsBoatEntity implements IInventory, INamedContainerProvider {
	private NonNullList<ItemStack> boatContainerItems = NonNullList.withSize(36, ItemStack.EMPTY);
	private boolean dropContentsWhenDead = true;
	@Nullable
	private ResourceLocation lootTable;
	private long lootTableSeed;

	public ContainerBoatEntity(EntityType<? extends BoatEntity> entityType, World worldIn) {
		super(entityType, worldIn);
	}

	@Override
	protected void dropBreakItems() {
		super.dropBreakItems();
		if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
			InventoryHelper.dropInventoryItems(this.world, this, this);
		}
	}

	@Override
	public void killBoat(DamageSource source) {
		super.killBoat(source);
		if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
			InventoryHelper.dropInventoryItems(this.world, this, this);
			if (!this.world.isRemote) {
				Entity entity = source.getImmediateSource();
				if (entity != null && entity.getType() == EntityType.PLAYER) {
					PiglinTasks.func_234478_a_((PlayerEntity)entity, true);
				}
			}
		}
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.boatContainerItems) {
			if (!itemstack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getStackInSlot(int index) {
		this.addLoot((PlayerEntity) null);
		return this.boatContainerItems.get(index);
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		this.addLoot((PlayerEntity) null);
		return ItemStackHelper.getAndSplit(this.boatContainerItems, index, count);
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		this.addLoot((PlayerEntity) null);
		ItemStack itemstack = this.boatContainerItems.get(index);
		if (itemstack.isEmpty()) {
			return ItemStack.EMPTY;
		} else {
			this.boatContainerItems.set(index, ItemStack.EMPTY);
			return itemstack;
		}
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		this.addLoot((PlayerEntity) null);
		this.boatContainerItems.set(index, stack);
		if (!stack.isEmpty() && stack.getCount() > this.getInventoryStackLimit()) {
			stack.setCount(this.getInventoryStackLimit());
		}
	}

	@Override
	public boolean replaceItemInInventory(int inventorySlot, ItemStack itemStackIn) {
		if (inventorySlot >= 0 && inventorySlot < this.getSizeInventory()) {
			this.setInventorySlotContents(inventorySlot, itemStackIn);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void markDirty() {
	}

	@Override
	public boolean isUsableByPlayer(PlayerEntity player) {
		if (this.removed) {
			return false;
		} else {
			return !(player.getDistanceSq(this) > 64.0D);
		}
	}

	@Override
	public void remove(boolean keepData) {
		if (!this.world.isRemote && this.dropContentsWhenDead) {
			InventoryHelper.dropInventoryItems(this.world, this, this);
		}
		super.remove(keepData);
		if (!keepData) itemHandler.invalidate();
	}

	@Override
	protected void writeAdditional(CompoundNBT compound) {
		super.writeAdditional(compound);
		if (this.lootTable != null) {
			compound.putString("LootTable", this.lootTable.toString());
			if (this.lootTableSeed != 0L) {
				compound.putLong("LootTableSeed", this.lootTableSeed);
			}
		} else {
			ItemStackHelper.saveAllItems(compound, this.boatContainerItems);
		}
	}

	@Override
	protected void readAdditional(CompoundNBT compound) {
		super.readAdditional(compound);
		this.boatContainerItems = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
		if (compound.contains("LootTable", 8)) {
			this.lootTable = new ResourceLocation(compound.getString("LootTable"));
			this.lootTableSeed = compound.getLong("LootTableSeed");
		} else {
			ItemStackHelper.loadAllItems(compound, this.boatContainerItems);
		}
	}

	@Override
	public ActionResultType processInitialInteract(PlayerEntity player, Hand hand) {
		ActionResultType ret = super.processInitialInteract(player, hand);
		if (player.isSecondaryUseActive()){
			if (ret.isSuccessOrConsume()) return ret;
			player.openContainer(this);
			if (!player.world.isRemote) {
				PiglinTasks.func_234478_a_(player, true);
				return ActionResultType.CONSUME;
			} else {
				return ActionResultType.SUCCESS;
			}
		} else {
			return ret;
		}
	}

	public void addLoot(@Nullable PlayerEntity player) {
		if (this.lootTable != null && this.world.getServer() != null) {
			LootTable loottable = this.world.getServer().getLootTableManager().getLootTableFromLocation(this.lootTable);
			this.lootTable = null;
			LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerWorld) this.world)).withParameter(LootParameters.field_237457_g_, this.getPositionVec()).withSeed(this.lootTableSeed);
			lootcontext$builder.withParameter(LootParameters.KILLER_ENTITY, this);
			if (player != null) {
				lootcontext$builder.withLuck(player.getLuck()).withParameter(LootParameters.THIS_ENTITY, player);
			}

			loottable.fillInventory(this, lootcontext$builder.build(LootParameterSets.CHEST));
		}
	}

	@Override
	public void clear() {
		this.addLoot((PlayerEntity) null);
		this.boatContainerItems.clear();
	}

	public void setLootTable(ResourceLocation lootTableIn, long lootTableSeedIn) {
		this.lootTable = lootTableIn;
		this.lootTableSeed = lootTableSeedIn;
	}

	@Nullable
	@Override
	public Container createMenu(int p_createMenu_1_, PlayerInventory p_createMenu_2_, PlayerEntity p_createMenu_3_) {
		if (this.lootTable != null && p_createMenu_3_.isSpectator()) {
			return null;
		} else {
			this.addLoot(p_createMenu_2_.player);
			return this.createContainer(p_createMenu_1_, p_createMenu_2_);
		}
	}

	protected abstract Container createContainer(int id, PlayerInventory playerInventoryIn);

	private net.minecraftforge.common.util.LazyOptional<?> itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.InvWrapper(this));

	@Override
	public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.util.Direction facing) {
		if (this.isAlive() && capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return itemHandler.cast();
		return super.getCapability(capability, facing);
	}

	public void dropContentsWhenDead(boolean value) {
		this.dropContentsWhenDead = value;
	}

	@Override
	public Item getItemDropBoat() {
		return BoatHelper.getBoatItem(this.getModBoatType());
	}

	@Override
	public void updatePassenger(Entity passenger) {
		if (this.isPassenger(passenger)) {
			float f = passenger instanceof AnimalEntity ? 0.4F : 0.2F;
			float f1 = (float) ((this.removed ? (double) 0.01F : this.getMountedYOffset()) + passenger.getYOffset());

			Vector3d vector3d = (new Vector3d((double) f, 0.0D, 0.0D)).rotateYaw(-this.rotationYaw * ((float) Math.PI / 180F) - ((float) Math.PI / 2F));
			passenger.setPosition(this.getPosX() + vector3d.x, this.getPosY() + (double) f1, this.getPosZ() + vector3d.z);
			passenger.rotationYaw += this.deltaRotation;
			passenger.setRotationYawHead(passenger.getRotationYawHead() + this.deltaRotation);
			this.applyYawToEntity(passenger);
			if (passenger instanceof AnimalEntity) {
				int j = passenger.getEntityId() % 2 == 0 ? 90 : 270;
				passenger.setRenderYawOffset(((AnimalEntity) passenger).renderYawOffset + (float) j);
				passenger.setRotationYawHead(passenger.getRotationYawHead() + (float) j);
			}
		}
	}

	@Override
	protected boolean canFitPassenger(Entity passenger) {
		return !this.isBeingRidden() && !this.areEyesInFluid(FluidTags.WATER);
	}
}