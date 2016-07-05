package com.github.alexthe666.iceandfire.entity;

import java.util.Random;

import javax.annotation.Nullable;

import net.ilexiconn.llibrary.server.animation.Animation;
import net.ilexiconn.llibrary.server.animation.AnimationHandler;
import net.ilexiconn.llibrary.server.animation.IAnimatedEntity;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.AnimalChest;
import net.minecraft.inventory.IInventoryChangedListener;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.core.ModItems;
import com.github.alexthe666.iceandfire.message.MessageDaytime;
import com.github.alexthe666.iceandfire.message.MessageDragonArmor;

import fossilsarcheology.api.EnumDiet;
import fossilsarcheology.api.FoodMappings;

public abstract class EntityDragonBase extends EntityTameable implements IAnimatedEntity, IInventoryChangedListener {
	public double minimumDamage;
	public double maximumDamage;
	public double minimumHealth;
	public double maximumHealth;
	public double minimumSpeed;
	public double maximumSpeed;
	public EnumDiet diet;
	private boolean isSleeping;
	public float sleepProgress;
	private boolean isSitting;
	private boolean isHovering;
	public float hoverProgress;
	private boolean isFlying;
	public float flyProgress;
	private boolean isBreathingFire;
	public float fireBreathProgress;
	private int fireTicks;
	private int hoverTicks;
	public int flyTicks;
	private static final DataParameter<Integer> HUNGER = EntityDataManager.<Integer> createKey(EntityDragonBase.class, DataSerializers.VARINT);
	private static final DataParameter<Integer> AGE_TICKS = EntityDataManager.<Integer> createKey(EntityDragonBase.class, DataSerializers.VARINT);
	private static final DataParameter<Boolean> GENDER = EntityDataManager.<Boolean> createKey(EntityDragonBase.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Integer> VARIANT = EntityDataManager.<Integer> createKey(EntityDragonBase.class, DataSerializers.VARINT);
	private static final DataParameter<Boolean> SLEEPING = EntityDataManager.<Boolean> createKey(EntityDragonBase.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> FIREBREATHING = EntityDataManager.<Boolean> createKey(EntityDragonBase.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> HOVERING = EntityDataManager.<Boolean> createKey(EntityDragonBase.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> FLYING = EntityDataManager.<Boolean> createKey(EntityDragonBase.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Integer> HEAD_ARMOR = EntityDataManager.<Integer> createKey(EntityDragonBase.class, DataSerializers.VARINT);
	private static final DataParameter<Integer> NECK_ARMOR = EntityDataManager.<Integer> createKey(EntityDragonBase.class, DataSerializers.VARINT);
	private static final DataParameter<Integer> BODY_ARMOR = EntityDataManager.<Integer> createKey(EntityDragonBase.class, DataSerializers.VARINT);
	private static final DataParameter<Integer> TAIL_ARMOR = EntityDataManager.<Integer> createKey(EntityDragonBase.class, DataSerializers.VARINT);
	public AnimalChest dragonInv;
	private int animationTick;
	private Animation currentAnimation;
	protected float minimumSize;
	protected float maximumSize;
	public boolean isDaytime;
	public static Animation ANIMATION_EAT;
	public static Animation ANIMATION_SPEAK;
	public static Animation ANIMATION_BITE;
	public static Animation ANIMATION_SHAKEPREY;
	public boolean attackDecision;
	public int animationCycle;
	public BlockPos airTarget;
	public BlockPos homeArea;
	protected int flyHovering;
	@SideOnly(Side.CLIENT)
	public RollBuffer roll_buffer;
	public int spacebarTicks;
	public int spacebarTickCounter;

	public EntityDragonBase(World world, EnumDiet diet, double minimumDamage, double maximumDamage, double minimumHealth, double maximumHealth, double minimumSpeed, double maximumSpeed) {
		super(world);
		this.diet = diet;
		this.minimumDamage = minimumDamage;
		this.maximumDamage = maximumDamage;
		this.minimumHealth = minimumHealth;
		this.maximumHealth = maximumHealth;
		this.minimumSpeed = minimumSpeed;
		this.maximumSpeed = maximumSpeed;
		ANIMATION_EAT = Animation.create(20);
		updateAttributes();
		initDragonInv();
		if (FMLCommonHandler.instance().getSide().isClient()) {
			roll_buffer = new RollBuffer();
		}
	}

	private void initDragonInv() {
		AnimalChest animalchest = this.dragonInv;
		this.dragonInv = new AnimalChest("dragonInv", 4);
		this.dragonInv.setCustomName(this.getName());
		if (animalchest != null) {
			animalchest.removeInventoryChangeListener(this);
			int i = Math.min(animalchest.getSizeInventory(), this.dragonInv.getSizeInventory());

			for (int j = 0; j < i; ++j) {
				ItemStack itemstack = animalchest.getStackInSlot(j);

				if (itemstack != null) {
					this.dragonInv.setInventorySlotContents(j, itemstack.copy());
				}
			}
		}

		this.dragonInv.addInventoryChangeListener(this);
		this.updateDragonSlots();
		this.itemHandler = new net.minecraftforge.items.wrapper.InvWrapper(this.dragonInv);
		if (worldObj.isRemote) {
			IceAndFire.NETWORK_WRAPPER.sendToServer(new MessageDragonArmor(this.getEntityId(), 0, this.getIntFromArmor(this.dragonInv.getStackInSlot(0))));
			IceAndFire.NETWORK_WRAPPER.sendToServer(new MessageDragonArmor(this.getEntityId(), 1, this.getIntFromArmor(this.dragonInv.getStackInSlot(1))));
			IceAndFire.NETWORK_WRAPPER.sendToServer(new MessageDragonArmor(this.getEntityId(), 2, this.getIntFromArmor(this.dragonInv.getStackInSlot(2))));
			IceAndFire.NETWORK_WRAPPER.sendToServer(new MessageDragonArmor(this.getEntityId(), 3, this.getIntFromArmor(this.dragonInv.getStackInSlot(3))));
		}
	}

	private void updateDragonSlots() {
		if (!this.worldObj.isRemote) {
			this.setArmorInSlot(0, getIntFromArmor(this.dragonInv.getStackInSlot(0)));
			this.setArmorInSlot(1, getIntFromArmor(this.dragonInv.getStackInSlot(1)));
			this.setArmorInSlot(2, getIntFromArmor(this.dragonInv.getStackInSlot(2)));
			this.setArmorInSlot(3, getIntFromArmor(this.dragonInv.getStackInSlot(3)));
		}
	}

	public void openGUI(EntityPlayer playerEntity) {
		if (!this.worldObj.isRemote && (!this.isBeingRidden() || this.isPassenger(playerEntity))) {
			playerEntity.openGui(IceAndFire.instance, 0, this.worldObj, this.getEntityId(), 0, 0);
		}
	}

	public void onDeath(DamageSource cause) {
		super.onDeath(cause);
		if (dragonInv != null && !this.worldObj.isRemote) {
			for (int i = 0; i < dragonInv.getSizeInventory(); ++i) {
				ItemStack itemstack = dragonInv.getStackInSlot(i);
				if (itemstack != null) {
					this.entityDropItem(itemstack, 0.0F);
				}
			}
		}
	}

	public int getIntFromArmor(ItemStack stack) {
		if (stack != null && stack.getItem() != null && stack.getItem() == ModItems.dragon_armor_iron) {
			return 1;
		}
		if (stack != null && stack.getItem() != null && stack.getItem() == ModItems.dragon_armor_gold) {
			return 2;
		}
		if (stack != null && stack.getItem() != null && stack.getItem() == ModItems.dragon_armor_diamond) {
			return 3;
		}
		return 0;
	}

	@Override
	public boolean isAIDisabled() {
		return false;
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataManager.register(HUNGER, Integer.valueOf(0));
		this.dataManager.register(AGE_TICKS, Integer.valueOf(0));
		this.dataManager.register(GENDER, Boolean.valueOf(false));
		this.dataManager.register(VARIANT, Integer.valueOf(0));
		this.dataManager.register(SLEEPING, Boolean.valueOf(false));
		this.dataManager.register(FIREBREATHING, Boolean.valueOf(false));
		this.dataManager.register(HOVERING, Boolean.valueOf(false));
		this.dataManager.register(FLYING, Boolean.valueOf(false));
		this.dataManager.register(HEAD_ARMOR, Integer.valueOf(0));
		this.dataManager.register(NECK_ARMOR, Integer.valueOf(0));
		this.dataManager.register(BODY_ARMOR, Integer.valueOf(0));
		this.dataManager.register(TAIL_ARMOR, Integer.valueOf(0));

	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		compound.setInteger("Hunger", this.getHunger());
		compound.setInteger("AgeTicks", this.getAgeInTicks());
		compound.setBoolean("Gender", this.isMale());
		compound.setInteger("Variant", this.getVariant());
		compound.setBoolean("Sleeping", this.isSleeping());
		compound.setBoolean("FireBreathing", this.isBreathingFire());
		compound.setBoolean("AttackDecision", attackDecision);
		compound.setBoolean("Hovering", this.isHovering());
		compound.setBoolean("Flying", this.isFlying());
		compound.setInteger("ArmorHead", this.getArmorInSlot(0));
		compound.setInteger("ArmorNeck", this.getArmorInSlot(1));
		compound.setInteger("ArmorBody", this.getArmorInSlot(2));
		compound.setInteger("ArmorTail", this.getArmorInSlot(3));
		if (dragonInv != null) {
			NBTTagList nbttaglist = new NBTTagList();
			for (int i = 0; i < this.dragonInv.getSizeInventory(); ++i) {
				ItemStack itemstack = this.dragonInv.getStackInSlot(i);
				if (itemstack != null) {
					NBTTagCompound nbttagcompound = new NBTTagCompound();
					nbttagcompound.setByte("Slot", (byte) i);
					itemstack.writeToNBT(nbttagcompound);
					nbttaglist.appendTag(nbttagcompound);
				}
			}
			compound.setTag("Items", nbttaglist);
		}
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		this.setHunger(compound.getInteger("Hunger"));
		this.setAgeInTicks(compound.getInteger("AgeTicks"));
		this.setGender(compound.getBoolean("Gender"));
		this.setVariant(compound.getInteger("Variant"));
		this.setSleeping(compound.getBoolean("Sleeping"));
		this.setBreathingFire(compound.getBoolean("FireBreathing"));
		this.attackDecision = compound.getBoolean("AttackDecision");
		this.setHovering(compound.getBoolean("Hovering"));
		this.setFlying(compound.getBoolean("Flying"));
		this.setArmorInSlot(0, compound.getInteger("ArmorHead"));
		this.setArmorInSlot(1, compound.getInteger("ArmorNeck"));
		this.setArmorInSlot(2, compound.getInteger("ArmorBody"));
		this.setArmorInSlot(3, compound.getInteger("ArmorTail"));
		if (dragonInv != null) {
			NBTTagList nbttaglist = compound.getTagList("Items", 10);
			this.initDragonInv();
			for (int i = 0; i < nbttaglist.tagCount(); ++i) {
				NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
				int j = nbttagcompound.getByte("Slot") & 255;
				if (j >= 2 && j < this.dragonInv.getSizeInventory()) {
					this.dragonInv.setInventorySlotContents(j, ItemStack.loadItemStackFromNBT(nbttagcompound));
				}
			}
		}

	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(500.0D);
		getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
		getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(1.0D);
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);

	}

	private void updateAttributes() {
		double healthStep = (maximumHealth - minimumHealth) / (125);
		double attackStep = (maximumDamage - minimumDamage) / (125);
		double speedStep = (maximumSpeed - minimumSpeed) / (125);
		if (this.getAgeInDays() <= 125) {
			this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(Math.round(minimumHealth + (healthStep * this.getAgeInDays())));
			this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(Math.round(minimumDamage + (attackStep * this.getAgeInDays())));
			this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(minimumSpeed + (speedStep * this.getAgeInDays()));

		}
	}

	public int getHunger() {
		return this.dataManager.get(HUNGER).intValue();
	}

	public void setHunger(int hunger) {
		this.dataManager.set(HUNGER, Integer.valueOf(Math.min(100, hunger)));
	}

	public int getVariant() {
		return this.dataManager.get(VARIANT).intValue();
	}

	public void setVariant(int variant) {
		this.dataManager.set(VARIANT, Integer.valueOf(variant));
	}

	public int getAgeInDays() {
		return this.dataManager.get(AGE_TICKS).intValue() / 24000;
	}

	public void setAgeInDays(int age) {
		this.dataManager.set(AGE_TICKS, Integer.valueOf(age * 24000));
	}

	public int getAgeInTicks() {
		return this.dataManager.get(AGE_TICKS).intValue();
	}

	public void setAgeInTicks(int age) {
		this.dataManager.set(AGE_TICKS, Integer.valueOf(age));
	}

	public boolean isMale() {
		return this.dataManager.get(GENDER).booleanValue();
	}

	public boolean isHovering() {
		if (worldObj.isRemote) {
			boolean isHovering = this.dataManager.get(HOVERING).booleanValue();
			this.isHovering = isHovering;
			return isHovering;
		}
		return isHovering;
	}

	public boolean isFlying() {
		if (worldObj.isRemote) {
			boolean isFlying = this.dataManager.get(FLYING).booleanValue();
			this.isFlying = isFlying;
			return isFlying;
		}
		return isFlying;
	}

	public void setGender(boolean male) {
		this.dataManager.set(GENDER, Boolean.valueOf(male));
	}

	public void setSleeping(boolean sleeping) {
		this.dataManager.set(SLEEPING, Boolean.valueOf(sleeping));
		if (!worldObj.isRemote) {
			this.isSleeping = sleeping;
		}
	}

	public void setHovering(boolean hovering) {
		this.dataManager.set(HOVERING, Boolean.valueOf(hovering));
		if (!worldObj.isRemote) {
			this.isHovering = hovering;
		}
	}

	public void setFlying(boolean flying) {
		this.dataManager.set(FLYING, Boolean.valueOf(flying));
		if (!worldObj.isRemote) {
			this.isFlying = flying;
		}
	}

	public boolean isSleeping() {
		if (worldObj.isRemote) {
			boolean isSleeping = this.dataManager.get(SLEEPING).booleanValue();
			this.isSleeping = isSleeping;
			return isSleeping;
		}
		return isSleeping;
	}

	public void setBreathingFire(boolean breathing) {
		this.dataManager.set(FIREBREATHING, Boolean.valueOf(breathing));
		if (!worldObj.isRemote) {
			this.isBreathingFire = breathing;
		}
	}

	public boolean isBreathingFire() {
		if (worldObj.isRemote) {
			boolean breathing = this.dataManager.get(FIREBREATHING).booleanValue();
			this.isBreathingFire = breathing;
			return breathing;
		}
		return isBreathingFire;
	}

	protected boolean canFitPassenger(Entity passenger) {
		return this.getPassengers().size() < 2;
	}

	@Override
	public boolean isSitting() {
		if (worldObj.isRemote) {
			boolean isSitting = (this.dataManager.get(TAMED).byteValue() & 1) != 0;
			this.isSitting = isSitting;
			return isSitting;
		}
		return isSitting;
	}

	@Override
	public void setSitting(boolean sitting) {
		super.setSitting(sitting);
		if (!worldObj.isRemote) {
			this.isSitting = sitting;
		}
	}

	public int getArmorInSlot(int i) {
		switch (i) {
		default:
			return this.dataManager.get(HEAD_ARMOR).intValue();
		case 1:
			return this.dataManager.get(NECK_ARMOR).intValue();
		case 2:
			return this.dataManager.get(BODY_ARMOR).intValue();
		case 3:
			return this.dataManager.get(TAIL_ARMOR).intValue();
		}
	}

	public void setArmorInSlot(int i, int armorType) {
		switch (i) {
		case 0:
			this.dataManager.set(HEAD_ARMOR, Integer.valueOf(armorType));
			break;
		case 1:
			this.dataManager.set(NECK_ARMOR, Integer.valueOf(armorType));
			break;
		case 2:
			this.dataManager.set(BODY_ARMOR, Integer.valueOf(armorType));
			break;
		case 3:
			this.dataManager.set(TAIL_ARMOR, Integer.valueOf(armorType));
			break;
		}
	}

	public boolean canMove() {
		return !this.isSitting() && !this.isSleeping() && !(!this.getPassengers().isEmpty() && this.getOwner() != null && this.getPassengers().contains(this.getOwner()));
	}

	@Override
	public boolean processInteract(EntityPlayer player, EnumHand hand, @Nullable ItemStack stack) {
		this.setTamed(true);
		this.setOwnerId(player.getUniqueID());
		player.startRiding(this);
		if (stack != null) {
			if (stack.getItem() != null) {
				int itemFoodAmount = FoodMappings.instance().getItemFoodAmount(stack.getItem(), diet);
				if (itemFoodAmount > 0 && this.getHunger() < 100) {
					this.growDragon(1);
					this.setHunger(this.getHunger() + itemFoodAmount);
					this.setHealth(Math.min(this.getMaxHealth(), (int) (this.getHealth() + (itemFoodAmount / 10))));
					this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
					this.spawnItemCrackParticles(stack.getItem());
					this.eatFoodBonus(stack);
					if (!player.isCreative()) {
						stack.stackSize--;
					}
					return true;
				}
			}
		} else {
			this.openGUI(player);
		}
		return super.processInteract(player, hand, stack);
	}

	public void eatFoodBonus(ItemStack stack) {

	}

	public void growDragon(int ageInDays) {
		this.setAgeInDays(this.getAgeInDays() + ageInDays);
		this.setScaleForAge(false);
		this.updateAttributes();
	}

	public void spawnItemCrackParticles(Item item) {
		double motionX = getRNG().nextGaussian() * 0.07D;
		double motionY = getRNG().nextGaussian() * 0.07D;
		double motionZ = getRNG().nextGaussian() * 0.07D;
		float f = (float) (getRNG().nextFloat() * (this.getEntityBoundingBox().maxX - this.getEntityBoundingBox().minX) + this.getEntityBoundingBox().minX);
		float f1 = (float) (getRNG().nextFloat() * (this.getEntityBoundingBox().maxY - this.getEntityBoundingBox().minY) + this.getEntityBoundingBox().minY);
		float f2 = (float) (getRNG().nextFloat() * (this.getEntityBoundingBox().maxZ - this.getEntityBoundingBox().minZ) + this.getEntityBoundingBox().minZ);
		this.worldObj.spawnParticle(EnumParticleTypes.ITEM_CRACK, f, f1, f2, motionX, motionY, motionZ, new int[] { Item.getIdFromItem(item) });
	}

	public boolean isDaytime() {
		if (!this.firstUpdate && this.worldObj != null) {
			if (worldObj.isRemote) {
				return isDaytime;
			} else {
				IceAndFire.NETWORK_WRAPPER.sendToAll(new MessageDaytime(this.getEntityId(), this.worldObj.isDaytime()));
				return this.worldObj.isDaytime();
			}
		} else {
			return true;
		}
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (this.isFlying() || this.isHovering()) {
			if (animationCycle < 15) {
				animationCycle++;
			} else {
				animationCycle = 0;
			}
			if (animationCycle > 12 && animationCycle < 15) {
				for (int i = 0; i < this.getRenderSize(); i++) {
					for (int i1 = 0; i1 < 20; i1++) {
						double motionX = getRNG().nextGaussian() * 0.07D;
						double motionY = getRNG().nextGaussian() * 0.07D;
						double motionZ = getRNG().nextGaussian() * 0.07D;
						float radius = 0.75F * (0.7F * getRenderSize()) * -3;
						float angle = (0.01745329251F * this.renderYawOffset) + i1 * 1F;
						double extraX = (double) (radius * MathHelper.sin((float) (Math.PI + angle)));
						double extraZ = (double) (radius * MathHelper.cos(angle));
						double extraY = 0.8F;

						IBlockState iblockstate = this.worldObj.getBlockState(new BlockPos(MathHelper.floor_double(this.posX + extraX), MathHelper.floor_double(this.posY + extraY) - 1, MathHelper.floor_double(this.posZ + extraZ)));
						if (iblockstate.getMaterial() != Material.AIR) {
							worldObj.spawnParticle(EnumParticleTypes.BLOCK_CRACK, true, this.posX + extraX, this.posY + extraY, this.posZ + extraZ, motionX, motionY, motionZ, new int[] { Block.getStateId(iblockstate) });
						}
					}
				}
			}
		}
		boolean sleeping = isSleeping();
		if (sleeping && sleepProgress < 20.0F) {
			sleepProgress += 0.5F;
		} else if (!sleeping && sleepProgress > 0.0F) {
			sleepProgress -= 0.5F;
		}
		boolean fireBreathing = isBreathingFire();
		if (fireBreathing && fireBreathProgress < 20.0F) {
			fireBreathProgress += 0.5F;
		} else if (!fireBreathing && fireBreathProgress > 0.0F) {
			fireBreathProgress -= 0.5F;
		}
		boolean hovering = isHovering();
		if (hovering && hoverProgress < 20.0F) {
			hoverProgress += 0.5F;
		} else if (!hovering && hoverProgress > 0.0F) {
			hoverProgress -= 0.5F;
		}
		boolean flying = isFlying();
		if (flying && flyProgress < 20.0F) {
			flyProgress += 0.5F;
		} else if (!flying && flyProgress > 0.0F) {
			flyProgress -= 0.5F;
		}
		if (this.onGround && this.doesWantToLand() && (this.isFlying() || this.isHovering())) {
			this.setHovering(false);
			this.setFlying(false);
		}
		if (this.isHovering()) {
			this.hoverTicks++;
			if (this.hoverTicks > 40 && flyHovering == 0) {
				if (!this.isChild()) {
					this.setFlying(true);
				}
				this.setHovering(false);
				this.flyHovering = 0;
				this.hoverTicks = 0;
			}
			if (flyHovering == 0) {
				// move upwards
			}
			if (flyHovering == 1) {
				// move down
			}
			if (flyHovering == 2) {
				this.motionY *= 0;
				// stay still
			}
		}
		if (!this.onGround && this.motionY < 0.0D || this.isHovering() || this.isFlying()) {
			this.motionY *= 0.6D;
		}
		if (this.isFlying() && getAttackTarget() == null) {
			flyAround();
		} else if (getAttackTarget() != null) {
			flyTowardsTarget();
		}
		if (this.isFlying()) {
			this.flyTicks++;
		}
		if ((this.isHovering() || this.isFlying()) && this.isSleeping()) {
			this.setFlying(false);
			this.setHovering(false);
		}
		if (this.getRNG().nextInt(4000) == 0 && !this.isFlying() && this.getPassengers().isEmpty() && !this.isChild() && !this.isHovering() && !this.isSleeping() && !this.isSitting() && !this.doesWantToLand()) {
			this.setSleeping(false);
			this.setHovering(true);
			this.setSitting(false);
			this.flyHovering = 0;
			this.flyTicks = 0;
		}
		if(getAttackTarget() != null && !this.getPassengers().isEmpty() && this.getOwner() != null && this.getPassengers().contains(this.getOwner())){
			this.setAttackTarget(null);
		}
		AnimationHandler.INSTANCE.updateAnimations(this);
		this.setAgeInTicks(this.getAgeInTicks() + 1);
		if (this.getAgeInTicks() % 24000 == 0) {
			this.updateAttributes();
			this.setScale(this.getRenderSize());
		}
		if (this.getAgeInTicks() % 1200 == 0) {
			if (this.getHunger() > 0) {
				this.setHunger(this.getHunger() - 1);
			}
		}
		if (this.isBreathingFire()) {
			this.fireTicks++;
			if (fireTicks > (this.isChild() ? 60 : this.isAdult() ? 400 : 180)) {
				this.setBreathingFire(false);
				this.attackDecision = true;
				fireTicks = 0;
			}
		}
	}

	public void fall(float distance, float damageMultiplier) {
	}

	public boolean isActuallyBreathingFire() {
		return this.fireTicks > 20 && this.isBreathingFire();
	}

	public boolean doesWantToLand() {
		return this.flyTicks > 5000;
	}

	public abstract String getVariantName(int variant);

	public abstract String getTexture();

	public abstract String getTextureOverlay();

	public void updatePassenger(Entity passenger) {
		if (this.isPassenger(passenger)) {
			if (passenger instanceof EntityPlayer && this.getAttackTarget() != passenger && this.isOwner((EntityPlayer) passenger)) {
				renderYawOffset = rotationYaw;
				this.rotationYaw = passenger.rotationYaw;
				float radius = 1F * (0.7F * getRenderSize());
				float angle = (0.01745329251F * this.renderYawOffset);
				double extraX = (double) (radius * MathHelper.sin((float) (Math.PI + angle)));
				double extraZ = (double) (radius * MathHelper.cos(angle));
				float bob = (float) (Math.sin(ticksExisted * -0.05) * 1 * 0.25 - 1 * 0.25);

				double extraY = 0.75F * (getRenderSize() + bob);
				passenger.setPosition(this.posX + extraX, this.posY + extraY, this.posZ + extraZ);
				this.stepHeight = 1;
			} else {
				this.updatePreyInMouth(passenger);
			}
		}
	}

	private void updatePreyInMouth(Entity prey) {
		if (this.getAnimation() == this.ANIMATION_SHAKEPREY) {
			if (this.getAnimationTick() > 55 && prey != null) {
				prey.attackEntityFrom(DamageSource.causeMobDamage(this), ((EntityLivingBase) prey).getMaxHealth() * 2);
				this.attackDecision = !this.attackDecision;
				this.onKillEntity((EntityLivingBase) prey);
			}
			prey.setPosition(this.posX, this.posY + this.getMountedYOffset() + prey.getYOffset(), this.posZ);
			float modTick_0 = this.getAnimationTick() - 15;
			float modTick_1 = this.getAnimationTick() > 15 ? 6 * MathHelper.sin((float) (Math.PI + (modTick_0 * 0.3F))) : 0;
			float modTick_2 = this.getAnimationTick() > 15 ? 15 : this.getAnimationTick();
			this.rotationYaw *= 0;
			prey.rotationYaw = this.rotationYaw + this.rotationYawHead + 180;
			rotationYaw = renderYawOffset;
			float radius = 0.75F * (0.7F * getRenderSize()) * -3;
			float angle = (0.01745329251F * this.renderYawOffset) + 3.15F + (modTick_1 * 1.75F) * 0.05F;
			double extraX = (double) (radius * MathHelper.sin((float) (Math.PI + angle)));
			double extraZ = (double) (radius * MathHelper.cos(angle));
			double extraY = 0.8F * (getRenderSize() + (modTick_1 * 0.05) + (modTick_2 * 0.05) - 2);
			prey.setPosition(this.posX + extraX, this.posY + extraY, this.posZ + extraZ);
		} else {
			prey.dismountRidingEntity();
		}
	}

	public int getDragonStage() {
		int age = this.getAgeInDays();
		if (age >= 100) {
			return 5;
		} else if (age >= 75) {
			return 4;
		} else if (age >= 50) {
			return 3;
		} else if (age >= 25) {
			return 2;
		} else {
			return 1;
		}
	}

	public boolean isTeen() {
		return getDragonStage() < 4 && getDragonStage() > 1;
	}

	public boolean isAdult() {
		return getDragonStage() >= 4;
	}

	public boolean isChild() {
		return getDragonStage() < 2;
	}

	@Override
	@Nullable
	public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {
		livingdata = super.onInitialSpawn(difficulty, livingdata);
		this.setGender(this.getRNG().nextBoolean());
		int age = this.getRNG().nextInt(80) + 1;
		this.growDragon(age);
		this.setHunger(50);
		this.setVariant(new Random().nextInt(4));
		this.setSleeping(false);
		this.updateAttributes();
		return livingdata;
	}

	@Override
	public boolean attackEntityFrom(DamageSource dmg, float i) {
		if (i > 0) {
			this.setSitting(false);
			this.setSleeping(false);
		}
		return super.attackEntityFrom(dmg, i);
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		roll_buffer.calculateChainFlapBuffer(50, 10, 4, this);
		if (this.getAttackTarget() != null && this.getRidingEntity() == null && this.getAttackTarget().isDead) {
			this.setAttackTarget(null);
		}
		if (!this.isInWater() && !this.isSleeping() && !this.isDaytime() && this.getRNG().nextInt(250) == 0 && this.getAttackTarget() == null && this.getPassengers().isEmpty()) {
			this.setSleeping(true);
		}
		if (this.isSleeping() && (this.isInWater() || this.isDaytime() || this.getAttackTarget() != null && !this.getPassengers().isEmpty())) {
			this.setSleeping(false);
		}
	}

	@Override
	public void setScaleForAge(boolean par1) {
		this.setScale(Math.min(this.getRenderSize(), maximumSize));
	}

	public float getRenderSize() {
		float step = (this.maximumSize - this.minimumSize) / ((125 * 24000));

		if (this.getAgeInTicks() > 125 * 24000) {
			return this.minimumSize + ((step) * 125 * 24000);
		}
		return this.minimumSize + ((step * this.getAgeInTicks()));
	}

	@Override
	public boolean attackEntityAsMob(Entity entityIn) {
		boolean flag = entityIn.attackEntityFrom(DamageSource.causeMobDamage(this), ((int) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue()));

		if (flag) {
			this.applyEnchantments(this, entityIn);
		}

		return flag;
	}

	@Override
	public int getAnimationTick() {
		return animationTick;
	}

	@Override
	public void setAnimationTick(int tick) {
		animationTick = tick;
	}

	@Override
	public Animation getAnimation() {
		return currentAnimation;
	}

	@Override
	public void setAnimation(Animation animation) {
		currentAnimation = animation;
	}

	public void playLivingSound() {
		if (!this.isSleeping()) {
			if (this.getAnimation() == this.NO_ANIMATION) {
				this.setAnimation(ANIMATION_SPEAK);
			}
			super.playLivingSound();
		}
	}

	protected void playHurtSound(DamageSource source) {
		if (this.getAnimation() == this.NO_ANIMATION) {
			this.setAnimation(ANIMATION_SPEAK);
		}
		super.playHurtSound(source);
	}

	@Override
	public Animation[] getAnimations() {
		return new Animation[] { IAnimatedEntity.NO_ANIMATION, EntityDragonBase.ANIMATION_EAT };
	}

	@Override
	public EntityAgeable createChild(EntityAgeable ageable) {
		return null;
	}

	public void flyAround() {
		if (airTarget != null) {
			if (!isTargetInAir() || getDistanceSquared(new Vec3d(airTarget.getX(), airTarget.getY(), airTarget.getZ())) < 4 || flyTicks > 6000) {
				airTarget = null;
			}
			flyTowardsTarget();
		}
	}

	public void flyTowardsTarget() {
		if (airTarget != null && isTargetInAir() && this.isFlying() && this.getDistanceSquared(new Vec3d(airTarget.getX(), this.posY, airTarget.getZ())) > 3) {
			double targetX = airTarget.getX() + 0.5D - posX;
			double targetY = airTarget.getY() + 1D - posY;
			double targetZ = airTarget.getZ() + 0.5D - posZ;
			motionX += (Math.signum(targetX) * 0.5D - motionX) * 0.100000000372529 * getFlySpeed();
			motionY += (Math.signum(targetY) * 0.5D - motionY) * 0.100000000372529 * getFlySpeed();
			motionZ += (Math.signum(targetZ) * 0.5D - motionZ) * 0.100000000372529 * getFlySpeed();
			float angle = (float) (Math.atan2(motionZ, motionX) * 180.0D / Math.PI) - 90.0F;
			float rotation = MathHelper.wrapDegrees(angle - rotationYaw);
			moveForward = 0.5F;
			prevRotationYaw = rotationYaw;
			rotationYaw += rotation;
		} else {
			this.airTarget = null;
		}
		if (airTarget != null && isTargetInAir() && this.isFlying() && this.getDistanceSquared(new Vec3d(airTarget.getX(), this.posY, airTarget.getZ())) < 3 && this.doesWantToLand()) {
			this.setFlying(false);
			this.setHovering(true);
			this.flyHovering = 1;
		}
	}

	private double getFlySpeed() {
		return 2;
	}

	protected boolean isTargetInAir() {
		return airTarget != null && (worldObj.getBlockState(airTarget).getMaterial() == Material.AIR && worldObj.getBlockState(airTarget).getMaterial() == Material.AIR);
	}

	public float getDistanceSquared(Vec3d vec3d) {
		float f = (float) (this.posX - vec3d.xCoord);
		float f1 = (float) (this.posY - vec3d.yCoord);
		float f2 = (float) (this.posZ - vec3d.zCoord);
		return f * f + f1 * f1 + f2 * f2;
	}

	private net.minecraftforge.items.IItemHandler itemHandler = null;

	@Override
	public void onInventoryChanged(InventoryBasic invBasic) {
		int dragonArmorHead = this.getArmorInSlot(0);
		int dragonArmorNeck = this.getArmorInSlot(1);
		int dragonArmorBody = this.getArmorInSlot(2);
		int dragonArmorTail = this.getArmorInSlot(3);
		this.updateDragonSlots();
		if (this.ticksExisted > 20) {
			if (dragonArmorHead != this.getIntFromArmor(this.dragonInv.getStackInSlot(0))) {
				this.playSound(SoundEvents.ENTITY_HORSE_ARMOR, 0.5F, 1.0F);
			}
			if (dragonArmorNeck != this.getIntFromArmor(this.dragonInv.getStackInSlot(1))) {
				this.playSound(SoundEvents.ENTITY_HORSE_ARMOR, 0.5F, 1.0F);
			}
			if (dragonArmorBody != this.getIntFromArmor(this.dragonInv.getStackInSlot(2))) {
				this.playSound(SoundEvents.ENTITY_HORSE_ARMOR, 0.5F, 1.0F);
			}
			if (dragonArmorTail != this.getIntFromArmor(this.dragonInv.getStackInSlot(3))) {
				this.playSound(SoundEvents.ENTITY_HORSE_ARMOR, 0.5F, 1.0F);
			}
		}
	}

	public boolean replaceItemInInventory(int inventorySlot, @Nullable ItemStack itemStackIn) {
		int j = inventorySlot - 500 + 2;
		if (j >= 0 && j < this.dragonInv.getSizeInventory()) {
			this.dragonInv.setInventorySlotContents(j, itemStackIn);
			return true;
		} else {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, net.minecraft.util.EnumFacing facing) {
		if (capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return (T) itemHandler;
		return super.getCapability(capability, facing);
	}

	@Override
	public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, net.minecraft.util.EnumFacing facing) {
		return capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}
}
