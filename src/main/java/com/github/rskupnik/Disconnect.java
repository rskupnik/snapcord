package com.github.rskupnik;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;

public class Disconnect {

    public static void main(String[] args) {
        DiscordAPI discordAPI = Javacord.getApi("MjMxNzU1MDgwMTU4ODA2MDE2.CtE-aw.SXSzdjJvbFdP9XX0mg5vCl6OKtU", true);
        discordAPI.disconnect();
    }
}
