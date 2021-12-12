package destructiveskull.fabric.discord.bot.api;

public class ConfigFile {
    String token;
    String channel_id;

    public ConfigFile(String token_arg, String channel_id_arg){
        token = token_arg;
        channel_id = channel_id_arg;
    }

    public String GetToken(){
        return token;
    }

    public String GetChannelID(){
        return channel_id;
    }
}
