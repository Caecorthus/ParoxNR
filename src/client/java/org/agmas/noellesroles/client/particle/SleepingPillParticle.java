package org.agmas.noellesroles.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;

/**
 * 与 wathe 毒粒子（骷髅头形状）同款效果，贴图重染色为蓝色；
 * 贴图定义见 assets/noellesroles/particles/sleeping_pill_poison.json。
 */
public class SleepingPillParticle extends SpriteBillboardParticle {
    protected SleepingPillParticle(ClientWorld clientWorld,
                                   double x, double y, double z, double vx, double vy, double vz) {
        super(clientWorld, x, y, z, vx, vy, vz);

        this.maxAge = 16;
        this.scale = 0.2f;
        this.setVelocity(
                vx + (world.getRandom().nextBetween(0, 100) / 4000f) * (world.getRandom().nextBoolean() ? -1 : 1),
                vy,
                vz + (world.getRandom().nextBetween(0, 100) / 4000f) * (world.getRandom().nextBoolean() ? -1 : 1));

        this.red = 1f;
        this.green = 1f;
        this.blue = 1f;
        this.alpha = 1f;

        this.velocityMultiplier = 0.96f;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha = 1.0f - ((float) this.age / this.maxAge);
    }

    public static class Factory<DefaultParticleType extends ParticleEffect> implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            SleepingPillParticle particle = new SleepingPillParticle(world, x, y, z, vx, vy, vz);
            particle.setSprite(this.sprites);
            return particle;
        }
    }
}
