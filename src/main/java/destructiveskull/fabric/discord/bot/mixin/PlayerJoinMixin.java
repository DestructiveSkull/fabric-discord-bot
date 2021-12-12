package destructiveskull.fabric.discord.bot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import destructiveskull.fabric.discord.bot.api.PlayerFirstJoinCallback;
import destructiveskull.fabric.discord.bot.api.PlayerJoinCallback;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;


@Mixin(PlayerManager.class)
public class PlayerJoinMixin {
    @Inject(at = @At(value = "TAIL"), method = "onPlayerConnect", cancellable = true)
    private  void onPlayerJoin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo info) {
        PlayerJoinCallback.EVENT.invoker().joinServer(player, player.getServer());
        if (player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.LEAVE_GAME)) < 1) {
            PlayerFirstJoinCallback.EVENT.invoker().joinServerForFirstTime(player, player.getServer());
        }

        ActionResult result1 = destructiveskull.fabric.discord.bot.api.event.PlayerJoinCallback.EVENT.invoker().joinServer(player, player.getServer());

        if (result1 == ActionResult.FAIL) {
            info.cancel();
        }
    }
}