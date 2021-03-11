package com.minecraftabnormals.extraboats.common.entity.item.boat;

import com.minecraftabnormals.extraboats.core.BoatHelper;
import com.minecraftabnormals.extraboats.core.registry.ExtraBoatsEntities;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import net.minecraftforge.fml.network.FMLPlayMessages;

import javax.annotation.Nullable;

public class FurnaceBoatEntity extends ExtraBoatsBoatEntity {
	private static final DataParameter<Integer> FUEL = EntityDataManager.createKey(FurnaceBoatEntity.class, DataSerializers.VARINT);
	private static final Ingredient FUEL_ITEMS = Ingredient.fromItems(Items.COAL, Items.CHARCOAL);

	public FurnaceBoatEntity(EntityType<? extends BoatEntity> entityType, World worldIn) {
		super(entityType, worldIn);
	}

	public FurnaceBoatEntity(World worldIn, double x, double y, double z) {
		this(ExtraBoatsEntities.FURNACE_BOAT.get(), worldIn);
		this.setPosition(x, y, z);
		this.setMotion(Vector3d.ZERO);
		this.prevPosX = x;
		this.prevPosY = y;
		this.prevPosZ = z;
	}

	public FurnaceBoatEntity(FMLPlayMessages.SpawnEntity packet, World worldIn) {
		super(ExtraBoatsEntities.FURNACE_BOAT.get(), worldIn);
	}

	@Override
	protected void registerData() {
		super.registerData();
		this.dataManager.register(FUEL, 0);
	}

	@Override
	protected void dropBreakItems() {
		super.dropBreakItems();
		this.entityDropItem(Blocks.FURNACE);
	}

	@Override
	public void killBoat(DamageSource source) {
		super.killBoat(source);
		this.entityDropItem(Blocks.FURNACE);
	}

	@Override
	public ActionResultType processInitialInteract(PlayerEntity player, Hand hand) {
		if (player.isSneaking()) {
			ItemStack itemstack = player.getHeldItem(hand);
			if (FUEL_ITEMS.test(itemstack) && this.getFuel() + 3600 <= 32000) {
				if (!player.abilities.isCreativeMode) {
					itemstack.shrink(1);
				}

				this.setFuel(this.getFuel() + 3600);
			}

			return ActionResultType.func_233537_a_(this.world.isRemote);
		} else {
			return super.processInitialInteract(player, hand);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getFuel() > 0) {
			this.setFuel(this.getFuel() - 1);

			float f = (this.rotationYaw - 90.0F) * ((float) Math.PI / 180F);
			float f1 = MathHelper.cos(f);
			float f2 = MathHelper.sin(f);

			if (this.world.isRemote && this.rand.nextInt(4) == 0) {
				this.world.addParticle(ParticleTypes.LARGE_SMOKE, this.getPosX() + (double) f1 * 0.5D, this.getPosY() + 1.0D, this.getPosZ() + (double) f2 * 0.5D, 0.0D, 0.0D, 0.0D);
			}

			if (this.rand.nextInt(40) == 0) {
				this.world.playSound(this.getPosX() + (double) f1 * 0.5D, this.getPosY(), this.getPosZ() + (double) f2 * 0.5D, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, this.getSoundCategory(), 1.0F, 1.0F, false);
			}
		}
	}

	@Override
	protected void controlBoat() {
		if (this.isBeingRidden()) {
			float f = 0.0F;
			if (this.leftInputDown) {
				--this.deltaRotation;
			}

			if (this.rightInputDown) {
				++this.deltaRotation;
			}

			if (this.rightInputDown != this.leftInputDown && !this.forwardInputDown && !this.backInputDown) {
				f += 0.005F;
			}

			this.rotationYaw += this.deltaRotation;
			if (this.forwardInputDown) {
				f += 0.04F;
			}

			if (this.backInputDown) {
				f -= 0.021F;
			}

			if (this.getFuel() > 0) {
				f += 0.026F;
			}

			this.setMotion(this.getMotion().add((double) (MathHelper.sin(-this.rotationYaw * ((float) Math.PI / 180F)) * f), 0.0D, (double) (MathHelper.cos(this.rotationYaw * ((float) Math.PI / 180F)) * f)));
			this.setPaddleState(this.rightInputDown && !this.leftInputDown || this.forwardInputDown, this.leftInputDown && !this.rightInputDown || this.forwardInputDown);

			if (this.getFuel() > 0 && this.status == BoatEntity.Status.IN_WATER) {
				float f1 = (this.rotationYaw - 90.0F) * ((float) Math.PI / 180F);
				float f2 = MathHelper.cos(f1);
				float f3 = MathHelper.sin(f1);
				for (int i = 0; i < 10; ++i) {
					this.world.addParticle(ParticleTypes.SPLASH, this.getPosX() + (double) f2 * 0.8D + (this.rand.nextDouble() - 0.5D), this.getPosY() + 0.2F, this.getPosZ() + (double) f3 * 0.8D + (this.rand.nextDouble() - 0.5D), 0.0D, 0.05D, 0.0D);
				}
			}
		}
	}

	@Override
	protected void writeAdditional(CompoundNBT compound) {
		super.writeAdditional(compound);
		compound.putInt("Fuel", this.getFuel());
	}

	@Override
	protected void readAdditional(CompoundNBT compound) {
		super.readAdditional(compound);
		this.setFuel(compound.getInt("Fuel"));
	}

	private void setFuel(@Nullable int fuel) {
		this.dataManager.set(FUEL, fuel);
	}

	@Nullable
	public int getFuel() {
		return this.dataManager.get(FUEL);
	}

	@Override
	public Item getItemBoat() {
		return BoatHelper.getFurnaceBoatItem(this.getModBoatType());
	}

	@Override
	public Item getItemDropBoat() {
		return BoatHelper.getBoatItem(this.getModBoatType());
	}

	@Override
	public BlockState getDisplayTile() {
		return Blocks.FURNACE.getDefaultState().with(FurnaceBlock.FACING, Direction.SOUTH).with(FurnaceBlock.LIT, Boolean.valueOf(this.getFuel() > 0));
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