package sh.talonfox.enhancedweather.wind;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import sh.talonfox.enhancedweather.EnhancedWeather;
import sh.talonfox.enhancedweather.network.WindSync;
import sh.talonfox.enhancedweather.weather.weatherevents.Cloud;

import java.io.*;
import java.util.Random;

public class Wind {
    public float AngleGlobal;
    public float SpeedGlobal = 0;
    public float SpeedGlobalChangeRate = 0.05F;
    public int SpeedGlobalRandChangeTimer = 0;
    public int SpeedGlobalRandChangeDelay = 10;

    public float AngleGust = 0;
    public float SpeedGust = 0;
    public int TimeGust = 0;
    public int GustEventTimeRand = 60;

    public float AngleEvent = 0;
    public float SpeedEvent = 0;
    public int TimeEvent = 0;

    public int LowWindTimer = 0;
    public int HighWindTimer = 0;
    private long clientTicks = 0;

    public Wind() {
        Random rand = new Random();
        AngleGlobal = rand.nextInt(360);
    }

    public void tick(MinecraftServer server, int dimid) {
        Random rand = new Random();
        if (LowWindTimer <= 0) {
            if (SpeedGlobalRandChangeTimer-- <= 0) {
                if (HighWindTimer <= 0)
                    SpeedGlobal += (rand.nextDouble() * SpeedGlobalChangeRate) - (SpeedGlobalChangeRate / 2);
                else
                    SpeedGlobal += (rand.nextDouble() * SpeedGlobalChangeRate);
                SpeedGlobalRandChangeTimer = SpeedGlobalRandChangeDelay;
                if (HighWindTimer <= 0)
                    if (rand.nextInt(EnhancedWeather.CONFIG.Wind_LowWindStartChance) == 0) {
                        LowWindTimer = EnhancedWeather.CONFIG.Wind_LowWindDurationBase + rand.nextInt(EnhancedWeather.CONFIG.Wind_LowWindDurationExtra);
                        EnhancedWeather.LOGGER.info("Low Wind for {} ticks", LowWindTimer);
                    } else
                        LowWindTimer = 0;
                if (HighWindTimer <= 0)
                    if (rand.nextInt(EnhancedWeather.CONFIG.Wind_HighWindStartChance) == 0) {
                        HighWindTimer = EnhancedWeather.CONFIG.Wind_HighWindDurationBase + rand.nextInt(EnhancedWeather.CONFIG.Wind_HighWindDurationExtra);
                        EnhancedWeather.LOGGER.info("High Wind for {} ticks", HighWindTimer);
                    }
            }
        } else {
            LowWindTimer--;
            SpeedGlobal -= 0.01F;
        }
        if (HighWindTimer > 0) {
            HighWindTimer--;
        }
        AngleGlobal += (rand.nextFloat()) - (rand.nextFloat());
        if(AngleGlobal < -180)
            AngleGlobal += 360;
        if(AngleGlobal > 180)
            AngleGlobal -= 360;
        if (SpeedGlobal < Math.max(0.00001F,((float) EnhancedWeather.CONFIG.Wind_MinimumSpeed)/100F))
            SpeedGlobal = Math.max(0.00001F,((float) EnhancedWeather.CONFIG.Wind_MinimumSpeed)/100F);
        if (SpeedGlobal > Math.max(0.00001F,((float) EnhancedWeather.CONFIG.Wind_MaximumSpeed)/100F))
            SpeedGlobal = Math.max(0.00001F,((float) EnhancedWeather.CONFIG.Wind_MaximumSpeed)/100F);
        if((server.getTicks() % 40) == 0)
            WindSync.send(server, dimid);
    }

    public void tickClient() {
        Random rand = new Random();
        clientTicks++;
        if (TimeEvent > 0) {
            TimeEvent--;
        }
        if(clientTicks % 10 == 0) {
            Cloud cloud = EnhancedWeather.CLIENT_WEATHER.getClosestCloud(new Vec3d(MinecraftClient.getInstance().player.getX(),200,MinecraftClient.getInstance().player.getZ()),256,false,false,false,true,0);
            if(cloud != null) {
                TimeEvent = 80;
                double var11 = cloud.Position.getX() - MinecraftClient.getInstance().player.getX();
                double var15 = cloud.Position.getZ() - MinecraftClient.getInstance().player.getZ();
                AngleEvent = -((float)Math.atan2(var11, var15)) * 180.0F / (float)Math.PI;
                SpeedEvent = 2F;
            }
            cloud = EnhancedWeather.CLIENT_WEATHER.getClosestCloud(new Vec3d(MinecraftClient.getInstance().player.getX(),200,MinecraftClient.getInstance().player.getZ()),256,false,false,true,false,0);
            if(cloud != null) {
                TimeEvent = 80;
                AngleEvent = cloud.Angle;
                SpeedEvent = 2F;
            }
        }
        if (this.TimeGust == 0) {
            SpeedGust = 0;
            AngleGust = 0;
        }
        if (rand.nextInt((int)(100 - 0.5F)) == 0 && HighWindTimer > 0)  {
            SpeedGust = SpeedGlobal + rand.nextFloat() * 0.6F;
            AngleGust = AngleGlobal + rand.nextInt(120) - 60;
            TimeGust = rand.nextInt(GustEventTimeRand);
        }
        if (TimeGust > 0) {
            TimeGust--;
        }
    }

    public Vec3d ApplyWindForce(Vec3d motion, float weight, float multiplier, float maxSpeed) {
        float windSpeed = (TimeEvent > 0)?SpeedEvent:((TimeGust > 0)?SpeedGust:SpeedGlobal);
        float windAngle = (TimeEvent > 0)?AngleEvent:((TimeGust > 0)?AngleGust:AngleGlobal);

        float windX = (float) -Math.sin(Math.toRadians(windAngle)) * windSpeed;
        float windZ = (float) Math.cos(Math.toRadians(windAngle)) * windSpeed;

        float objX = (float) motion.x;
        float objZ = (float) motion.z;

        float windWeight = 1F;
        float objWeight = weight;

        if (objWeight <= 0) {
            objWeight = 0.001F;
        }

        float weightDiff = windWeight / objWeight;

        float vecX = (objX - windX) * weightDiff;
        float vecZ = (objZ - windZ) * weightDiff;

        vecX *= multiplier;
        vecZ *= multiplier;

        Vec3d newMotion = motion;

        double speedCheck = (Math.abs(vecX) + Math.abs(vecZ)) / 2D;
        if (speedCheck < maxSpeed) {
            newMotion = new Vec3d(objX - vecX, motion.y, objZ - vecZ);
        } else {
            float speedDampen = (float)(maxSpeed / speedCheck);
            newMotion = new Vec3d(objX - vecX*speedDampen, motion.y, objZ - vecZ*speedDampen);
        }

        return newMotion;
    }

    public void save(MinecraftServer server, int dimid) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("AngleGlobal", new JsonPrimitive(AngleGlobal));
        jsonObject.put("SpeedGlobal", new JsonPrimitive(SpeedGlobal));
        jsonObject.put("SpeedGlobalRandChangeTimer", new JsonPrimitive(SpeedGlobalRandChangeTimer));
        jsonObject.put("LowWindTimer", new JsonPrimitive(LowWindTimer));
        jsonObject.put("HighWindTimer", new JsonPrimitive(HighWindTimer));
        String data = jsonObject.toJson(true,true);
        File file = new File(server.getSavePath(WorldSavePath.ROOT).toAbsolutePath() + "/enhancedweather/Wind_DIM"+dimid+".json5");
        try {
            new File(file.getParent()).mkdir();
            file.delete();
            file.createNewFile();
            FileWriter stream = new FileWriter(file);
            stream.write(data);
            stream.close();
        } catch (Exception e) {
            EnhancedWeather.LOGGER.error("Failed to save Wind Data for Dimension #"+dimid);
            EnhancedWeather.LOGGER.error("Reason: "+e.toString());
        }
    }
    public void load(MinecraftServer server, int dimid) {
        File file = new File(server.getSavePath(WorldSavePath.ROOT).toAbsolutePath() + "/enhancedweather/Wind_DIM"+dimid+".json5");
        if(file.exists() && file.isFile()) {
            try {
                JsonObject jsonObject = Jankson.builder().build().load(file);
                AngleGlobal = jsonObject.getFloat("AngleGlobal",0);
                SpeedGlobal = jsonObject.getFloat("SpeedGlobal",0);
                SpeedGlobalRandChangeTimer = jsonObject.getInt("SpeedGlobalRandChangeTimer",0);
                LowWindTimer = jsonObject.getInt("LowWindTimer",0);
                HighWindTimer = jsonObject.getInt("HighWindTimer",0);
            } catch (Exception e) {
                EnhancedWeather.LOGGER.error("Failed to load Wind Data for Dimension #"+dimid);
                EnhancedWeather.LOGGER.error("Reason: "+e.toString());
            }
        }
    }
}
